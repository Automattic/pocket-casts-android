package au.com.shiftyjelly.pocketcasts.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class SystemBatteryRestrictions @Inject constructor(@ApplicationContext private val context: Context) {

    enum class Status {
        Unrestricted,
        Optimized,
        Restricted,
        Other;
        // "Other" occurs when battery use is unrestricted but background processing is restricted
        // The only way I know that users can get into this state is if the app is set to be restricted
        // and the user accepts the dialog from Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        // (that dialog seems to be designed for use with apps that are set to have "Optimized" battery
        // usage).

        companion object {
            internal fun get(isIgnoringOptimizations: Boolean, isBackgroundRestricted: Boolean) = when {
                isIgnoringOptimizations && !isBackgroundRestricted -> Unrestricted
                !isIgnoringOptimizations && !isBackgroundRestricted -> Optimized
                !isIgnoringOptimizations && isBackgroundRestricted -> Restricted
                else -> Other
            }
        }
    }

    val status: Status
        get() {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            val isBackgroundRestricted =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    // User could not enable background restrictions before P
                    false
                } else {
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    activityManager.isBackgroundRestricted
                }

            return Status.get(
                isIgnoringOptimizations = isIgnoringOptimizations,
                isBackgroundRestricted = isBackgroundRestricted
            )
        }

    fun isUnrestricted() = status == Status.Unrestricted

    fun promptToUpdateBatteryRestriction(context: Context) {
        when (status) {
            Status.Unrestricted -> goToAppSettings(context)
            Status.Optimized -> requestUserTurnOffBatteryOptimization(context)
            Status.Restricted -> goToAppSettings(context)
            Status.Other -> goToAppSettings(context)
        }
    }

    private fun requestUserTurnOffBatteryOptimization(context: Context) {
        // We do not want to send this request if the app is not currently Status.Optimized because
        // granting this request will only ignore battery optimizations, it will not remove the
        // app's background restrictions.
        if (status == Status.Restricted) {
            Timber.w(
                "Improperly requesting that the user turn off battery optimization when their current " +
                    "setting is $status. Use SystemBatteryOptimization::goToAppSettings prompt instead"
            )
        }

        val packageName = context.packageName
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = Uri.parse("package:$packageName")
            }
            context.startActivity(intent)
        }
    }

    private fun goToAppSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}
