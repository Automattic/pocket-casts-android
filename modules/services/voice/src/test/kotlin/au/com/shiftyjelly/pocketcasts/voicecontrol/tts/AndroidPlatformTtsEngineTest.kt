package au.com.shiftyjelly.pocketcasts.voicecontrol.tts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidPlatformTtsEngineTest {
    private lateinit var engine: AndroidPlatformTtsEngine
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        engine = AndroidPlatformTtsEngine(context)
    }

    @Test
    fun `release disposes resources without crash`() {
        engine.release()
        // No crash expected
    }
}
