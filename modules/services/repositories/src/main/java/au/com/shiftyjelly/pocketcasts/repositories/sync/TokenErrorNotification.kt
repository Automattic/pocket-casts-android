package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

open class TokenErrorNotification @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
) {

    private var lastSignInErrorNotification: Long? = null

    fun show(intent: Intent) {
        onShowSignInErrorNotificationDebounced {
            analyticsTracker.track(AnalyticsEvent.SIGNED_OUT_ALERT_SHOWN)
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
        val notification = NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_SIGN_IN_ERROR.id)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(context.getString(LR.string.token_refresh_sign_in_error_title))
            .setContentText(context.getString(LR.string.token_refresh_sign_in_error_description))
            .setAutoCancel(true)
            .setSmallIcon(IR.drawable.ic_failedwarning)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context)
            .notify(Settings.NotificationId.SIGN_IN_ERROR.value, notification)
    }

    // Avoid invoking the passed function multiple times in a short period of time
    @Synchronized
    private fun onShowSignInErrorNotificationDebounced(onTokenErrorUiShown: () -> Unit) {
        val now = System.currentTimeMillis()
        // Do not invoke this method more than once every 2 seconds
        val shouldInvoke = lastSignInErrorNotification == null ||
            lastSignInErrorNotification!! < now - (2 * 1000)
        if (shouldInvoke) {
            onTokenErrorUiShown()
        }
        lastSignInErrorNotification = now
    }
}
