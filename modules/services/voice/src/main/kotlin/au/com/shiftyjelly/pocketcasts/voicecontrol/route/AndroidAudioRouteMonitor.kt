package au.com.shiftyjelly.pocketcasts.voicecontrol.route

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AndroidAudioRouteMonitor @Inject constructor(
    @ApplicationContext context: Context,
    private val gracePeriodSignal: GracePeriodSignal,
) : AudioRouteMonitor {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mutableRoute = MutableStateFlow(readRoute())

    override val route: StateFlow<AudioRoute> = mutableRoute.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            mutableRoute.value = readRoute()
            gracePeriodSignal.onAudioRouteChanged()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            mutableRoute.value = readRoute()
            gracePeriodSignal.onAudioRouteChanged()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
    }

    private fun readRoute(): AudioRoute {
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).toList()

        return classifyRoute(
            outputDeviceTypes = outputDevices.map { it.type },
            inputDeviceTypes = inputDevices.map { it.type },
        )
    }

    internal companion object {
        fun classifyRoute(
            outputDeviceTypes: List<Int>,
            inputDeviceTypes: List<Int>,
        ): AudioRoute {
            val hasHeadsetInput = inputDeviceTypes.any { it.isHeadsetInput() }
            val hasA2dp = outputDeviceTypes.any { it == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }

            return when {
                outputDeviceTypes.any { it.isHeadsetOutput() } -> AudioRoute.Headset(hasMicrophone = hasHeadsetInput)
                hasA2dp && hasHeadsetInput -> AudioRoute.Headset(hasMicrophone = true)
                hasA2dp -> AudioRoute.BluetoothA2dpOnly
                outputDeviceTypes.any { it == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } -> AudioRoute.Speaker
                else -> AudioRoute.Unknown
            }
        }

        private fun Int.isHeadsetOutput(): Boolean {
            return this == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                this == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                this == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                isBleHeadset()
        }

        private fun Int.isHeadsetInput(): Boolean {
            return this == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                this == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                isBleHeadset()
        }

        private fun Int.isBleHeadset(): Boolean {
            return this == AudioDeviceInfo.TYPE_BLE_HEADSET
        }
    }
}
