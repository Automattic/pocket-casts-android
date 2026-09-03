package au.com.shiftyjelly.pocketcasts.voicecontrol.route

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class AndroidAudioRouteMonitor @Inject constructor(
    @ApplicationContext context: Context,
    private val gracePeriodSignal: GracePeriodSignal,
) : AudioRouteMonitor {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mutableRoute = MutableStateFlow(readRoute())
    private val scope = CoroutineScope(Dispatchers.Default)
    private var debounceJob: Job? = null

    // Bluetooth devices are enumerated incrementally — output first, then input.
    // A 500ms debounce prevents transient Headset(hasMicrophone=false) → NoMic
    // from killing the engine during the connection gap.
    override val route: StateFlow<AudioRoute> = mutableRoute.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            scheduleRouteUpdate()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            scheduleRouteUpdate()
        }
    }

    private fun scheduleRouteUpdate() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(500L)
            mutableRoute.value = readRoute()
            gracePeriodSignal.onAudioRouteChanged()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
    }

    @Suppress("DEPRECATION") // isBluetoothScoOn deprecated in API 34; used to distinguish enumerated-but-inactive SCO
    private fun readRoute(): AudioRoute {
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).toList()

        return classifyRoute(
            outputDeviceTypes = outputDevices.map { it.type },
            inputDeviceTypes = inputDevices.map { it.type },
            // SCO I/O devices are enumerated for AirPods even when the SCO link is off
            // (A2DP music path). Only treat BT as a live headset mic when SCO is actually on.
            bluetoothScoActive = audioManager.isBluetoothScoOn,
        )
    }

    internal companion object {
        fun classifyRoute(
            outputDeviceTypes: List<Int>,
            inputDeviceTypes: List<Int>,
            bluetoothScoActive: Boolean = false,
        ): AudioRoute {
            val hasWiredOrBleOutput = outputDeviceTypes.any { it.isWiredOrBleHeadsetOutput() }
            val hasWiredOrBleInput = inputDeviceTypes.any { it.isWiredOrBleHeadsetInput() }
            val hasA2dp = outputDeviceTypes.any { it == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            val hasScoDevices = outputDeviceTypes.any { it == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } ||
                inputDeviceTypes.any { it == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }

            return when {
                // Wired / BLE headsets don't need classic SCO.
                hasWiredOrBleOutput || hasWiredOrBleInput ->
                    AudioRoute.Headset(hasMicrophone = hasWiredOrBleInput)

                // Classic BT with a live SCO link — mic is on the headset.
                (hasScoDevices || hasA2dp) && bluetoothScoActive ->
                    AudioRoute.Headset(hasMicrophone = true)

                // A2DP (and/or enumerated-but-inactive SCO) — VoiceAsrEngine must open SCO
                // before capture, otherwise Oboe binds the phone bottom mic.
                hasA2dp || hasScoDevices -> AudioRoute.BluetoothA2dpOnly

                outputDeviceTypes.any { it == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } -> AudioRoute.Speaker

                else -> AudioRoute.Unknown
            }
        }

        private fun Int.isWiredOrBleHeadsetOutput(): Boolean {
            return this == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                this == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                isBleHeadset()
        }

        private fun Int.isWiredOrBleHeadsetInput(): Boolean {
            return this == AudioDeviceInfo.TYPE_WIRED_HEADSET || isBleHeadset()
        }

        private fun Int.isBleHeadset(): Boolean {
            return this == AudioDeviceInfo.TYPE_BLE_HEADSET
        }
    }
}
