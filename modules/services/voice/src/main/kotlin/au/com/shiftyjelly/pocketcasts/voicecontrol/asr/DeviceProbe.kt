package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

import android.os.Build

class DeviceProbe(
    val hardware: String = Build.HARDWARE,
    val socManufacturer: String = Build.SOC_MANUFACTURER,
    val sdkInt: Int = Build.VERSION.SDK_INT,
) {
    val isSnapdragon: Boolean by lazy {
        hardware.lowercase().contains("qcom") || socManufacturer.lowercase().contains("qualcomm")
    }

    val hasNpu: Boolean by lazy { false }

    val apiLevel: Int = sdkInt

    val hasSufficientRam: Boolean by lazy { sdkInt >= 26 }
}
