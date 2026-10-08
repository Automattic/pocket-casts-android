package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.core.content.getSystemService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class AudioNoisyManagerTest {

    private val context = RuntimeEnvironment.getApplication()
    private val shadowAudioManager = shadowOf(requireNotNull(context.getSystemService<AudioManager>()))
    private val hdmiDevice = outputDevice(AudioDeviceInfo.TYPE_HDMI)
    private val speakerDevice = outputDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)

    private var noisyCount = 0
    private val listener = object : AudioNoisyManager.AudioBecomingNoisyListener {
        override fun onAudioBecomingNoisy() {
            noisyCount++
        }
    }

    @Test
    fun `HDMI disconnect is reported as noisy when enabled`() {
        AudioNoisyManager(context, pausesOnHdmiDisconnect = true).register(listener)

        removeOutputDevice(hdmiDevice)

        assertEquals(1, noisyCount)
    }

    @Test
    fun `non HDMI disconnect is ignored`() {
        AudioNoisyManager(context, pausesOnHdmiDisconnect = true).register(listener)

        removeOutputDevice(speakerDevice)

        assertEquals(0, noisyCount)
    }

    @Test
    fun `HDMI input disconnect is ignored`() {
        AudioNoisyManager(context, pausesOnHdmiDisconnect = true).register(listener)

        removeOutputDevice(outputDevice(AudioDeviceInfo.TYPE_HDMI, isSink = false))

        assertEquals(0, noisyCount)
    }

    @Test
    fun `HDMI disconnect is ignored when disabled`() {
        AudioNoisyManager(context).register(listener)

        removeOutputDevice(hdmiDevice)

        assertEquals(0, noisyCount)
    }

    @Test
    fun `HDMI disconnect is ignored after unregistering`() {
        val manager = AudioNoisyManager(context, pausesOnHdmiDisconnect = true)
        manager.register(listener)
        manager.unregister()

        removeOutputDevice(hdmiDevice)

        assertEquals(0, noisyCount)
    }

    private fun outputDevice(type: Int, isSink: Boolean = true) = mock<AudioDeviceInfo> {
        on { this.type } doReturn type
        on { this.isSink } doReturn isSink
    }

    private fun removeOutputDevice(device: AudioDeviceInfo) {
        shadowAudioManager.addOutputDevice(device, false)
        shadowAudioManager.removeOutputDevice(device, true)
        ShadowLooper.idleMainLooper()
    }
}
