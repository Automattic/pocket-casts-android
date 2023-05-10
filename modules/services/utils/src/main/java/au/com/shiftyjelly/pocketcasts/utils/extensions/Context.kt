package au.com.shiftyjelly.pocketcasts.utils.extensions

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity

fun Context.getLaunchActivityPendingIntent(): PendingIntent {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
}

fun Context.isScreenReaderOn(): Boolean {
    val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
    return manager != null && manager.isEnabled && manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).isNotEmpty()
}

// From https://stackoverflow.com/a/68423182/1910286
fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

fun Context.isLandscape() =
    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
