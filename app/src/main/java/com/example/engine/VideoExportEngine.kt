package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.ExportConfig
import com.example.domain.model.StudioScene
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

data class ExportResult(
    val localFile: File,
    val galleryUri: Uri?,
    val width: Int,
    val height: Int,
    val fps: Int,
    val durationMs: Long,
    val fileSizeBytes: Long
)

object VideoExportEngine {

    private const val TAG = "VideoExportEngine"
    private const val VIDEO_MIME = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val AUDIO_MIME = MediaFormat.MIMETYPE_AUDIO_AAC

    data class SceneMovieSegment(
        val scene: StudioScene,
        val startMs: Long,
        val endMs: Long
    ) {
        val durationMs: Long get() = (endMs - startMs).coerceAtLeast(1000L)
    }

    fun buildSceneTimelineRanges(scenes: List<StudioScene>): List<SceneMovieSegment> {
        var cursor = 0L
        return scenes.map { sc ->
            val dur = sc.durationMs.coerceAtLeast(1000L)
            val seg = SceneMovieSegment(sc, cursor, cursor + dur)
            cursor += dur
            seg
        }
    }

    suspend fun exportSceneToMp4(
        context: Context,
        scene: StudioScene,
        characterLibrary: Map<String, CartoonCharacter>,
        audioEngine: AudioEngine,
        config: ExportConfig,
        customBgBitmap: Bitmap?,
        projectName: String,
        onProgress: (Float, String) -> Unit
    ): ExportResult = exportScenesToMp4(
        context = context,
        scenes = listOf(scene),
        characterLibrary = characterLibrary,
        audioEngine = audioEngine,
        config = config,
        customBgBitmap = customBgBitmap,
        projectName = projectName,
        onProgress = onProgress
    )

    /**
     * Renders every animation frame across one or multiple scenes sequentially via StageRenderer
     * onto an offscreen Bitmap, encodes to H.264 AVC, mixes Dialogue/SFX/Music into AAC,
     * and muxes a real playable MP4 movie file.
     */
    suspend fun exportScenesToMp4(
        context: Context,
        scenes: List<StudioScene>,
        characterLibrary: Map<String, CartoonCharacter>,
        audioEngine: AudioEngine,
        config: ExportConfig,
        customBgBitmap: Bitmap?,
        projectName: String,
        onProgress: (Float, String) -> Unit
    ): ExportResult = withContext(Dispatchers.Default) {
        val segments = buildSceneTimelineRanges(scenes.ifEmpty { return@withContext throw IllegalArgumentException("কোনো দৃশ্য পাওয়া যায়নি") })
        val (width, height) = config.computeVideoDimensions()
        val fps = config.fps.coerceIn(15, 60)
        val durationMs = (segments.lastOrNull()?.endMs ?: 1000L).coerceAtLeast(1000L)
        val totalFrames = ((durationMs * fps) / 1000L).toInt().coerceAtLeast(fps)

        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = projectName.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "ToonMotion" }
        val outFile = File(exportDir, "${safeName}_${System.currentTimeMillis()}.mp4")

        onProgress(0.02f, "${segments.size}টি দৃশ্যের অডিও মিক্স করা হচ্ছে...")
        val mixedPcm = audioEngine.mixScenesAudioToPcm(
            scenes = segments.map { it.scene },
            includeDialogue = config.includeDialogue,
            includeSfx = config.includeSfx,
            includeMusic = config.includeMusic,
            masterVolume = 1.0f
        )

        var videoEncoder: MediaCodec? = null
        var audioEncoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)
        val argbPixels = IntArray(width * height)

        try {
            // Configure Video Encoder (H.264 YUV420Flexible for universal Image-plane compatibility)
            val videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, (width * height * fps / 5).coerceIn(1_200_000, 8_000_000))
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            videoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME).apply {
                configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            // Configure Audio Encoder (AAC 44.1kHz Mono)
            val sampleRate = ProceduralSoundSynthesizer.SAMPLE_RATE
            val audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME, sampleRate, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            audioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME).apply {
                configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            val activeMuxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = activeMuxer

            var videoTrackIdx = -1
            var audioTrackIdx = -1
            val vInfo = MediaCodec.BufferInfo()
            val aInfo = MediaCodec.BufferInfo()

            var frameIndex = 0
            var videoInputDone = false
            var videoOutputDone = false

            var audioSampleCursor = 0
            var audioInputDone = false
            var audioOutputDone = false

            val customCharBitmaps = characterLibrary.values
                .filter { !it.customSpritePath.isNullOrBlank() }
                .mapNotNull { ch ->
                    runCatching { android.graphics.BitmapFactory.decodeFile(ch.customSpritePath) }.getOrNull()?.let { ch.id to it }
                }
                .toMap()

            val sceneCustomBgs = segments.mapNotNull { it.scene.customBackgroundUri }
                .distinct()
                .mapNotNull { path ->
                    runCatching { android.graphics.BitmapFactory.decodeFile(path) }.getOrNull()?.let { path to it }
                }
                .toMap()

            val pendingVideoSamples = ArrayList<Pair<ByteArray, MediaCodec.BufferInfo>>()
            val pendingAudioSamples = ArrayList<Pair<ByteArray, MediaCodec.BufferInfo>>()

            fun tryStartMuxer() {
                if (!muxerStarted && videoTrackIdx >= 0 && audioTrackIdx >= 0) {
                    activeMuxer.start()
                    muxerStarted = true
                    for ((bytes, bi) in pendingVideoSamples) {
                        activeMuxer.writeSampleData(videoTrackIdx, ByteBuffer.wrap(bytes), bi)
                    }
                    pendingVideoSamples.clear()
                    for ((bytes, bi) in pendingAudioSamples) {
                        activeMuxer.writeSampleData(audioTrackIdx, ByteBuffer.wrap(bytes), bi)
                    }
                    pendingAudioSamples.clear()
                }
            }

            while (!videoOutputDone || !audioOutputDone) {
                coroutineContext.ensureActive()

                // Feed Video Frames
                if (!videoInputDone) {
                    val inIdx = videoEncoder.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        if (frameIndex >= totalFrames) {
                            videoEncoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            videoInputDone = true
                        } else {
                            val movieTimeMs = ((frameIndex.toLong() * durationMs) / totalFrames).coerceAtMost(durationMs)
                            val seg = segments.firstOrNull { movieTimeMs < it.endMs } ?: segments.last()
                            val localTimeMs = (movieTimeMs - seg.startMs).coerceIn(0L, seg.durationMs)
                            val eval = AnimationEvaluator.evaluateScene(seg.scene, localTimeMs)
                            val bgBmp = seg.scene.customBackgroundUri?.let { sceneCustomBgs[it] } ?: customBgBitmap
                            StageRenderer.drawScene(
                                canvas = frameCanvas,
                                width = width.toFloat(),
                                height = height.toFloat(),
                                scene = seg.scene,
                                evaluated = eval,
                                characterLibrary = characterLibrary,
                                options = StageRenderer.RenderOptions(
                                    showRigOverlay = false,
                                    showCameraGuides = false,
                                    customBackgroundBitmap = bgBmp,
                                    customCharacterBitmaps = customCharBitmaps
                                )
                            )
                            frameBitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)
                            val inputImage = videoEncoder.getInputImage(inIdx)
                            if (inputImage != null) {
                                fillYuv420ImageFromArgb(inputImage, argbPixels, width, height)
                                val ptsUs = (frameIndex.toLong() * 1_000_000L) / fps
                                val bufSize = videoEncoder.getInputBuffer(inIdx)?.capacity() ?: (width * height * 3 / 2)
                                videoEncoder.queueInputBuffer(inIdx, 0, bufSize, ptsUs, 0)
                            } else {
                                // ByteBuffer fallback
                                val buf = videoEncoder.getInputBuffer(inIdx)!!
                                writeNv12ToBuffer(buf, argbPixels, width, height)
                                val ptsUs = (frameIndex.toLong() * 1_000_000L) / fps
                                videoEncoder.queueInputBuffer(inIdx, 0, width * height * 3 / 2, ptsUs, 0)
                            }
                            frameIndex++
                            val pct = 0.05f + 0.90f * (frameIndex.toFloat() / totalFrames)
                            onProgress(pct, "দৃশ্য ${segments.indexOf(seg) + 1}/${segments.size} • ফ্রেম $frameIndex/$totalFrames")
                        }
                    }
                }

                // Drain Video Output
                if (!videoOutputDone) {
                    val outIdx = videoEncoder.dequeueOutputBuffer(vInfo, 5_000L)
                    when {
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            videoTrackIdx = activeMuxer.addTrack(videoEncoder.outputFormat)
                            tryStartMuxer()
                        }
                        outIdx >= 0 -> {
                            val outBuf = videoEncoder.getOutputBuffer(outIdx)
                            if (outBuf != null && vInfo.size > 0 && (vInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                outBuf.position(vInfo.offset)
                                outBuf.limit(vInfo.offset + vInfo.size)
                                val copyInfo = MediaCodec.BufferInfo().apply {
                                    set(0, vInfo.size, vInfo.presentationTimeUs, vInfo.flags)
                                }
                                if (muxerStarted) {
                                    activeMuxer.writeSampleData(videoTrackIdx, outBuf, copyInfo)
                                } else {
                                    val arr = ByteArray(vInfo.size)
                                    outBuf.get(arr)
                                    pendingVideoSamples.add(arr to copyInfo)
                                }
                            }
                            videoEncoder.releaseOutputBuffer(outIdx, false)
                            if ((vInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                videoOutputDone = true
                            }
                        }
                    }
                }

                // Feed Audio Samples
                if (!audioInputDone) {
                    val inIdx = audioEncoder.dequeueInputBuffer(5_000L)
                    if (inIdx >= 0) {
                        if (audioSampleCursor >= mixedPcm.size) {
                            audioEncoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            audioInputDone = true
                        } else {
                            val buf = audioEncoder.getInputBuffer(inIdx)!!
                            buf.clear()
                            val maxShorts = (buf.capacity() / 2).coerceAtMost(1024)
                            val count = (mixedPcm.size - audioSampleCursor).coerceAtMost(maxShorts)
                            val bb = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
                            for (i in 0 until count) bb.putShort(mixedPcm[audioSampleCursor + i])
                            buf.put(bb.array())
                            val ptsUs = (audioSampleCursor.toLong() * 1_000_000L) / sampleRate
                            audioEncoder.queueInputBuffer(inIdx, 0, count * 2, ptsUs, 0)
                            audioSampleCursor += count
                        }
                    }
                }

                // Drain Audio Output
                if (!audioOutputDone) {
                    val outIdx = audioEncoder.dequeueOutputBuffer(aInfo, 5_000L)
                    when {
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            audioTrackIdx = activeMuxer.addTrack(audioEncoder.outputFormat)
                            tryStartMuxer()
                        }
                        outIdx >= 0 -> {
                            val outBuf = audioEncoder.getOutputBuffer(outIdx)
                            if (outBuf != null && aInfo.size > 0 && (aInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                outBuf.position(aInfo.offset)
                                outBuf.limit(aInfo.offset + aInfo.size)
                                val copyInfo = MediaCodec.BufferInfo().apply {
                                    set(0, aInfo.size, aInfo.presentationTimeUs, aInfo.flags)
                                }
                                if (muxerStarted) {
                                    activeMuxer.writeSampleData(audioTrackIdx, outBuf, copyInfo)
                                } else {
                                    val arr = ByteArray(aInfo.size)
                                    outBuf.get(arr)
                                    pendingAudioSamples.add(arr to copyInfo)
                                }
                            }
                            audioEncoder.releaseOutputBuffer(outIdx, false)
                            if ((aInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                audioOutputDone = true
                            }
                        }
                    }
                }
            }

            onProgress(0.97f, "MP4 তৈরি ও ফোনে সংরক্ষণ করা হচ্ছে...")
            if (muxerStarted) {
                activeMuxer.stop()
                muxerStarted = false
            }
            activeMuxer.release()
            muxer = null

            val galleryUri = saveMp4ToDeviceGallery(context, outFile)
            onProgress(1.0f, "এক্সপোর্ট সম্পন্ন!")

            ExportResult(
                localFile = outFile,
                galleryUri = galleryUri,
                width = width,
                height = height,
                fps = fps,
                durationMs = durationMs,
                fileSizeBytes = outFile.length()
            )
        } catch (e: CancellationException) {
            outFile.delete()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Video export failed", e)
            outFile.delete()
            throw e
        } finally {
            frameBitmap.recycle()
            try { videoEncoder?.stop(); videoEncoder?.release() } catch (_: Exception) {}
            try { audioEncoder?.stop(); audioEncoder?.release() } catch (_: Exception) {}
            try { if (muxerStarted) muxer?.stop(); muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun saveMp4ToDeviceGallery(context: Context, file: File): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ToonMotion Studio")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gallery MediaStore copy skipped: ${e.message}")
            null
        }
    }

    private fun fillYuv420ImageFromArgb(image: Image, argb: IntArray, width: Int, height: Int) {
        val planes = image.planes
        val yBuf = planes[0].buffer
        val uBuf = planes[1].buffer
        val vBuf = planes[2].buffer

        val yRowStride = planes[0].rowStride
        val yPixelStride = planes[0].pixelStride
        val uRowStride = planes[1].rowStride
        val uPixelStride = planes[1].pixelStride
        val vRowStride = planes[2].rowStride
        val vPixelStride = planes[2].pixelStride

        for (y in 0 until height) {
            for (x in 0 until width) {
                val px = argb[y * width + x]
                val r = (px ushr 16) and 0xFF
                val g = (px ushr 8) and 0xFF
                val b = px and 0xFF
                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yBuf.put(y * yRowStride + x * yPixelStride, yVal.coerceIn(0, 255).toByte())

                if ((y and 1) == 0 && (x and 1) == 0) {
                    val uvY = y ushr 1
                    val uvX = x ushr 1
                    val uVal = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val vVal = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    uBuf.put(uvY * uRowStride + uvX * uPixelStride, uVal.coerceIn(0, 255).toByte())
                    vBuf.put(uvY * vRowStride + uvX * vPixelStride, vVal.coerceIn(0, 255).toByte())
                }
            }
        }
    }

    private fun writeNv12ToBuffer(buf: ByteBuffer, argb: IntArray, width: Int, height: Int) {
        buf.clear()
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        for (y in 0 until height) {
            for (x in 0 until width) {
                val px = argb[y * width + x]
                val r = (px ushr 16) and 0xFF
                val g = (px ushr 8) and 0xFF
                val b = px and 0xFF
                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                buf.put(yIndex++, yVal.coerceIn(0, 255).toByte())
                if ((y and 1) == 0 && (x and 1) == 0 && uvIndex + 1 < buf.capacity()) {
                    val uVal = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val vVal = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    buf.put(uvIndex++, uVal.coerceIn(0, 255).toByte())
                    buf.put(uvIndex++, vVal.coerceIn(0, 255).toByte())
                }
            }
        }
    }
}
