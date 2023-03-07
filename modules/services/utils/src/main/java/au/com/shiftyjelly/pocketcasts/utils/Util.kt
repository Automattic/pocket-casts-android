package au.com.shiftyjelly.pocketcasts.utils

import android.app.UiModeManager
import android.content.Context
import android.content.Context.ACCESSIBILITY_SERVICE
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.res.Configuration
import android.os.Build
import android.os.Looper
import android.text.format.Formatter
import android.view.accessibility.AccessibilityManager
import java.util.Locale

object Util {
    private const val MINIMUM_SMALLEST_WIDTH_DP_FOR_TABLET = 570

    fun isCarUiMode(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_CAR
    }

    fun isAutomotive(context: Context): Boolean =
        appInfoHasBoolean("pocketcasts_automotive", context)

    fun isWearOs(context: Context): Boolean =
        appInfoHasBoolean("pocketcasts_wear_os", context)

    private fun appInfoHasBoolean(key: String, context: Context, default: Boolean = false): Boolean {
        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getApplicationInfo(context.packageName, ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        }.metaData
        return appInfo?.getBoolean(key, default) ?: default
    }

    fun isTablet(context: Context) =
        !isAutomotive(context) &&
            context.resources.configuration.smallestScreenWidthDp > MINIMUM_SMALLEST_WIDTH_DP_FOR_TABLET

    fun isTalkbackOn(context: Context): Boolean {
        val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager?
        return am?.isEnabled == true
    }

    @Suppress("NAME_SHADOWING")
    fun formattedSeconds(seconds: Double): String {
        var seconds = seconds
        if (seconds < 0) {
            seconds = 0.0
        }

        val hours = seconds.toLong() / 3600
        val min = seconds.toLong() / 60
        val sec = seconds.toLong() % 60

        return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, min - hours * 60, sec) else String.format(Locale.US, "%02d:%02d", min, sec)
    }

    fun formattedBytes(bytes: Long?, context: Context, minimumBytes: Int = 1): String {
        return if (bytes == null || bytes <= minimumBytes) "-" else Formatter.formatShortFileSize(context, bytes)
    }

    /**
     * Returns `true` if called on the main thread, `false` otherwise.
     */
    fun isOnMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
}
