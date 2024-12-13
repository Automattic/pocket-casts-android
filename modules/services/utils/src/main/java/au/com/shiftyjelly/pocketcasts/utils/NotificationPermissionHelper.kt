package au.com.shiftyjelly.pocketcasts.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object NotificationPermissionHelper {

    fun checkForNotificationPermission(
        activity: Activity,
        launcher: ActivityResultLauncher<String>,
        onShowRequestPermissionRationale: () -> Unit = {},
        onPermissionGranted: () -> Unit = {},
        onPermissionHandlingNotRequired: () -> Unit = {},
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED -> {
                    onPermissionGranted()
                }

                activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    onShowRequestPermissionRationale()
                }

                else -> {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            onPermissionHandlingNotRequired()
        }
    }

    fun hasNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
