package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.ProjectRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches ToonMotion Studio`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ToonMotion Studio", appName)
    }

    @Test
    fun `placeholder AI generator creates multi-character scene with transparent notice`() = runTest {
        val repo = PlaceholderAiRepository()
        val chars = ProjectRepository.defaultCharacterLibrary()
        val result = repo.generateAnimationFromPrompt(
            prompt = "Two characters walk in the park, talk, and laugh together",
            availableCharacters = chars
        )
        assertTrue(result.isPlaceholderOrOffline)
        assertTrue(result.noticeMessage.contains("Firebase AI Logic"))
        assertEquals(2, result.scene.characters.size)
        assertTrue(result.scene.motionClips.isNotEmpty())
        assertTrue(result.scene.audioClips.isNotEmpty())
        assertTrue(result.scene.cameraCuts.isNotEmpty())
    }
}
