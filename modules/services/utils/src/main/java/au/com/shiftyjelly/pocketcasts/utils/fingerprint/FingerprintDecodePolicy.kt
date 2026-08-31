package au.com.shiftyjelly.pocketcasts.utils.fingerprint

import android.os.Build
import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.utils.config.FirebaseConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FingerprintDecodePolicy @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) {
    fun current(): FingerprintPolicy = resolve(
        sdkInt = Build.VERSION.SDK_INT,
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        hardware = Build.HARDWARE,
        board = Build.BOARD,
        override = remoteConfig.getString(FirebaseConfig.FINGERPRINT_POLICY_OVERRIDE),
    )

    companion object {
        @VisibleForTesting
        fun resolve(
            sdkInt: Int,
            manufacturer: String,
            brand: String,
            hardware: String,
            board: String,
            override: String,
        ): FingerprintPolicy {
            if (!isAffected(sdkInt, manufacturer, brand, hardware, board)) return FingerprintPolicy.PLATFORM
            return parseOverride(override) ?: FingerprintPolicy.TAP_ONLY
        }

        private fun isAffected(
            sdkInt: Int,
            manufacturer: String,
            brand: String,
            hardware: String,
            board: String,
        ): Boolean {
            if (sdkInt > Build.VERSION_CODES.P) return false
            val vendor = "$manufacturer $brand".lowercase()
            val soc = "$hardware $board".lowercase()
            val isHuawei = "huawei" in vendor || "honor" in vendor
            val isHiSilicon = "kirin" in soc || "hi3" in soc
            return isHuawei && isHiSilicon
        }

        private fun parseOverride(value: String): FingerprintPolicy? = FingerprintPolicy.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}
