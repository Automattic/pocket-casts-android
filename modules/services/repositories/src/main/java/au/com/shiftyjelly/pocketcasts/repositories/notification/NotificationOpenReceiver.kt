package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class NotificationOpenReceiver : BroadcastReceiver() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REVAMP_NOTIFICATION_OPENED -> handleRevampedNotificationOpen(intent, context)
            else -> handleRelayedNotificationOpen(intent, context)
        }
    }

    private fun handleRelayedNotificationOpen(intent: Intent, context: Context) {
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: return

        analyticsTracker.track(
            event = AnalyticsEvent.NOTIFICATION_OPENED,
            properties = mapOf(
                "category" to category,
            ),
        )

        val originalIntent = Intent().apply {
            flags = intent.flags
            action = intent.action
            IntentCompat.getParcelableExtra(intent, EXTRA_ORIGINAL_COMPONENT, ComponentName::class.java)?.let {
                component = it
            }
            putExtras(intent)
            removeExtra(EXTRA_CATEGORY)
            removeExtra(EXTRA_ORIGINAL_COMPONENT)
        }
        tryLaunchIntent(originalIntent, context)
    }

    private fun handleRevampedNotificationOpen(intent: Intent, context: Context) {
        val subCategory = intent.getStringExtra(EXTRA_REVAMPED_TYPE) ?: return

        val notificationType = NotificationType.fromSubCategory(subCategory)
        notificationType?.let {
            analyticsTracker.track(
                event = AnalyticsEvent.NOTIFICATION_OPENED,
                properties = mapOf(
                    "type" to it.analyticsType,
                    "category" to CATEGORY_DEEP_LINK,
                ),
            )
            val target = it.toIntent(context)
            tryLaunchIntent(target, context)
        }
    }

    private fun tryLaunchIntent(intent: Intent, context: Context) {
        try {
            context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } catch (t: Throwable) {
            Timber.w("Failed to launch activity for intent $intent")
        }
    }

    companion object {
        private const val ACTION_REVAMP_NOTIFICATION_OPENED = "au.com.shiftyjelly.pocketcasts.ACTION_REVAMPED_NOTIFICATION_OPENED"
        private const val EXTRA_REVAMPED_TYPE = "au.com.shiftyjelly.pocketcasts.extras.revamped.notification.type"
        private const val EXTRA_CATEGORY = "au.com.shiftyjelly.pocketcasts.extras.notification.category"
        private const val EXTRA_ORIGINAL_COMPONENT = "au.com.shiftyjelly.pocketcasts.extras.notification.component"
        private const val CATEGORY_DEEP_LINK = "DEEP_LINK"
        private const val CATEGORY_EPISODE = "ep"
        private const val CATEGORY_PODCAST = "po"

        fun NotificationType.toBroadcast(context: Context) = Intent(context, NotificationOpenReceiver::class.java).apply {
            action = ACTION_REVAMP_NOTIFICATION_OPENED
            putExtra(EXTRA_REVAMPED_TYPE, this@toBroadcast.subcategory)
        }

        fun toEpisodeIntentRelay(context: Context, intent: Intent) = Intent(context, NotificationOpenReceiver::class.java).copyCommonIntentProps(intent, CATEGORY_EPISODE)

        fun toPodcastIntentRelay(context: Context, intent: Intent) = Intent(context, NotificationOpenReceiver::class.java).copyCommonIntentProps(intent, CATEGORY_PODCAST)

        fun toDeeplinkIntentRelay(context: Context, intent: Intent) = Intent(context, NotificationOpenReceiver::class.java).copyCommonIntentProps(intent, CATEGORY_DEEP_LINK)

        private fun Intent.copyCommonIntentProps(source: Intent, category: String): Intent {
            flags = source.flags
            action = source.action
            putExtras(source)
            source.component?.let {
                putExtra(EXTRA_ORIGINAL_COMPONENT, it)
            }
            putExtra(EXTRA_CATEGORY, category)
            return this
        }
    }
}
