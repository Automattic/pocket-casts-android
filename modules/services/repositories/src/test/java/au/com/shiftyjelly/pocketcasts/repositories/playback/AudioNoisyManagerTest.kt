package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.core.content.getSystemService
import java.util.concurrent.TimeUnit
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
        override fun onAudioBecomingNoisy() = Unit

        override fun onHdmiAudioDisconnected() {
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
    fun `HDMI disconnect waits before it is reported`() {
        AudioNoisyManager(context, pausesOnHdmiDisconnect = true).register(listener)

        shadowAudioManager.addOutputDevice(hdmiDevice, false)
        shadowAudioManager.removeOutputDevice(hdmiDevice, true)
        ShadowLooper.idleMainLooper(999, TimeUnit.MILLISECONDS)

        assertEquals(0, noisyCount)
    }

    @Test
    fun `HDMI reconnecting within the delay is not reported`() {
        AudioNoisyManager(context, pausesOnHdmiDisconnect = true).register(listener)

        shadowAudioManager.addOutputDevice(hdmiDevice, false)
        shadowAudioManager.removeOutputDevice(hdmiDevice, true)
        shadowAudioManager.addOutputDevice(hdmiDevice, true)
        ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS)

        assertEquals(0, noisyCount)
    }

    @Test
    fun `HDMI ARC and eARC disconnects are reported`() {
        AudioNoisyManager(context, pausesOnHdmiDisconnect = true).register(listener)

        removeOutputDevice(outputDevice(AudioDeviceInfo.TYPE_HDMI_ARC))
        removeOutputDevice(outputDevice(AudioDeviceInfo.TYPE_HDMI_EARC))

        assertEquals(2, noisyCount)
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
        ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS)
    }
}
