package au.com.shiftyjelly.pocketcasts.voicecontrol.route

enum class MicExposure { Isolated, Exposed, NoMic }

fun AudioRoute.toMicExposure(): MicExposure = when (this) {
    is AudioRoute.Headset -> if (hasMicrophone) MicExposure.Isolated else MicExposure.NoMic
    AudioRoute.Speaker -> MicExposure.Exposed
    AudioRoute.BluetoothA2dpOnly -> MicExposure.Exposed
    AudioRoute.Unknown -> MicExposure.Exposed
}
