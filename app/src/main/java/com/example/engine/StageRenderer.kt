package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import com.example.domain.model.BodyAccessory
import com.example.domain.model.BottomClothingStyle
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.CharacterViewAngle
import com.example.domain.model.EarStyle
import com.example.domain.model.EyeDirection
import com.example.domain.model.EyeShape
import com.example.domain.model.EyebrowStyle
import com.example.domain.model.FaceShape
import com.example.domain.model.FacialExpression
import com.example.domain.model.GlassesStyle
import com.example.domain.model.HairAccessory
import com.example.domain.model.HairStyle
import com.example.domain.model.HatStyle
import com.example.domain.model.LightingMood
import com.example.domain.model.NoseStyle
import com.example.domain.model.PropType
import com.example.domain.model.RigJoint
import com.example.domain.model.SceneBackground
import com.example.domain.model.ShoesStyle
import com.example.domain.model.StudioScene
import com.example.domain.model.TopClothingStyle
import com.example.domain.model.Viseme
import com.example.domain.model.WeatherType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object StageRenderer {

    data class RenderOptions(
        val showRigOverlay: Boolean = false,
        val selectedCharacterId: String? = null,
        val selectedJoint: RigJoint? = null,
        val selectedPropId: String? = null,
        val showCameraGuides: Boolean = false,
        val customBackgroundBitmap: Bitmap? = null,
        val customCharacterBitmaps: Map<String, Bitmap> = emptyMap(),
        val qualityScale: Float = 1.0f
    )

    fun drawScene(
        canvas: Canvas,
        width: Float,
        height: Float,
        scene: StudioScene,
        evaluated: EvaluatedSceneState,
        characterLibrary: Map<String, CartoonCharacter>,
        options: RenderOptions = RenderOptions()
    ) {
        if (width <= 2f || height <= 2f) return
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        canvas.save()

        // Apply Camera Transform around stage center
        val cam = evaluated.camera
        canvas.translate(width * 0.5f, height * 0.5f)
        canvas.scale(cam.zoom, cam.zoom)
        canvas.rotate(cam.rotationDeg)
        canvas.translate(-width * 0.5f - cam.panX * width, -height * 0.5f - cam.panY * height)

        // 1. Background
        drawBackground(canvas, width, height, scene, options.customBackgroundBitmap, fillPaint, strokePaint)

        // 2. Interleaved Characters & Props sorted by zOrder
        data class DrawItem(val z: Int, val isChar: Boolean, val charEval: EvaluatedCharacter? = null, val propEval: EvaluatedProp? = null)
        val items = buildList {
            evaluated.characters.forEach { add(DrawItem(it.instance.zOrder, true, charEval = it)) }
            evaluated.props.forEach { add(DrawItem(it.prop.zOrder, false, propEval = it)) }
        }.sortedBy { it.z }

        for (item in items) {
            if (item.isChar && item.charEval != null) {
                val cEval = item.charEval
                val charModel = characterLibrary[cEval.instance.characterId]
                    ?: CartoonCharacter(id = cEval.instance.characterId, name = cEval.instance.name)
                drawCharacterOnStage(
                    canvas = canvas,
                    stageW = width,
                    stageH = height,
                    evalChar = cEval,
                    character = charModel,
                    isSelected = options.selectedCharacterId == cEval.instance.id,
                    showRig = options.showRigOverlay && options.selectedCharacterId == cEval.instance.id,
                    selectedJoint = options.selectedJoint,
                    fillPaint = fillPaint,
                    strokePaint = strokePaint,
                    customSpriteBitmap = options.customCharacterBitmaps[charModel.id]
                )
            } else if (!item.isChar && item.propEval != null) {
                drawPropOnStage(
                    canvas = canvas,
                    stageW = width,
                    stageH = height,
                    evalProp = item.propEval,
                    isSelected = options.selectedPropId == item.propEval.prop.id,
                    fill = fillPaint,
                    stroke = strokePaint
                )
            }
        }

        // 3. Weather particles
        if (scene.weather != WeatherType.CLEAR) {
            drawWeather(canvas, width, height, scene.weather, evaluated.timeMs, fillPaint, strokePaint)
        }

        // 4. Lighting Mood tint / spotlight
        drawLightingMood(canvas, width, height, scene.lighting, fillPaint)

        canvas.restore()

        // 5. Optional Studio Camera Frame Guides (not in exported video!)
        if (options.showCameraGuides) {
            drawCameraOverlayGuides(canvas, width, height, evaluated.camera.activeCameraName, strokePaint, fillPaint)
        }
    }

    private fun drawBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        scene: StudioScene,
        customBitmap: Bitmap?,
        fill: Paint,
        stroke: Paint
    ) {
        if (scene.background == SceneBackground.CUSTOM_IMAGE && customBitmap != null) {
            val srcRect = android.graphics.Rect(0, 0, customBitmap.width, customBitmap.height)
            val dstRect = RectF(-w * 0.15f, -h * 0.15f, w * 1.15f, h * 1.15f)
            canvas.drawBitmap(customBitmap, srcRect, dstRect, fill)
            return
        }

        val skyColor = (scene.customSkyHex ?: scene.background.defaultSkyHex).toInt()
        val groundColor = (scene.customGroundHex ?: scene.background.defaultGroundHex).toInt()
        val horizonY = h * 0.66f

        // Overscan sky/ground so camera zoom/pan never shows black bars easily
        fill.shader = LinearGradient(
            0f, -h * 0.2f, 0f, horizonY,
            skyColor, blendColor(skyColor, Color.WHITE, 0.22f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(-w * 0.5f, -h * 0.5f, w * 1.5f, horizonY, fill)
        fill.shader = null

        fill.color = groundColor
        canvas.drawRect(-w * 0.5f, horizonY, w * 1.5f, h * 1.5f, fill)

        // Horizon line accent
        stroke.color = Color.argb(70, 0, 0, 0)
        stroke.strokeWidth = h * 0.005f
        canvas.drawLine(-w * 0.5f, horizonY, w * 1.5f, horizonY, stroke)

        when (scene.background) {
            SceneBackground.BD_VILLAGE_HOME -> {
                fill.color = Color.argb(190, 255, 255, 255)
                drawCloud(canvas, w * 0.22f, h * 0.16f, w * 0.09f, fill)
                drawCloud(canvas, w * 0.76f, h * 0.14f, w * 0.10f, fill)
                // Distant green trees + tin-roof Bengali cottage (টিনের ঘর)
                drawCartoonTree(canvas, w * 0.12f, horizonY, h * 0.26f, fill, stroke)
                drawCartoonTree(canvas, w * 0.88f, horizonY, h * 0.24f, fill, stroke)
                // Cottage wall
                fill.color = 0xFFE6D5B8.toInt()
                canvas.drawRect(w * 0.26f, horizonY - h * 0.18f, w * 0.56f, horizonY, fill)
                stroke.color = 0xFF5C4033.toInt()
                stroke.strokeWidth = 2.5f
                canvas.drawRect(w * 0.26f, horizonY - h * 0.18f, w * 0.56f, horizonY, stroke)
                // Tin sloped roof
                fill.color = 0xFFB91C1C.toInt()
                val roof = Path().apply {
                    moveTo(w * 0.22f, horizonY - h * 0.18f)
                    lineTo(w * 0.41f, horizonY - h * 0.30f)
                    lineTo(w * 0.60f, horizonY - h * 0.18f)
                    close()
                }
                canvas.drawPath(roof, fill)
                canvas.drawPath(roof, stroke)
                // Door & Window
                fill.color = 0xFF78350F.toInt()
                canvas.drawRect(w * 0.37f, horizonY - h * 0.12f, w * 0.44f, horizonY, fill)
                fill.color = 0xFF93C5FD.toInt()
                canvas.drawRect(w * 0.47f, horizonY - h * 0.13f, w * 0.53f, horizonY - h * 0.06f, fill)
                // Courtyard dirt path (উঠান / মেঠো পথ)
                fill.color = 0xFFD2B48C.toInt()
                canvas.drawRect(-w * 0.2f, horizonY + h * 0.05f, w * 1.2f, horizonY + h * 0.24f, fill)
            }
            SceneBackground.BD_TEA_STALL -> {
                drawCartoonTree(canvas, w * 0.10f, horizonY, h * 0.25f, fill, stroke)
                // টং চায়ের দোকান
                fill.color = 0xFF7C2D12.toInt()
                canvas.drawRect(w * 0.28f, horizonY - h * 0.25f, w * 0.72f, horizonY, fill)
                // Striped awning roof
                fill.color = 0xFF0F766E.toInt()
                canvas.drawRect(w * 0.24f, horizonY - h * 0.30f, w * 0.76f, horizonY - h * 0.23f, fill)
                // Counter & tea kettle
                fill.color = 0xFFB45309.toInt()
                canvas.drawRect(w * 0.30f, horizonY - h * 0.10f, w * 0.70f, horizonY, fill)
                fill.color = 0xFFFACC15.toInt()
                canvas.drawCircle(w * 0.50f, horizonY - h * 0.13f, h * 0.025f, fill)
            }
            SceneBackground.BD_RIVER_BOAT -> {
                fill.color = 0xFFFDE047.toInt()
                canvas.drawCircle(w * 0.80f, h * 0.18f, h * 0.055f, fill)
                // River water band
                fill.color = 0xFF0284C7.toInt()
                canvas.drawRect(-w * 0.3f, horizonY - h * 0.08f, w * 1.3f, horizonY + h * 0.08f, fill)
                // Traditional Sailboat (পালতোলা নৌকা)
                fill.color = 0xFF451A03.toInt()
                val boat = Path().apply {
                    moveTo(w * 0.35f, horizonY)
                    lineTo(w * 0.62f, horizonY)
                    lineTo(w * 0.56f, horizonY + h * 0.035f)
                    lineTo(w * 0.41f, horizonY + h * 0.035f)
                    close()
                }
                canvas.drawPath(boat, fill)
                fill.color = 0xFFEF4444.toInt()
                val sail = Path().apply {
                    moveTo(w * 0.48f, horizonY - h * 0.18f)
                    lineTo(w * 0.57f, horizonY - h * 0.05f)
                    lineTo(w * 0.48f, horizonY - h * 0.02f)
                    close()
                }
                canvas.drawPath(sail, fill)
            }
            SceneBackground.BD_SCHOOL -> {
                fill.color = Color.argb(180, 255, 255, 255)
                drawCloud(canvas, w * 0.25f, h * 0.15f, w * 0.09f, fill)
                // School Building
                fill.color = 0xFFFEF3C7.toInt()
                canvas.drawRect(w * 0.18f, horizonY - h * 0.26f, w * 0.82f, horizonY, fill)
                fill.color = 0xFF047857.toInt()
                canvas.drawRect(w * 0.16f, horizonY - h * 0.29f, w * 0.84f, horizonY - h * 0.25f, fill)
                // Bangladesh Flag Pole
                stroke.color = 0xFF334155.toInt()
                stroke.strokeWidth = 4f
                canvas.drawLine(w * 0.10f, horizonY, w * 0.10f, horizonY - h * 0.34f, stroke)
                fill.color = 0xFF006A4E.toInt()
                canvas.drawRect(w * 0.10f, horizonY - h * 0.34f, w * 0.18f, horizonY - h * 0.27f, fill)
                fill.color = 0xFFF42A41.toInt()
                canvas.drawCircle(w * 0.136f, horizonY - h * 0.305f, h * 0.018f, fill)
            }
            SceneBackground.PARK, SceneBackground.PLAYGROUND, SceneBackground.VILLAGE -> {
                // Soft cartoon clouds + rolling hills + trees
                fill.color = Color.argb(180, 255, 255, 255)
                drawCloud(canvas, w * 0.18f, h * 0.18f, w * 0.09f, fill)
                drawCloud(canvas, w * 0.72f, h * 0.14f, w * 0.11f, fill)

                fill.color = blendColor(groundColor, Color.BLACK, 0.15f)
                val hill = Path().apply {
                    moveTo(-w * 0.2f, horizonY)
                    quadTo(w * 0.25f, horizonY - h * 0.16f, w * 0.65f, horizonY)
                    quadTo(w * 0.9f, horizonY - h * 0.11f, w * 1.2f, horizonY)
                    close()
                }
                canvas.drawPath(hill, fill)

                drawCartoonTree(canvas, w * 0.12f, horizonY, h * 0.24f, fill, stroke)
                drawCartoonTree(canvas, w * 0.87f, horizonY, h * 0.28f, fill, stroke)
                if (scene.background == SceneBackground.PARK) {
                    // Park path
                    fill.color = 0xFFD6C29F.toInt()
                    canvas.drawRect(-w * 0.3f, horizonY + h * 0.09f, w * 1.3f, horizonY + h * 0.22f, fill)
                }
            }
            SceneBackground.CITY, SceneBackground.STREET, SceneBackground.BD_CITY_STREET, SceneBackground.NIGHT_SCENE, SceneBackground.RAINY_SCENE -> {
                val isNight = scene.background == SceneBackground.NIGHT_SCENE || scene.background == SceneBackground.RAINY_SCENE
                val bldgColors = if (isNight) intArrayOf(0xFF162038.toInt(), 0xFF1E2942.toInt(), 0xFF131B2E.toInt())
                else intArrayOf(0xFF51637A.toInt(), 0xFF425266.toInt(), 0xFF60748C.toInt())
                val widths = floatArrayOf(0.14f, 0.16f, 0.13f, 0.18f, 0.15f, 0.14f, 0.15f)
                val heights = floatArrayOf(0.32f, 0.44f, 0.26f, 0.48f, 0.35f, 0.40f, 0.29f)
                var curX = -w * 0.04f
                for (i in widths.indices) {
                    val bw = w * widths[i]
                    val bh = h * heights[i]
                    fill.color = bldgColors[i % bldgColors.size]
                    canvas.drawRect(curX, horizonY - bh, curX + bw * 0.94f, horizonY, fill)
                    // Windows
                    fill.color = if (isNight) Color.argb(210, 255, 210, 95) else Color.argb(140, 200, 235, 255)
                    var wy = horizonY - bh + h * 0.03f
                    while (wy < horizonY - h * 0.04f) {
                        var wx = curX + bw * 0.14f
                        while (wx < curX + bw * 0.74f) {
                            canvas.drawRect(wx, wy, wx + bw * 0.16f, wy + h * 0.025f, fill)
                            wx += bw * 0.25f
                        }
                        wy += h * 0.055f
                    }
                    curX += bw
                }
                // Road markings
                fill.color = Color.argb(200, 240, 210, 90)
                for (d in -1..6) {
                    canvas.drawRect(d * w * 0.18f, horizonY + h * 0.14f, d * w * 0.18f + w * 0.09f, horizonY + h * 0.165f, fill)
                }
                if (isNight) {
                    fill.color = 0xFFFDE68A.toInt()
                    canvas.drawCircle(w * 0.82f, h * 0.16f, h * 0.055f, fill)
                }
            }
            SceneBackground.FOREST, SceneBackground.MOUNTAIN -> {
                if (scene.background == SceneBackground.MOUNTAIN) {
                    val peaks = listOf(0.15f to 0.44f, 0.48f to 0.52f, 0.84f to 0.42f)
                    for ((px, ph) in peaks) {
                        fill.color = 0xFF475569.toInt()
                        val mPath = Path().apply {
                            moveTo(w * (px - 0.28f), horizonY)
                            lineTo(w * px, horizonY - h * ph)
                            lineTo(w * (px + 0.28f), horizonY)
                            close()
                        }
                        canvas.drawPath(mPath, fill)
                        // Snow cap
                        fill.color = 0xFFF1F5F9.toInt()
                        val cap = Path().apply {
                            moveTo(w * px, horizonY - h * ph)
                            lineTo(w * (px - 0.09f), horizonY - h * ph * 0.68f)
                            lineTo(w * (px + 0.09f), horizonY - h * ph * 0.68f)
                            close()
                        }
                        canvas.drawPath(cap, fill)
                    }
                }
                val treeXs = floatArrayOf(0.08f, 0.22f, 0.78f, 0.92f)
                for (tx in treeXs) {
                    drawPineTree(canvas, w * tx, horizonY, h * 0.28f, fill)
                }
            }
            SceneBackground.BEACH, SceneBackground.RIVER -> {
                fill.color = 0xFF0EA5E9.toInt()
                canvas.drawRect(-w * 0.3f, horizonY - h * 0.06f, w * 1.3f, horizonY + h * 0.05f, fill)
                fill.color = 0xFFFDE047.toInt()
                canvas.drawCircle(w * 0.82f, h * 0.16f, h * 0.06f, fill)
            }
            SceneBackground.BEDROOM, SceneBackground.LIVING_ROOM, SceneBackground.KITCHEN,
            SceneBackground.BD_DINING, SceneBackground.OFFICE, SceneBackground.SCHOOL,
            SceneBackground.CLASSROOM, SceneBackground.RESTAURANT, SceneBackground.HOSPITAL -> {
                // Indoor room perspective walls + window + floorboard lines
                fill.color = blendColor(skyColor, Color.BLACK, 0.16f)
                canvas.drawRect(-w * 0.3f, -h * 0.2f, w * 0.14f, horizonY, fill)
                canvas.drawRect(w * 0.86f, -h * 0.2f, w * 1.3f, horizonY, fill)
                // Baseboard
                fill.color = blendColor(groundColor, Color.BLACK, 0.28f)
                canvas.drawRect(-w * 0.3f, horizonY - h * 0.025f, w * 1.3f, horizonY, fill)

                // Back wall window or chalkboard/cross
                val frameRect = RectF(w * 0.32f, h * 0.14f, w * 0.68f, h * 0.42f)
                when (scene.background) {
                    SceneBackground.CLASSROOM -> {
                        fill.color = 0xFF1F4E3D.toInt()
                        canvas.drawRoundRect(frameRect, 8f, 8f, fill)
                        stroke.color = 0xFFA16207.toInt()
                        stroke.strokeWidth = h * 0.015f
                        canvas.drawRoundRect(frameRect, 8f, 8f, stroke)
                    }
                    SceneBackground.HOSPITAL -> {
                        fill.color = 0xFFE11D48.toInt()
                        canvas.drawRect(w * 0.48f, h * 0.18f, w * 0.52f, h * 0.34f, fill)
                        canvas.drawRect(w * 0.44f, h * 0.24f, w * 0.56f, h * 0.28f, fill)
                    }
                    else -> {
                        fill.color = 0xFF93C5FD.toInt()
                        canvas.drawRoundRect(frameRect, 10f, 10f, fill)
                        stroke.color = 0xFFCBD5E1.toInt()
                        stroke.strokeWidth = h * 0.012f
                        canvas.drawRoundRect(frameRect, 10f, 10f, stroke)
                        canvas.drawLine(frameRect.centerX(), frameRect.top, frameRect.centerX(), frameRect.bottom, stroke)
                        canvas.drawLine(frameRect.left, frameRect.centerY(), frameRect.right, frameRect.centerY(), stroke)
                    }
                }
            }
            else -> {
                // Transit / Market / Custom atmospheric archways
                fill.color = Color.argb(40, 255, 255, 255)
                canvas.drawCircle(w * 0.5f, h * 0.22f, h * 0.18f, fill)
            }
        }
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, r: Float, fill: Paint) {
        canvas.drawCircle(cx, cy, r * 0.55f, fill)
        canvas.drawCircle(cx - r * 0.45f, cy + r * 0.12f, r * 0.4f, fill)
        canvas.drawCircle(cx + r * 0.45f, cy + r * 0.12f, r * 0.42f, fill)
    }

    private fun drawCartoonTree(canvas: Canvas, x: Float, groundY: Float, h: Float, fill: Paint, stroke: Paint) {
        fill.color = 0xFF6B4226.toInt()
        canvas.drawRect(x - h * 0.07f, groundY - h * 0.45f, x + h * 0.07f, groundY, fill)
        fill.color = 0xFF2E8B57.toInt()
        canvas.drawCircle(x, groundY - h * 0.58f, h * 0.28f, fill)
        canvas.drawCircle(x - h * 0.16f, groundY - h * 0.48f, h * 0.21f, fill)
        canvas.drawCircle(x + h * 0.16f, groundY - h * 0.48f, h * 0.21f, fill)
    }

    private fun drawPineTree(canvas: Canvas, x: Float, groundY: Float, h: Float, fill: Paint) {
        fill.color = 0xFF5C3A21.toInt()
        canvas.drawRect(x - h * 0.06f, groundY - h * 0.25f, x + h * 0.06f, groundY, fill)
        fill.color = 0xFF1B5E3A.toInt()
        val p = Path().apply {
            moveTo(x, groundY - h)
            lineTo(x - h * 0.28f, groundY - h * 0.22f)
            lineTo(x + h * 0.28f, groundY - h * 0.22f)
            close()
        }
        canvas.drawPath(p, fill)
    }

    fun drawCharacterOnStage(
        canvas: Canvas,
        stageW: Float,
        stageH: Float,
        evalChar: EvaluatedCharacter,
        character: CartoonCharacter,
        isSelected: Boolean,
        showRig: Boolean,
        selectedJoint: RigJoint?,
        fillPaint: Paint,
        strokePaint: Paint,
        customSpriteBitmap: Bitmap? = null
    ) {
        val rootPos = Offset(evalChar.x * stageW, evalChar.y * stageH)
        val baseHeightPx = stageH * 0.46f * evalChar.scale
        val isBack = evalChar.viewAngle == CharacterViewAngle.BACK
        val isFront = evalChar.viewAngle == CharacterViewAngle.FRONT
        val isSide = evalChar.viewAngle == CharacterViewAngle.SIDE

        // Imported External Character Sprite (Transparent PNG / Photo)
        if (character.customSpritePath != null && customSpriteBitmap != null && !customSpriteBitmap.isRecycled) {
            val aspect = (customSpriteBitmap.width.toFloat() / customSpriteBitmap.height.coerceAtLeast(1)).coerceIn(0.35f, 2.2f)
            val drawH = baseHeightPx
            val drawW = drawH * aspect
            val bobY = evalChar.pose.hipOffsetY * baseHeightPx
            val swayDeg = evalChar.pose.angleOf(RigJoint.TORSO) * 0.35f

            canvas.save()
            if (evalChar.rotationDeg != 0f) {
                canvas.rotate(evalChar.rotationDeg, rootPos.x, rootPos.y)
            }
            fillPaint.color = Color.argb(65, 0, 0, 0)
            canvas.drawOval(
                RectF(
                    rootPos.x - drawW * 0.42f,
                    rootPos.y - drawH * 0.03f,
                    rootPos.x + drawW * 0.42f,
                    rootPos.y + drawH * 0.035f
                ),
                fillPaint
            )

            canvas.save()
            canvas.translate(rootPos.x, rootPos.y + bobY)
            canvas.rotate(swayDeg)
            val flipScaleX = if (evalChar.facingRight) 1f else -1f
            val angleScaleX = when (evalChar.viewAngle) {
                CharacterViewAngle.SIDE -> 0.85f
                else -> 1f
            }
            canvas.scale(flipScaleX * angleScaleX, 1f)
            val srcRect = android.graphics.Rect(0, 0, customSpriteBitmap.width, customSpriteBitmap.height)
            val dstRect = RectF(-drawW * 0.5f, -drawH, drawW * 0.5f, 0f)
            fillPaint.alpha = if (isBack) 215 else 255
            canvas.drawBitmap(customSpriteBitmap, srcRect, dstRect, fillPaint)
            fillPaint.alpha = 255
            canvas.restore()

            if (isSelected) {
                strokePaint.color = 0xFF00E5FF.toInt()
                strokePaint.strokeWidth = 3.5f
                strokePaint.pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
                canvas.drawRect(-drawW * 0.52f + rootPos.x, -drawH + rootPos.y, drawW * 0.52f + rootPos.x, rootPos.y + 4f, strokePaint)
                strokePaint.pathEffect = null
            }
            canvas.restore()
            return
        }

        val rig = RigSolver.solveRig(
            rootGroundPos = rootPos,
            baseHeightPx = baseHeightPx,
            body = character.body,
            headSizeScale = character.face.headSize,
            pose = evalChar.pose,
            facingRight = evalChar.facingRight
        )

        canvas.save()
        if (evalChar.rotationDeg != 0f) {
            canvas.rotate(evalChar.rotationDeg, rootPos.x, rootPos.y)
        }

        // Ground shadow
        fillPaint.color = Color.argb(65, 0, 0, 0)
        canvas.drawOval(
            RectF(
                rootPos.x - rig.torsoWidth * 0.85f,
                rootPos.y - baseHeightPx * 0.025f,
                rootPos.x + rig.torsoWidth * 0.85f,
                rootPos.y + baseHeightPx * 0.03f
            ),
            fillPaint
        )

        val outlineColor = 0xFF141824.toInt()
        val skinColor = character.face.skinToneHex.toInt()
        val topColor = character.clothing.topColorHex.toInt()
        val bottomColor = character.clothing.bottomColorHex.toInt()
        val shoeColor = character.clothing.shoesColorHex.toInt()
        val dir = if (evalChar.facingRight) 1f else -1f
        val effectiveTorsoW = when {
            isSide -> rig.torsoWidth * 0.78f
            isFront || isBack -> rig.torsoWidth * 1.04f
            else -> rig.torsoWidth
        }

        fun j(joint: RigJoint): Offset = rig.joints[joint] ?: rootPos

        // Hero cape behind torso when viewed from Front / Side / 3-4
        if (character.clothing.accessory == BodyAccessory.CAPE && !isBack) {
            fillPaint.color = character.clothing.accessoryColorHex.toInt()
            val cape = Path().apply {
                moveTo(j(RigJoint.SHOULDER_L).x, j(RigJoint.SHOULDER_L).y)
                lineTo(j(RigJoint.SHOULDER_R).x, j(RigJoint.SHOULDER_R).y)
                lineTo(j(RigJoint.HIP).x - dir * effectiveTorsoW * 1.25f, j(RigJoint.HIP).y + rig.upperLegLen * 0.9f)
                lineTo(j(RigJoint.HIP).x - dir * effectiveTorsoW * 0.35f, j(RigJoint.HIP).y + rig.upperLegLen * 0.95f)
                close()
            }
            canvas.drawPath(cape, fillPaint)
        }

        // Back arm (Left when facing right)
        drawLimb(canvas, j(RigJoint.SHOULDER_L), j(RigJoint.UPPER_ARM_L), rig.armThickness, topColor, outlineColor, strokePaint)
        drawLimb(canvas, j(RigJoint.UPPER_ARM_L), j(RigJoint.HAND_L), rig.armThickness * 0.88f, if (character.clothing.topStyle == TopClothingStyle.TSHIRT) skinColor else topColor, outlineColor, strokePaint)
        drawHandWithFingers(canvas, j(RigJoint.HAND_L), j(RigJoint.FINGERS_L), rig.armThickness * 0.68f, skinColor, outlineColor, fillPaint, strokePaint)

        // Back leg (Left)
        val legPantsColor = if (character.clothing.bottomStyle == BottomClothingStyle.NONE) skinColor else bottomColor
        drawLimb(canvas, j(RigJoint.UPPER_LEG_L), j(RigJoint.KNEE_L), rig.legThickness, legPantsColor, outlineColor, strokePaint)
        val lowerLegColorL = if (character.clothing.bottomStyle == BottomClothingStyle.SHORTS || character.clothing.bottomStyle == BottomClothingStyle.SKIRT) skinColor else legPantsColor
        drawLimb(canvas, j(RigJoint.KNEE_L), j(RigJoint.LOWER_LEG_L), rig.legThickness * 0.9f, lowerLegColorL, outlineColor, strokePaint)
        drawShoe(canvas, j(RigJoint.LOWER_LEG_L), j(RigJoint.FOOT_L), rig.legThickness, shoeColor, character.clothing.shoesStyle, outlineColor, fillPaint, strokePaint, evalChar.viewAngle)

        // Front leg (Right)
        drawLimb(canvas, j(RigJoint.UPPER_LEG_R), j(RigJoint.KNEE_R), rig.legThickness, legPantsColor, outlineColor, strokePaint)
        val lowerLegColorR = if (character.clothing.bottomStyle == BottomClothingStyle.SHORTS || character.clothing.bottomStyle == BottomClothingStyle.SKIRT) skinColor else legPantsColor
        drawLimb(canvas, j(RigJoint.KNEE_R), j(RigJoint.LOWER_LEG_R), rig.legThickness * 0.9f, lowerLegColorR, outlineColor, strokePaint)
        drawShoe(canvas, j(RigJoint.LOWER_LEG_R), j(RigJoint.FOOT_R), rig.legThickness, shoeColor, character.clothing.shoesStyle, outlineColor, fillPaint, strokePaint, evalChar.viewAngle)

        // Skirt / Lungi / Saree / Dress bottom flare if selected
        val isLungiOrSaree = character.clothing.bottomStyle == BottomClothingStyle.LUNGI ||
            character.clothing.bottomStyle == BottomClothingStyle.SAREE_DRAPE ||
            character.clothing.topStyle == TopClothingStyle.SAREE
        if (character.clothing.bottomStyle == BottomClothingStyle.SKIRT ||
            character.clothing.topStyle == TopClothingStyle.DRESS ||
            isLungiOrSaree
        ) {
            val wrapColor = if (character.clothing.topStyle == TopClothingStyle.DRESS || character.clothing.topStyle == TopClothingStyle.SAREE) topColor else bottomColor
            val hip = j(RigJoint.HIP)
            val dropRatio = if (isLungiOrSaree) 1.38f else 0.72f
            val flareWidth = if (isLungiOrSaree) 1.24f else 1.55f
            val wrapPath = Path().apply {
                moveTo(hip.x - rig.hipHalfWidth * 1.08f, hip.y - rig.upperLegLen * 0.1f)
                lineTo(hip.x + rig.hipHalfWidth * 1.08f, hip.y - rig.upperLegLen * 0.1f)
                lineTo(hip.x + rig.hipHalfWidth * flareWidth, hip.y + rig.upperLegLen * dropRatio)
                lineTo(hip.x - rig.hipHalfWidth * flareWidth, hip.y + rig.upperLegLen * dropRatio)
                close()
            }
            fillPaint.color = wrapColor
            canvas.drawPath(wrapPath, fillPaint)
            if (character.clothing.bottomStyle == BottomClothingStyle.LUNGI) {
                strokePaint.color = blendColor(wrapColor, Color.BLACK, 0.30f)
                strokePaint.strokeWidth = 2f
                canvas.drawLine(hip.x - rig.hipHalfWidth * 0.9f, hip.y + rig.upperLegLen * 0.65f, hip.x + rig.hipHalfWidth * 0.9f, hip.y + rig.upperLegLen * 0.65f, strokePaint)
            }
            strokePaint.color = outlineColor
            strokePaint.strokeWidth = 2.6f
            canvas.drawPath(wrapPath, strokePaint)
        }

        // Torso & Clothing
        drawTorso(canvas, j(RigJoint.HIP), j(RigJoint.CHEST), effectiveTorsoW, character.clothing.topStyle, topColor, bottomColor, outlineColor, fillPaint, strokePaint, isBack)

        // Scarf accessory
        if (character.clothing.accessory == BodyAccessory.SCARF) {
            fillPaint.color = character.clothing.accessoryColorHex.toInt()
            val neck = j(RigJoint.NECK)
            canvas.drawRoundRect(
                RectF(neck.x - effectiveTorsoW * 0.42f, neck.y - 6f, neck.x + effectiveTorsoW * 0.42f, neck.y + rig.headRadius * 0.25f),
                10f, 10f, fillPaint
            )
        }

        // Neck
        drawLimb(canvas, j(RigJoint.CHEST), j(RigJoint.NECK), rig.armThickness * 0.95f, skinColor, outlineColor, strokePaint)

        // Head + Hair + Face (respects Front / 3-4 / Side / Back)
        drawHeadAndFace(
            canvas = canvas,
            headCenter = j(RigJoint.HEAD),
            headRadius = rig.headRadius,
            headAngleDeg = evalChar.pose.angleOf(RigJoint.HEAD) + evalChar.pose.angleOf(RigJoint.NECK) * 0.5f,
            facingRight = evalChar.facingRight,
            character = character,
            evalChar = evalChar,
            outlineColor = outlineColor,
            fillPaint = fillPaint,
            strokePaint = strokePaint
        )

        // Front arm (Right)
        drawLimb(canvas, j(RigJoint.SHOULDER_R), j(RigJoint.UPPER_ARM_R), rig.armThickness, topColor, outlineColor, strokePaint)
        drawLimb(canvas, j(RigJoint.UPPER_ARM_R), j(RigJoint.HAND_R), rig.armThickness * 0.88f, if (character.clothing.topStyle == TopClothingStyle.TSHIRT) skinColor else topColor, outlineColor, strokePaint)
        drawHandWithFingers(canvas, j(RigJoint.HAND_R), j(RigJoint.FINGERS_R), rig.armThickness * 0.68f, skinColor, outlineColor, fillPaint, strokePaint)

        // Back-view Backpack / Hero Cape over back of torso
        if (isBack && character.clothing.accessory == BodyAccessory.CAPE) {
            fillPaint.color = character.clothing.accessoryColorHex.toInt()
            val cape = Path().apply {
                moveTo(j(RigJoint.SHOULDER_L).x, j(RigJoint.SHOULDER_L).y)
                lineTo(j(RigJoint.SHOULDER_R).x, j(RigJoint.SHOULDER_R).y)
                lineTo(j(RigJoint.HIP).x + effectiveTorsoW * 0.95f, j(RigJoint.HIP).y + rig.upperLegLen * 0.95f)
                lineTo(j(RigJoint.HIP).x - effectiveTorsoW * 0.95f, j(RigJoint.HIP).y + rig.upperLegLen * 0.95f)
                close()
            }
            canvas.drawPath(cape, fillPaint)
            strokePaint.color = outlineColor
            strokePaint.strokeWidth = 2.6f
            canvas.drawPath(cape, strokePaint)
        } else if (isBack && character.clothing.accessory == BodyAccessory.BACKPACK) {
            fillPaint.color = character.clothing.accessoryColorHex.toInt()
            val midY = (j(RigJoint.CHEST).y + j(RigJoint.HIP).y) * 0.5f
            val midX = (j(RigJoint.CHEST).x + j(RigJoint.HIP).x) * 0.5f
            val packRect = RectF(midX - effectiveTorsoW * 0.45f, j(RigJoint.CHEST).y, midX + effectiveTorsoW * 0.45f, midY + effectiveTorsoW * 0.35f)
            canvas.drawRoundRect(packRect, 12f, 12f, fillPaint)
            strokePaint.color = outlineColor
            strokePaint.strokeWidth = 2.6f
            canvas.drawRoundRect(packRect, 12f, 12f, strokePaint)
        }

        // Selection ring
        if (isSelected) {
            strokePaint.color = 0xFF00E5FF.toInt()
            strokePaint.strokeWidth = 3.5f
            strokePaint.pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
            canvas.drawOval(
                RectF(
                    rootPos.x - effectiveTorsoW * 1.4f,
                    rootPos.y - baseHeightPx * 0.05f,
                    rootPos.x + effectiveTorsoW * 1.4f,
                    rootPos.y + baseHeightPx * 0.06f
                ),
                strokePaint
            )
            strokePaint.pathEffect = null
        }

        // Skeletal Rig Overlay (for Bone & Joint Drag-to-Pose Editor)
        if (showRig) {
            drawRigOverlay(canvas, rig, selectedJoint, fillPaint, strokePaint)
        }

        canvas.restore()
    }

    private fun drawLimb(
        canvas: Canvas,
        a: Offset,
        b: Offset,
        thickness: Float,
        fillColor: Int,
        outlineColor: Int,
        stroke: Paint
    ) {
        stroke.color = outlineColor
        stroke.strokeWidth = thickness + 5f
        canvas.drawLine(a.x, a.y, b.x, b.y, stroke)

        stroke.color = fillColor
        stroke.strokeWidth = thickness
        canvas.drawLine(a.x, a.y, b.x, b.y, stroke)
    }

    private fun drawHandWithFingers(
        canvas: Canvas,
        wrist: Offset,
        fingerTip: Offset,
        radius: Float,
        skinColor: Int,
        outlineColor: Int,
        fill: Paint,
        stroke: Paint
    ) {
        fill.color = skinColor
        canvas.drawCircle(wrist.x, wrist.y, radius, fill)
        stroke.color = outlineColor
        stroke.strokeWidth = 2.4f
        canvas.drawCircle(wrist.x, wrist.y, radius, stroke)

        // Stylized cartoon fingers
        stroke.color = skinColor
        stroke.strokeWidth = radius * 0.65f
        canvas.drawLine(wrist.x, wrist.y, fingerTip.x, fingerTip.y, stroke)
    }

    private fun drawShoe(
        canvas: Canvas,
        ankle: Offset,
        toe: Offset,
        legThickness: Float,
        shoeColor: Int,
        style: ShoesStyle,
        outlineColor: Int,
        fill: Paint,
        stroke: Paint,
        viewAngle: CharacterViewAngle = CharacterViewAngle.THREE_QUARTER
    ) {
        val h = when (style) {
            ShoesStyle.BOOTS -> legThickness * 1.15f
            ShoesStyle.SANDALS -> legThickness * 0.55f
            else -> legThickness * 0.85f
        }
        val rect = if (viewAngle == CharacterViewAngle.FRONT || viewAngle == CharacterViewAngle.BACK) {
            RectF(ankle.x - legThickness * 0.68f, ankle.y - h * 0.25f, ankle.x + legThickness * 0.68f, ankle.y + h * 0.75f)
        } else {
            val minX = minOf(ankle.x, toe.x) - legThickness * 0.45f
            val maxX = maxOf(ankle.x, toe.x) + legThickness * 0.55f
            RectF(minX, ankle.y - h * 0.3f, maxX, ankle.y + h * 0.75f)
        }
        fill.color = shoeColor
        canvas.drawRoundRect(rect, h * 0.45f, h * 0.45f, fill)
        stroke.color = outlineColor
        stroke.strokeWidth = 2.6f
        canvas.drawRoundRect(rect, h * 0.45f, h * 0.45f, stroke)
    }

    private fun drawTorso(
        canvas: Canvas,
        hip: Offset,
        chest: Offset,
        torsoWidth: Float,
        topStyle: TopClothingStyle,
        topColor: Int,
        bottomColor: Int,
        outlineColor: Int,
        fill: Paint,
        stroke: Paint,
        isBack: Boolean = false
    ) {
        val cx = (hip.x + chest.x) * 0.5f
        val topY = chest.y - torsoWidth * 0.14f
        val botY = hip.y + torsoWidth * 0.22f
        val rect = RectF(cx - torsoWidth * 0.56f, topY, cx + torsoWidth * 0.56f, botY)

        fill.color = topColor
        canvas.drawRoundRect(rect, torsoWidth * 0.34f, torsoWidth * 0.34f, fill)
        stroke.color = outlineColor
        stroke.strokeWidth = 3f
        canvas.drawRoundRect(rect, torsoWidth * 0.34f, torsoWidth * 0.34f, stroke)

        if (isBack) {
            if (topStyle == TopClothingStyle.HOODIE) {
                fill.color = blendColor(topColor, Color.BLACK, 0.16f)
                val hood = RectF(cx - torsoWidth * 0.38f, topY - 4f, cx + torsoWidth * 0.38f, topY + torsoWidth * 0.32f)
                canvas.drawRoundRect(hood, 14f, 14f, fill)
                canvas.drawRoundRect(hood, 14f, 14f, stroke)
            } else {
                // Subtle back shoulder seam
                stroke.color = blendColor(topColor, Color.BLACK, 0.25f)
                stroke.strokeWidth = 2f
                canvas.drawLine(cx, topY + 6f, cx, botY - 8f, stroke)
            }
            return
        }

        // Front Style details
        when (topStyle) {
            TopClothingStyle.JACKET -> {
                fill.color = Color.WHITE
                canvas.drawRect(cx - torsoWidth * 0.12f, topY + 4f, cx + torsoWidth * 0.12f, botY - 4f, fill)
            }
            TopClothingStyle.HOODIE -> {
                fill.color = blendColor(topColor, Color.BLACK, 0.2f)
                val pocket = RectF(cx - torsoWidth * 0.3f, botY - torsoWidth * 0.35f, cx + torsoWidth * 0.3f, botY - 6f)
                canvas.drawRoundRect(pocket, 8f, 8f, fill)
            }
            TopClothingStyle.UNIFORM, TopClothingStyle.SHIRT -> {
                fill.color = if (topStyle == TopClothingStyle.UNIFORM) 0xFFE11D48.toInt() else bottomColor
                val tie = Path().apply {
                    moveTo(cx, topY + 4f)
                    lineTo(cx - torsoWidth * 0.08f, topY + torsoWidth * 0.45f)
                    lineTo(cx, topY + torsoWidth * 0.58f)
                    lineTo(cx + torsoWidth * 0.08f, topY + torsoWidth * 0.45f)
                    close()
                }
                canvas.drawPath(tie, fill)
            }
            TopClothingStyle.TRADITIONAL, TopClothingStyle.PUNJABI -> {
                stroke.color = 0xFFFFD166.toInt()
                stroke.strokeWidth = 3f
                canvas.drawLine(cx, topY + 4f, cx, botY - 4f, stroke)
            }
            TopClothingStyle.SAREE -> {
                // Diagonal Saree pallu drape
                fill.color = blendColor(topColor, 0xFFFFD166.toInt(), 0.35f)
                val pallu = Path().apply {
                    moveTo(cx - torsoWidth * 0.52f, topY + 4f)
                    lineTo(cx - torsoWidth * 0.18f, topY + 4f)
                    lineTo(cx + torsoWidth * 0.52f, botY - 4f)
                    lineTo(cx + torsoWidth * 0.18f, botY - 4f)
                    close()
                }
                canvas.drawPath(pallu, fill)
            }
            TopClothingStyle.FOTUA -> {
                // Shoulder Gamcha stripe
                fill.color = 0xFFDC2626.toInt()
                canvas.drawRoundRect(RectF(cx + torsoWidth * 0.14f, topY, cx + torsoWidth * 0.44f, botY - 8f), 6f, 6f, fill)
            }
            TopClothingStyle.SALWAR_KAMEEZ -> {
                fill.color = blendColor(bottomColor, Color.WHITE, 0.2f)
                canvas.drawRect(cx - torsoWidth * 0.08f, topY + 8f, cx + torsoWidth * 0.08f, botY - 6f, fill)
            }
            else -> Unit
        }
    }

    private fun drawHeadAndFace(
        canvas: Canvas,
        headCenter: Offset,
        headRadius: Float,
        headAngleDeg: Float,
        facingRight: Boolean,
        character: CartoonCharacter,
        evalChar: EvaluatedCharacter,
        outlineColor: Int,
        fillPaint: Paint,
        strokePaint: Paint
    ) {
        val dir = if (facingRight) 1f else -1f
        val viewAngle = evalChar.viewAngle
        val isBack = viewAngle == CharacterViewAngle.BACK
        val isFront = viewAngle == CharacterViewAngle.FRONT
        val isSide = viewAngle == CharacterViewAngle.SIDE

        canvas.save()
        canvas.rotate(headAngleDeg * dir, headCenter.x, headCenter.y)

        val skin = character.face.skinToneHex.toInt()
        val hairColor = character.hair.colorHex.toInt()

        // Back hair for long/ponytail
        if (!isBack && (character.hair.style == HairStyle.LONG || character.hair.style == HairStyle.PONYTAIL)) {
            fillPaint.color = hairColor
            if (character.hair.style == HairStyle.LONG) {
                canvas.drawRoundRect(
                    RectF(headCenter.x - headRadius * 1.05f, headCenter.y - headRadius * 0.5f, headCenter.x + headRadius * 1.05f, headCenter.y + headRadius * 1.65f),
                    headRadius * 0.45f, headRadius * 0.45f, fillPaint
                )
            } else {
                canvas.drawOval(
                    RectF(headCenter.x - dir * headRadius * 1.4f, headCenter.y - headRadius * 0.3f, headCenter.x - dir * headRadius * 0.5f, headCenter.y + headRadius * 0.85f),
                    fillPaint
                )
            }
        }

        // Ears
        val earR = when (character.face.earStyle) {
            EarStyle.LARGE -> headRadius * 0.27f
            EarStyle.SMALL -> headRadius * 0.16f
            else -> headRadius * 0.22f
        }
        fillPaint.color = skin
        if (isSide) {
            canvas.drawCircle(headCenter.x - dir * headRadius * 0.42f, headCenter.y + headRadius * 0.08f, earR, fillPaint)
        } else {
            canvas.drawCircle(headCenter.x - headRadius * 0.96f, headCenter.y + headRadius * 0.08f, earR, fillPaint)
            canvas.drawCircle(headCenter.x + headRadius * 0.96f, headCenter.y + headRadius * 0.08f, earR, fillPaint)
        }

        // Head Shape
        fillPaint.color = skin
        val headWidthFactor = if (isSide) 0.86f else 1.0f
        val headRect = when (character.face.faceShape) {
            FaceShape.ROUND -> RectF(headCenter.x - headRadius * headWidthFactor, headCenter.y - headRadius, headCenter.x + headRadius * headWidthFactor, headCenter.y + headRadius)
            FaceShape.SQUARE -> RectF(headCenter.x - headRadius * 0.96f * headWidthFactor, headCenter.y - headRadius * 0.98f, headCenter.x + headRadius * 0.96f * headWidthFactor, headCenter.y + headRadius * 0.98f)
            FaceShape.HEART -> RectF(headCenter.x - headRadius * 0.94f * headWidthFactor, headCenter.y - headRadius * 1.02f, headCenter.x + headRadius * 0.94f * headWidthFactor, headCenter.y + headRadius * 0.95f)
            else -> RectF(headCenter.x - headRadius * 0.92f * headWidthFactor, headCenter.y - headRadius * 1.05f, headCenter.x + headRadius * 0.92f * headWidthFactor, headCenter.y + headRadius * 1.02f)
        }
        val corner = if (character.face.faceShape == FaceShape.SQUARE) headRadius * 0.36f else headRadius * 0.92f
        canvas.drawRoundRect(headRect, corner, corner, fillPaint)
        strokePaint.color = outlineColor
        strokePaint.strokeWidth = 3f
        canvas.drawRoundRect(headRect, corner, corner, strokePaint)

        // Full Back-of-Head view (no facial features visible)
        if (isBack) {
            if (character.hair.style != HairStyle.BALD) {
                fillPaint.color = hairColor
                strokePaint.color = outlineColor
                strokePaint.strokeWidth = 2.6f
                val backHairRect = RectF(
                    headRect.left - headRadius * 0.04f,
                    headRect.top - headRadius * 0.06f,
                    headRect.right + headRadius * 0.04f,
                    headCenter.y + headRadius * 0.62f
                )
                canvas.drawRoundRect(backHairRect, headRadius * 0.85f, headRadius * 0.85f, fillPaint)
                canvas.drawRoundRect(backHairRect, headRadius * 0.85f, headRadius * 0.85f, strokePaint)
                if (character.hair.style == HairStyle.LONG) {
                    canvas.drawRoundRect(
                        RectF(headRect.left - headRadius * 0.08f, headCenter.y, headRect.right + headRadius * 0.08f, headCenter.y + headRadius * 1.65f),
                        headRadius * 0.4f, headRadius * 0.4f, fillPaint
                    )
                } else if (character.hair.style == HairStyle.PONYTAIL) {
                    canvas.drawOval(
                        RectF(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.1f, headCenter.x + headRadius * 0.35f, headCenter.y + headRadius * 1.15f),
                        fillPaint
                    )
                } else if (character.hair.style == HairStyle.CURLY) {
                    for (a in -3..3) {
                        val ang = (a * 24f - 90f) * (PI / 180.0)
                        val cx = headCenter.x + (cos(ang) * headRadius * 0.88f).toFloat()
                        val cy = headCenter.y + (sin(ang) * headRadius * 0.88f).toFloat()
                        canvas.drawCircle(cx, cy, headRadius * 0.36f, fillPaint)
                    }
                }
            }
            if (character.clothing.hatStyle != HatStyle.NONE) {
                drawHat(canvas, headCenter, headRadius, dir, character.clothing.hatStyle, character.clothing.hatColorHex.toInt(), outlineColor, fillPaint, strokePaint, isBack = true)
            }
            canvas.restore()
            return
        }

        // Shy blush cheeks
        val lookShiftX = when {
            isFront -> 0f
            isSide -> dir * headRadius * 0.32f
            else -> dir * headRadius * 0.12f
        }
        if (evalChar.expression == FacialExpression.SHY || evalChar.expression == FacialExpression.SMILE) {
            fillPaint.color = Color.argb(if (evalChar.expression == FacialExpression.SHY) 130 else 65, 255, 105, 135)
            if (isSide) {
                val bx = headCenter.x + lookShiftX * 0.65f
                canvas.drawOval(RectF(bx - headRadius * 0.16f, headCenter.y + headRadius * 0.14f, bx + headRadius * 0.16f, headCenter.y + headRadius * 0.32f), fillPaint)
            } else {
                canvas.drawOval(RectF(headCenter.x + lookShiftX - headRadius * 0.65f, headCenter.y + headRadius * 0.14f, headCenter.x + lookShiftX - headRadius * 0.30f, headCenter.y + headRadius * 0.32f), fillPaint)
                canvas.drawOval(RectF(headCenter.x + lookShiftX + headRadius * 0.30f, headCenter.y + headRadius * 0.14f, headCenter.x + lookShiftX + headRadius * 0.65f, headCenter.y + headRadius * 0.32f), fillPaint)
            }
        }

        // Eyes
        val eyeY = headCenter.y - headRadius * 0.08f
        val eyeSpacing = if (isSide) headRadius * 0.22f else headRadius * 0.36f
        val eyeR = headRadius * 0.18f * character.face.eyeSize.coerceIn(0.75f, 1.35f)
        val pupilOffset = when (evalChar.eyeDirection) {
            EyeDirection.LEFT -> Offset(-eyeR * 0.32f, 0f)
            EyeDirection.RIGHT -> Offset(eyeR * 0.32f, 0f)
            EyeDirection.UP -> Offset(0f, -eyeR * 0.32f)
            EyeDirection.DOWN -> Offset(0f, eyeR * 0.32f)
            else -> if (isFront) Offset.Zero else Offset(dir * eyeR * 0.14f, 0f)
        }

        val eyeSides = if (isSide) intArrayOf(dir.toInt()) else intArrayOf(-1, 1)
        for (side in eyeSides) {
            val ex = if (isSide) headCenter.x + lookShiftX * 0.7f else headCenter.x + lookShiftX + side * eyeSpacing
            val openScale = (1f - evalChar.eyeBlinkAmount).coerceIn(0.08f, 1f)
            if (openScale < 0.18f) {
                strokePaint.color = outlineColor
                strokePaint.strokeWidth = 3.2f
                canvas.drawLine(ex - eyeR, eyeY, ex + eyeR, eyeY, strokePaint)
            } else {
                val eyeH = eyeR * (if (character.face.eyeShape == EyeShape.ALMOND) 0.78f else 1.08f) * openScale
                val eyeRect = RectF(ex - eyeR, eyeY - eyeH, ex + eyeR, eyeY + eyeH)
                fillPaint.color = Color.WHITE
                canvas.drawOval(eyeRect, fillPaint)
                strokePaint.color = outlineColor
                strokePaint.strokeWidth = 2.2f
                canvas.drawOval(eyeRect, strokePaint)

                // Iris + pupil
                fillPaint.color = character.face.eyeColorHex.toInt()
                val irisR = eyeR * 0.56f * openScale.coerceAtLeast(0.5f)
                canvas.drawCircle(ex + pupilOffset.x, eyeY + pupilOffset.y, irisR, fillPaint)
                fillPaint.color = Color.BLACK
                canvas.drawCircle(ex + pupilOffset.x, eyeY + pupilOffset.y, irisR * 0.54f, fillPaint)
                // Catchlight
                fillPaint.color = Color.WHITE
                canvas.drawCircle(ex + pupilOffset.x - irisR * 0.28f, eyeY + pupilOffset.y - irisR * 0.28f, irisR * 0.24f, fillPaint)
            }

            // Eyebrow
            val browTilt = when (evalChar.expression) {
                FacialExpression.ANGRY -> side * 6f
                FacialExpression.SAD, FacialExpression.CRY, FacialExpression.SHY -> -side * 5f
                FacialExpression.SURPRISED, FacialExpression.SHOCKED -> -4f
                else -> 0f
            }
            val browLift = if (evalChar.expression == FacialExpression.SURPRISED || evalChar.expression == FacialExpression.SHOCKED) -eyeR * 0.45f else 0f
            strokePaint.color = blendColor(hairColor, Color.BLACK, 0.25f)
            strokePaint.strokeWidth = when (character.face.eyebrowStyle) {
                EyebrowStyle.THICK -> 5.2f
                EyebrowStyle.THIN -> 2.4f
                else -> 3.6f
            }
            val by = eyeY - eyeR * 1.45f + browLift
            canvas.drawLine(ex - eyeR * 0.9f, by - browTilt, ex + eyeR * 0.9f, by + browTilt, strokePaint)
        }

        // Cry tears
        if (evalChar.expression == FacialExpression.CRY) {
            fillPaint.color = 0xFF38BDF8.toInt()
            canvas.drawOval(RectF(headCenter.x + lookShiftX - eyeSpacing - 4f, eyeY + eyeR, headCenter.x + lookShiftX - eyeSpacing + 4f, eyeY + eyeR * 2.2f), fillPaint)
            canvas.drawOval(RectF(headCenter.x + lookShiftX + eyeSpacing - 4f, eyeY + eyeR, headCenter.x + lookShiftX + eyeSpacing + 4f, eyeY + eyeR * 2.2f), fillPaint)
        }

        // Nose
        val noseX = headCenter.x + lookShiftX * 1.15f
        val noseY = headCenter.y + headRadius * 0.16f
        strokePaint.color = outlineColor
        strokePaint.strokeWidth = 2.6f
        when (character.face.noseStyle) {
            NoseStyle.POINTED -> {
                val np = Path().apply {
                    moveTo(noseX, noseY - headRadius * 0.08f)
                    lineTo(noseX + dir * headRadius * 0.14f, noseY + headRadius * 0.06f)
                    lineTo(noseX, noseY + headRadius * 0.09f)
                }
                canvas.drawPath(np, strokePaint)
            }
            else -> canvas.drawArc(RectF(noseX - 6f, noseY - 4f, noseX + 6f, noseY + 6f), 0f, 180f, false, strokePaint)
        }

        // Mouth (Viseme + Expression)
        drawMouth(
            canvas = canvas,
            cx = headCenter.x + lookShiftX,
            cy = headCenter.y + headRadius * 0.48f,
            headRadius = headRadius,
            viseme = evalChar.viseme,
            expression = evalChar.expression,
            fill = fillPaint,
            stroke = strokePaint
        )

        // Glasses
        if (character.clothing.glassesStyle != GlassesStyle.NONE) {
            drawGlasses(canvas, headCenter.x + lookShiftX, eyeY, eyeSpacing, eyeR, character.clothing.glassesStyle, fillPaint, strokePaint)
        }

        // Top Hair
        drawFrontHair(canvas, headCenter, headRadius, dir, character.hair.style, hairColor, outlineColor, fillPaint, strokePaint)

        // Hair Accessory
        if (character.hair.accessory == HairAccessory.HEADBAND) {
            strokePaint.color = character.hair.accessoryColorHex.toInt()
            strokePaint.strokeWidth = headRadius * 0.14f
            canvas.drawArc(RectF(headCenter.x - headRadius * 0.95f, headCenter.y - headRadius * 1.0f, headCenter.x + headRadius * 0.95f, headCenter.y + headRadius * 0.4f), 195f, 150f, false, strokePaint)
        } else if (character.hair.accessory == HairAccessory.CROWN) {
            fillPaint.color = 0xFFFFB300.toInt()
            val crown = Path().apply {
                moveTo(headCenter.x - headRadius * 0.45f, headCenter.y - headRadius * 0.95f)
                lineTo(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 1.38f)
                lineTo(headCenter.x, headCenter.y - headRadius * 1.15f)
                lineTo(headCenter.x + headRadius * 0.35f, headCenter.y - headRadius * 1.38f)
                lineTo(headCenter.x + headRadius * 0.45f, headCenter.y - headRadius * 0.95f)
                close()
            }
            canvas.drawPath(crown, fillPaint)
        }

        // Hat
        if (character.clothing.hatStyle != HatStyle.NONE) {
            drawHat(canvas, headCenter, headRadius, dir, character.clothing.hatStyle, character.clothing.hatColorHex.toInt(), outlineColor, fillPaint, strokePaint)
        }

        canvas.restore()
    }

    private fun drawMouth(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        headRadius: Float,
        viseme: Viseme,
        expression: FacialExpression,
        fill: Paint,
        stroke: Paint
    ) {
        val mw = headRadius * 0.36f
        val mh = headRadius * 0.24f
        if (viseme != Viseme.REST) {
            when (viseme) {
                Viseme.AH -> {
                    fill.color = 0xFF7F1D1D.toInt()
                    canvas.drawOval(RectF(cx - mw * 0.85f, cy - mh * 0.55f, cx + mw * 0.85f, cy + mh * 1.05f), fill)
                    fill.color = Color.WHITE
                    canvas.drawRect(cx - mw * 0.55f, cy - mh * 0.48f, cx + mw * 0.55f, cy - mh * 0.1f, fill)
                }
                Viseme.OO -> {
                    fill.color = 0xFF7F1D1D.toInt()
                    canvas.drawCircle(cx, cy + mh * 0.2f, mw * 0.45f, fill)
                }
                Viseme.EE -> {
                    fill.color = Color.WHITE
                    canvas.drawRoundRect(RectF(cx - mw, cy - mh * 0.3f, cx + mw, cy + mh * 0.45f), 8f, 8f, fill)
                    stroke.color = 0xFF141824.toInt()
                    stroke.strokeWidth = 2.2f
                    canvas.drawRoundRect(RectF(cx - mw, cy - mh * 0.3f, cx + mw, cy + mh * 0.45f), 8f, 8f, stroke)
                }
                Viseme.MM -> {
                    stroke.color = 0xFF141824.toInt()
                    stroke.strokeWidth = 3.5f
                    canvas.drawLine(cx - mw * 0.8f, cy, cx + mw * 0.8f, cy, stroke)
                }
                else -> {
                    fill.color = 0xFF7F1D1D.toInt()
                    canvas.drawOval(RectF(cx - mw * 0.7f, cy - mh * 0.25f, cx + mw * 0.7f, cy + mh * 0.65f), fill)
                }
            }
            return
        }

        stroke.color = 0xFF141824.toInt()
        stroke.strokeWidth = 3f
        when (expression) {
            FacialExpression.LAUGH -> {
                fill.color = 0xFF991B1B.toInt()
                val rect = RectF(cx - mw, cy - mh * 0.2f, cx + mw, cy + mh * 1.1f)
                canvas.drawArc(rect, 0f, 180f, true, fill)
                fill.color = Color.WHITE
                canvas.drawRect(cx - mw * 0.7f, cy - mh * 0.2f, cx + mw * 0.7f, cy + mh * 0.18f, fill)
                canvas.drawArc(rect, 0f, 180f, true, stroke)
            }
            FacialExpression.SMILE -> canvas.drawArc(RectF(cx - mw * 0.85f, cy - mh * 0.5f, cx + mw * 0.85f, cy + mh * 0.55f), 15f, 150f, false, stroke)
            FacialExpression.SAD, FacialExpression.CRY -> canvas.drawArc(RectF(cx - mw * 0.75f, cy + mh * 0.1f, cx + mw * 0.75f, cy + mh), 200f, 140f, false, stroke)
            FacialExpression.ANGRY -> {
                fill.color = Color.WHITE
                val rect = RectF(cx - mw * 0.8f, cy - mh * 0.2f, cx + mw * 0.8f, cy + mh * 0.45f)
                canvas.drawRoundRect(rect, 6f, 6f, fill)
                canvas.drawRoundRect(rect, 6f, 6f, stroke)
            }
            FacialExpression.SURPRISED, FacialExpression.SHOCKED -> {
                fill.color = 0xFF7F1D1D.toInt()
                canvas.drawOval(RectF(cx - mw * 0.48f, cy - mh * 0.25f, cx + mw * 0.48f, cy + mh * 0.85f), fill)
            }
            FacialExpression.CONFUSED -> canvas.drawLine(cx - mw * 0.6f, cy + 4f, cx + mw * 0.6f, cy - 5f, stroke)
            else -> canvas.drawLine(cx - mw * 0.65f, cy + 2f, cx + mw * 0.65f, cy + 2f, stroke)
        }
    }

    private fun drawGlasses(
        canvas: Canvas,
        cx: Float,
        eyeY: Float,
        spacing: Float,
        eyeR: Float,
        style: GlassesStyle,
        fill: Paint,
        stroke: Paint
    ) {
        val frameR = eyeR * 1.38f
        if (style == GlassesStyle.SUNGLASSES || style == GlassesStyle.VISOR) {
            fill.color = if (style == GlassesStyle.VISOR) 0xCC00E5FF.toInt() else 0xDD111827.toInt()
            if (style == GlassesStyle.VISOR) {
                canvas.drawRoundRect(RectF(cx - spacing - frameR, eyeY - frameR * 0.75f, cx + spacing + frameR, eyeY + frameR * 0.75f), 10f, 10f, fill)
                return
            }
            canvas.drawRoundRect(RectF(cx - spacing - frameR, eyeY - frameR * 0.8f, cx - spacing + frameR, eyeY + frameR * 0.8f), 8f, 8f, fill)
            canvas.drawRoundRect(RectF(cx + spacing - frameR, eyeY - frameR * 0.8f, cx + spacing + frameR, eyeY + frameR * 0.8f), 8f, 8f, fill)
        }
        stroke.color = 0xFF1E293B.toInt()
        stroke.strokeWidth = 3.2f
        if (style == GlassesStyle.ROUND) {
            canvas.drawCircle(cx - spacing, eyeY, frameR, stroke)
            canvas.drawCircle(cx + spacing, eyeY, frameR, stroke)
        } else {
            canvas.drawRoundRect(RectF(cx - spacing - frameR, eyeY - frameR * 0.8f, cx - spacing + frameR, eyeY + frameR * 0.8f), 6f, 6f, stroke)
            canvas.drawRoundRect(RectF(cx + spacing - frameR, eyeY - frameR * 0.8f, cx + spacing + frameR, eyeY + frameR * 0.8f), 6f, 6f, stroke)
        }
        canvas.drawLine(cx - spacing + frameR, eyeY, cx + spacing - frameR, eyeY, stroke)
    }

    private fun drawFrontHair(
        canvas: Canvas,
        c: Offset,
        r: Float,
        dir: Float,
        style: HairStyle,
        hairColor: Int,
        outlineColor: Int,
        fill: Paint,
        stroke: Paint
    ) {
        if (style == HairStyle.BALD) return
        fill.color = hairColor
        stroke.color = outlineColor
        stroke.strokeWidth = 2.6f

        when (style) {
            HairStyle.SPIKY -> {
                val p = Path().apply {
                    moveTo(c.x - r * 0.96f, c.y - r * 0.25f)
                    lineTo(c.x - r * 0.85f, c.y - r * 1.15f)
                    lineTo(c.x - r * 0.35f, c.y - r * 0.88f)
                    lineTo(c.x, c.y - r * 1.38f)
                    lineTo(c.x + r * 0.38f, c.y - r * 0.9f)
                    lineTo(c.x + r * 0.88f, c.y - r * 1.15f)
                    lineTo(c.x + r * 0.96f, c.y - r * 0.25f)
                    quadTo(c.x, c.y - r * 0.65f, c.x - r * 0.96f, c.y - r * 0.25f)
                    close()
                }
                canvas.drawPath(p, fill)
                canvas.drawPath(p, stroke)
            }
            HairStyle.CURLY -> {
                for (a in -3..3) {
                    val ang = (a * 24f - 90f) * (PI / 180.0)
                    val cx = c.x + (cos(ang) * r * 0.88f).toFloat()
                    val cy = c.y + (sin(ang) * r * 0.88f).toFloat()
                    canvas.drawCircle(cx, cy, r * 0.36f, fill)
                }
            }
            else -> {
                val hairArc = RectF(c.x - r * 1.02f, c.y - r * 1.12f, c.x + r * 1.02f, c.y + r * 0.45f)
                canvas.drawArc(hairArc, 185f, 170f, true, fill)
                canvas.drawArc(hairArc, 185f, 170f, false, stroke)
                // Bangs sweep
                val bang = Path().apply {
                    moveTo(c.x - r * 0.7f, c.y - r * 0.55f)
                    quadTo(c.x + dir * r * 0.1f, c.y - r * 0.22f, c.x + r * 0.8f, c.y - r * 0.52f)
                    quadTo(c.x, c.y - r * 0.85f, c.x - r * 0.7f, c.y - r * 0.55f)
                }
                canvas.drawPath(bang, fill)
            }
        }
    }

    private fun drawHat(
        canvas: Canvas,
        c: Offset,
        r: Float,
        dir: Float,
        style: HatStyle,
        hatColor: Int,
        outlineColor: Int,
        fill: Paint,
        stroke: Paint,
        isBack: Boolean = false
    ) {
        fill.color = hatColor
        stroke.color = outlineColor
        stroke.strokeWidth = 2.6f
        when (style) {
            HatStyle.CAP -> {
                val dome = RectF(c.x - r * 0.98f, c.y - r * 1.25f, c.x + r * 0.98f, c.y - r * 0.2f)
                canvas.drawArc(dome, 180f, 180f, true, fill)
                canvas.drawArc(dome, 180f, 180f, true, stroke)
                if (isBack) {
                    fill.color = outlineColor
                    canvas.drawRoundRect(RectF(c.x - r * 0.35f, c.y - r * 0.62f, c.x + r * 0.35f, c.y - r * 0.45f), 4f, 4f, fill)
                } else {
                    val brim = RectF(c.x - r * 0.1f * dir, c.y - r * 0.78f, c.x + dir * r * 1.38f, c.y - r * 0.54f)
                    canvas.drawRoundRect(brim, 8f, 8f, fill)
                    canvas.drawRoundRect(brim, 8f, 8f, stroke)
                }
            }
            HatStyle.BEANIE -> {
                val dome = RectF(c.x - r * 0.98f, c.y - r * 1.32f, c.x + r * 0.98f, c.y - r * 0.3f)
                canvas.drawArc(dome, 180f, 180f, true, fill)
                canvas.drawCircle(c.x, c.y - r * 1.35f, r * 0.18f, fill)
            }
            HatStyle.FEDORA -> {
                canvas.drawRoundRect(RectF(c.x - r * 1.28f, c.y - r * 0.82f, c.x + r * 1.28f, c.y - r * 0.64f), 8f, 8f, fill)
                canvas.drawRoundRect(RectF(c.x - r * 0.75f, c.y - r * 1.35f, c.x + r * 0.75f, c.y - r * 0.72f), 12f, 12f, fill)
            }
            HatStyle.HELMET -> {
                val dome = RectF(c.x - r * 1.06f, c.y - r * 1.25f, c.x + r * 1.06f, c.y - r * 0.15f)
                canvas.drawArc(dome, 175f, 190f, true, fill)
                canvas.drawArc(dome, 175f, 190f, true, stroke)
            }
            HatStyle.TUPI -> {
                fill.color = 0xFFF8FAFC.toInt()
                val dome = RectF(c.x - r * 0.86f, c.y - r * 1.22f, c.x + r * 0.86f, c.y - r * 0.35f)
                canvas.drawArc(dome, 180f, 180f, true, fill)
                canvas.drawArc(dome, 180f, 180f, true, stroke)
            }
            HatStyle.MATHAL -> {
                fill.color = 0xFFD97706.toInt()
                val cone = Path().apply {
                    moveTo(c.x, c.y - r * 1.55f)
                    lineTo(c.x - r * 1.48f, c.y - r * 0.55f)
                    lineTo(c.x + r * 1.48f, c.y - r * 0.55f)
                    close()
                }
                canvas.drawPath(cone, fill)
                canvas.drawPath(cone, stroke)
            }
            HatStyle.NONE -> Unit
        }
    }

    private fun drawRigOverlay(
        canvas: Canvas,
        rig: ResolvedRig,
        selectedJoint: RigJoint?,
        fill: Paint,
        stroke: Paint
    ) {
        stroke.color = 0xCC00E5FF.toInt()
        stroke.strokeWidth = 3f
        for (joint in RigJoint.entries) {
            val parent = joint.parent ?: continue
            val pPos = rig.joints[parent] ?: continue
            val cPos = rig.joints[joint] ?: continue
            canvas.drawLine(pPos.x, pPos.y, cPos.x, cPos.y, stroke)
        }
        for ((joint, pos) in rig.joints) {
            val isSel = joint == selectedJoint
            fill.color = if (isSel) 0xFFFFB300.toInt() else 0xFF00E5FF.toInt()
            canvas.drawCircle(pos.x, pos.y, if (isSel) 9f else 5.5f, fill)
            stroke.color = 0xFF0A0E17.toInt()
            stroke.strokeWidth = 2f
            canvas.drawCircle(pos.x, pos.y, if (isSel) 9f else 5.5f, stroke)
        }
    }

    private fun drawPropOnStage(
        canvas: Canvas,
        stageW: Float,
        stageH: Float,
        evalProp: EvaluatedProp,
        isSelected: Boolean,
        fill: Paint,
        stroke: Paint
    ) {
        val cx = evalProp.x * stageW
        val cy = evalProp.y * stageH
        val size = stageH * 0.10f * evalProp.scale
        val baseColor = evalProp.prop.colorHex.toInt()
        val outline = 0xFF141824.toInt()

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(evalProp.rotationDeg)
        stroke.color = outline
        stroke.strokeWidth = 2.6f

        when (evalProp.prop.type) {
            PropType.CHAIR -> {
                fill.color = baseColor
                canvas.drawRoundRect(RectF(-size * 0.45f, -size * 0.65f, -size * 0.25f, size * 0.5f), 4f, 4f, fill)
                canvas.drawRoundRect(RectF(-size * 0.45f, -size * 0.05f, size * 0.45f, size * 0.12f), 4f, 4f, fill)
                canvas.drawRect(size * 0.28f, size * 0.12f, size * 0.42f, size * 0.5f, fill)
            }
            PropType.TABLE, PropType.FURNITURE -> {
                fill.color = baseColor
                canvas.drawRoundRect(RectF(-size * 0.9f, -size * 0.35f, size * 0.9f, -size * 0.16f), 6f, 6f, fill)
                canvas.drawRoundRect(RectF(-size * 0.9f, -size * 0.35f, size * 0.9f, -size * 0.16f), 6f, 6f, stroke)
                canvas.drawRect(-size * 0.75f, -size * 0.16f, -size * 0.6f, size * 0.55f, fill)
                canvas.drawRect(size * 0.6f, -size * 0.16f, size * 0.75f, size * 0.55f, fill)
            }
            PropType.PHONE -> {
                fill.color = 0xFF1E293B.toInt()
                canvas.drawRoundRect(RectF(-size * 0.22f, -size * 0.38f, size * 0.22f, size * 0.38f), 8f, 8f, fill)
                fill.color = 0xFF38BDF8.toInt()
                canvas.drawRect(-size * 0.17f, -size * 0.28f, size * 0.17f, size * 0.26f, fill)
            }
            PropType.BOOK -> {
                fill.color = baseColor
                canvas.drawRoundRect(RectF(-size * 0.38f, -size * 0.44f, size * 0.38f, size * 0.44f), 6f, 6f, fill)
                canvas.drawRoundRect(RectF(-size * 0.38f, -size * 0.44f, size * 0.38f, size * 0.44f), 6f, 6f, stroke)
                fill.color = Color.WHITE
                canvas.drawRect(-size * 0.26f, -size * 0.26f, size * 0.26f, -size * 0.12f, fill)
            }
            PropType.BAG -> {
                fill.color = baseColor
                canvas.drawRoundRect(RectF(-size * 0.42f, -size * 0.42f, size * 0.42f, size * 0.42f), 12f, 12f, fill)
                canvas.drawRoundRect(RectF(-size * 0.42f, -size * 0.42f, size * 0.42f, size * 0.42f), 12f, 12f, stroke)
            }
            PropType.CUP -> {
                fill.color = Color.WHITE
                canvas.drawRoundRect(RectF(-size * 0.24f, -size * 0.28f, size * 0.24f, size * 0.28f), 6f, 6f, fill)
                canvas.drawRoundRect(RectF(-size * 0.24f, -size * 0.28f, size * 0.24f, size * 0.28f), 6f, 6f, stroke)
            }
            PropType.PLATE -> {
                fill.color = 0xFFF8FAFC.toInt()
                canvas.drawOval(RectF(-size * 0.55f, -size * 0.18f, size * 0.55f, size * 0.18f), fill)
                canvas.drawOval(RectF(-size * 0.55f, -size * 0.18f, size * 0.55f, size * 0.18f), stroke)
            }
            PropType.FOOD -> {
                // Burger / Apple cartoon snack
                fill.color = 0xFFE63946.toInt()
                canvas.drawCircle(0f, 0f, size * 0.32f, fill)
                canvas.drawCircle(0f, 0f, size * 0.32f, stroke)
                fill.color = 0xFF4ADE80.toInt()
                canvas.drawOval(RectF(0f, -size * 0.52f, size * 0.28f, -size * 0.30f), fill)
            }
            PropType.VEHICLE -> {
                fill.color = baseColor
                canvas.drawRoundRect(RectF(-size * 1.15f, -size * 0.3f, size * 1.15f, size * 0.3f), 14f, 14f, fill)
                canvas.drawRoundRect(RectF(-size * 0.7f, -size * 0.65f, size * 0.65f, -size * 0.25f), 10f, 10f, fill)
                fill.color = 0xFF111827.toInt()
                canvas.drawCircle(-size * 0.65f, size * 0.34f, size * 0.26f, fill)
                canvas.drawCircle(size * 0.65f, size * 0.34f, size * 0.26f, fill)
            }
            PropType.TOOL -> {
                fill.color = 0xFF94A3B8.toInt()
                canvas.drawRect(-size * 0.12f, -size * 0.45f, size * 0.12f, size * 0.45f, fill)
                fill.color = baseColor
                canvas.drawRoundRect(RectF(-size * 0.38f, -size * 0.55f, size * 0.38f, -size * 0.32f), 6f, 6f, fill)
            }
            PropType.TOY -> {
                fill.color = baseColor
                canvas.drawCircle(0f, 0f, size * 0.38f, fill)
                canvas.drawCircle(0f, 0f, size * 0.38f, stroke)
            }
            PropType.DOOR -> {
                fill.color = 0xFF78350F.toInt()
                canvas.drawRoundRect(RectF(-size * 0.48f, -size * 1.1f, size * 0.48f, size * 0.6f), 6f, 6f, fill)
                canvas.drawRoundRect(RectF(-size * 0.48f, -size * 1.1f, size * 0.48f, size * 0.6f), 6f, 6f, stroke)
                fill.color = 0xFFFACC15.toInt()
                canvas.drawCircle(size * 0.3f, -size * 0.2f, size * 0.08f, fill)
            }
            PropType.WINDOW -> {
                fill.color = 0xFF93C5FD.toInt()
                canvas.drawRect(-size * 0.6f, -size * 0.6f, size * 0.6f, size * 0.6f, fill)
                canvas.drawRect(-size * 0.6f, -size * 0.6f, size * 0.6f, size * 0.6f, stroke)
            }
            PropType.RICKSHAW -> {
                // Colorful Bangladeshi Rickshaw (হুড ও চাকা)
                fill.color = 0xFFE11D48.toInt()
                val hood = Path().apply {
                    moveTo(-size * 1.05f, -size * 0.15f)
                    lineTo(-size * 0.95f, -size * 1.10f)
                    lineTo(-size * 0.10f, -size * 0.95f)
                    lineTo(-size * 0.15f, -size * 0.15f)
                    close()
                }
                canvas.drawPath(hood, fill)
                canvas.drawPath(hood, stroke)
                fill.color = 0xFF10B981.toInt()
                canvas.drawRoundRect(RectF(-size * 1.05f, -size * 0.25f, size * 0.20f, size * 0.20f), 8f, 8f, fill)
                canvas.drawRoundRect(RectF(-size * 1.05f, -size * 0.25f, size * 0.20f, size * 0.20f), 8f, 8f, stroke)
                // Handlebar
                stroke.color = 0xFF111827.toInt()
                stroke.strokeWidth = 4f
                canvas.drawLine(size * 0.15f, -size * 0.05f, size * 0.72f, -size * 0.65f, stroke)
                // Wheels
                canvas.drawCircle(-size * 0.68f, size * 0.34f, size * 0.32f, stroke)
                canvas.drawCircle(size * 0.75f, size * 0.34f, size * 0.28f, stroke)
            }
            PropType.BICYCLE -> {
                stroke.color = baseColor
                stroke.strokeWidth = 4f
                canvas.drawCircle(-size * 0.65f, size * 0.32f, size * 0.28f, stroke)
                canvas.drawCircle(size * 0.65f, size * 0.32f, size * 0.28f, stroke)
                canvas.drawLine(-size * 0.65f, size * 0.32f, 0f, -size * 0.15f, stroke)
                canvas.drawLine(0f, -size * 0.15f, size * 0.65f, size * 0.32f, stroke)
                canvas.drawLine(size * 0.45f, -size * 0.38f, size * 0.65f, size * 0.32f, stroke)
            }
            PropType.TEA_CUP -> {
                fill.color = 0xFFF8FAFC.toInt()
                canvas.drawRoundRect(RectF(-size * 0.24f, -size * 0.25f, size * 0.24f, size * 0.25f), 6f, 6f, fill)
                canvas.drawRoundRect(RectF(-size * 0.24f, -size * 0.25f, size * 0.24f, size * 0.25f), 6f, 6f, stroke)
                fill.color = 0xFF92400E.toInt()
                canvas.drawRect(-size * 0.18f, -size * 0.20f, size * 0.18f, -size * 0.08f, fill)
            }
            PropType.RICE_PLATE -> {
                fill.color = 0xFFE2E8F0.toInt()
                canvas.drawOval(RectF(-size * 0.60f, -size * 0.20f, size * 0.60f, size * 0.20f), fill)
                canvas.drawOval(RectF(-size * 0.60f, -size * 0.20f, size * 0.60f, size * 0.20f), stroke)
                fill.color = Color.WHITE
                canvas.drawOval(RectF(-size * 0.36f, -size * 0.14f, size * 0.36f, size * 0.08f), fill)
            }
            PropType.UMBRELLA -> {
                fill.color = baseColor
                val dome = RectF(-size * 0.78f, -size * 0.90f, size * 0.78f, -size * 0.05f)
                canvas.drawArc(dome, 180f, 180f, true, fill)
                canvas.drawArc(dome, 180f, 180f, true, stroke)
                stroke.color = 0xFF1E293B.toInt()
                stroke.strokeWidth = 3.5f
                canvas.drawLine(0f, -size * 0.45f, 0f, size * 0.45f, stroke)
            }
            PropType.HURRICANE -> {
                fill.color = 0xFFFDE047.toInt()
                canvas.drawRoundRect(RectF(-size * 0.26f, -size * 0.40f, size * 0.26f, size * 0.18f), 10f, 10f, fill)
                fill.color = 0xFF991B1B.toInt()
                canvas.drawRect(-size * 0.32f, size * 0.18f, size * 0.32f, size * 0.38f, fill)
            }
            PropType.CRICKET_BAT -> {
                fill.color = 0xFFD97706.toInt()
                canvas.drawRoundRect(RectF(-size * 0.14f, -size * 0.20f, size * 0.14f, size * 0.65f), 4f, 4f, fill)
                canvas.drawRoundRect(RectF(-size * 0.14f, -size * 0.20f, size * 0.14f, size * 0.65f), 4f, 4f, stroke)
                fill.color = 0xFF1E293B.toInt()
                canvas.drawRect(-size * 0.05f, -size * 0.68f, size * 0.05f, -size * 0.20f, fill)
            }
            PropType.TREE -> {
                drawCartoonTree(canvas, 0f, size * 0.65f, size * 2.2f, fill, stroke)
            }
        }

        if (isSelected) {
            stroke.color = 0xFFFFB300.toInt()
            stroke.strokeWidth = 3f
            canvas.drawCircle(0f, 0f, size * 0.9f, stroke)
        }
        canvas.restore()
    }

    private fun drawWeather(
        canvas: Canvas,
        w: Float,
        h: Float,
        weather: WeatherType,
        timeMs: Long,
        fill: Paint,
        stroke: Paint
    ) {
        val t = (timeMs % 4000L) / 4000f
        when (weather) {
            WeatherType.RAIN -> {
                stroke.color = Color.argb(145, 175, 225, 255)
                stroke.strokeWidth = 2.2f
                for (i in 0 until 42) {
                    val rx = ((i * 97 + t * 220f) % w)
                    val ry = ((i * 151 + t * h * 2.2f) % h)
                    canvas.drawLine(rx, ry, rx - 10f, ry + 26f, stroke)
                }
            }
            WeatherType.SNOW -> {
                fill.color = Color.argb(210, 255, 255, 255)
                for (i in 0 until 34) {
                    val sx = ((i * 113 + sin(t * 2f * PI + i).toFloat() * 24f) % w + w) % w
                    val sy = ((i * 89 + t * h * 1.1f) % h)
                    canvas.drawCircle(sx, sy, 4.5f, fill)
                }
            }
            WeatherType.WINDY -> {
                stroke.color = Color.argb(90, 225, 245, 255)
                stroke.strokeWidth = 2.5f
                for (i in 0 until 12) {
                    val wx = ((i * 180 + t * w * 2.4f) % (w * 1.4f)) - w * 0.2f
                    val wy = h * (0.15f + (i % 6) * 0.12f)
                    canvas.drawLine(wx, wy, wx + w * 0.14f, wy, stroke)
                }
            }
            WeatherType.LEAVES -> {
                fill.color = 0xFFD97706.toInt()
                for (i in 0 until 20) {
                    val lx = ((i * 137 + sin(t * 2f * PI + i).toFloat() * 40f) % w + w) % w
                    val ly = ((i * 103 + t * h * 1.3f) % h)
                    canvas.drawOval(RectF(lx - 7f, ly - 4f, lx + 7f, ly + 4f), fill)
                }
            }
            WeatherType.CLEAR -> Unit
        }
    }

    private fun drawLightingMood(
        canvas: Canvas,
        w: Float,
        h: Float,
        mood: LightingMood,
        fill: Paint
    ) {
        if (mood == LightingMood.DAYLIGHT) return
        val alpha = (mood.tintAlpha * 255).toInt().coerceIn(0, 200)
        val c = mood.tintHex.toInt()
        fill.color = Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c))
        canvas.drawRect(-w * 0.5f, -h * 0.5f, w * 1.5f, h * 1.5f, fill)
    }

    private fun drawCameraOverlayGuides(
        canvas: Canvas,
        w: Float,
        h: Float,
        camName: String,
        stroke: Paint,
        fill: Paint
    ) {
        // Subtle rule-of-thirds + safe action brackets
        stroke.color = Color.argb(45, 255, 255, 255)
        stroke.strokeWidth = 1.5f
        canvas.drawLine(w / 3f, 0f, w / 3f, h, stroke)
        canvas.drawLine(w * 2f / 3f, 0f, w * 2f / 3f, h, stroke)
        canvas.drawLine(0f, h / 3f, w, h / 3f, stroke)
        canvas.drawLine(0f, h * 2f / 3f, w, h * 2f / 3f, stroke)

        val pad = 18f
        val len = 24f
        stroke.color = 0xCCFFB300.toInt()
        stroke.strokeWidth = 3f
        // Top-left
        canvas.drawLine(pad, pad, pad + len, pad, stroke)
        canvas.drawLine(pad, pad, pad, pad + len, stroke)
        // Top-right
        canvas.drawLine(w - pad, pad, w - pad - len, pad, stroke)
        canvas.drawLine(w - pad, pad, w - pad, pad + len, stroke)
        // Bottom-left
        canvas.drawLine(pad, h - pad, pad + len, h - pad, stroke)
        canvas.drawLine(pad, h - pad, pad, h - pad - len, stroke)
        // Bottom-right
        canvas.drawLine(w - pad, h - pad, w - pad - len, h - pad, stroke)
        canvas.drawLine(w - pad, h - pad, w - pad, h - pad - len, stroke)

        fill.color = 0xCCFFB300.toInt()
        fill.textSize = 26f
        canvas.drawText("CAM: $camName", pad + 8f, pad + 34f, fill)
    }

    private fun blendColor(colorA: Int, colorB: Int, ratio: Float): Int {
        val inv = 1f - ratio
        val r = (Color.red(colorA) * inv + Color.red(colorB) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(colorA) * inv + Color.green(colorB) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(colorA) * inv + Color.blue(colorB) * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
