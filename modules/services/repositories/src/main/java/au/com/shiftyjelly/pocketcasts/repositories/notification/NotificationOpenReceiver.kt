package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class NotificationOpenReceiver : BroadcastReceiver() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_NOTIFICATION_OPENED || context == null) return
        val subCategory = intent.getStringExtra(EXTRA_TYPE) ?: return

        val notificationType = NotificationType.fromSubCategory(subCategory)
        notificationType?.let {
            analyticsTracker.track(
                event = AnalyticsEvent.NOTIFICATION_OPENED,
                properties = mapOf(
                    "type" to it.analyticsType,
                    "category" to CATEGORY_DEEP_LINK
                ),
            )
            val target = it.toIntent(context).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            try {
                context.startActivity(target)
            } catch (t: Throwable) {
                Timber.w("Failed to launch activity for intent $target")
            }
        }
    }

    companion object {
        private const val ACTION_NOTIFICATION_OPENED = "au.com.shiftyjelly.pocketcasts.ACTION_NOTIFICATION_OPENED"
        private const val EXTRA_TYPE = "extras.notification.type"
        private const val CATEGORY_DEEP_LINK = "DEEP_LINK"

        fun NotificationType.toBroadcast(context: Context) = Intent(context, NotificationOpenReceiver::class.java).apply {
            action = ACTION_NOTIFICATION_OPENED
            putExtra(EXTRA_TYPE, this@toBroadcast.subcategory)
        }
    }
}
