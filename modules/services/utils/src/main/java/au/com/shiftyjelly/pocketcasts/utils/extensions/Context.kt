package au.com.shiftyjelly.pocketcasts.utils.extensions

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.PendingIntent
import android.content.Context
import android.view.accessibility.AccessibilityManager

fun Context.getLaunchActivityPendingIntent(): PendingIntent {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
}

fun Context.isScreenReaderOn(): Boolean {
    val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
    return manager != null && manager.isEnabled && manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).isNotEmpty()
}
