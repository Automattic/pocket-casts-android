package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        when (intent?.action) {
            ACTION_REVAMP_NOTIFICATION_OPENED -> handleRevampedNotificationOpen(intent, context)
            else -> handleRelayedNotificationOpen(intent ?: Intent(), context)
        }
    }

    private fun handleRelayedNotificationOpen(intent: Intent, context: Context?) {
        val category = intent.getStringExtra(EXTRA_CATEGORY)
        if (context == null || category == null) return

        analyticsTracker.track(
            event = AnalyticsEvent.NOTIFICATION_OPENED,
            properties = buildMap {
                put("category", category)
            },
        )

        val originalIntent = Intent().apply {
            intent.extras?.keySet()?.forEach {
                when (it) {
                    EXTRA_ORIGINAL_ACTION -> {
                        action = intent.getStringExtra(it)
                        intent.removeExtra(it)
                    }

                    EXTRA_ORIGINAL_FLAGS -> {
                        flags = intent.getIntExtra(it, 0)
                        intent.removeExtra(it)
                    }

                    EXTRA_ORIGINAL_COMPONENT -> {
                        component =
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(it)
                        intent.removeExtra(it)
                    }

                    EXTRA_CATEGORY -> intent.removeExtra(it)
                    else -> Unit
                }
            }
            putExtras(intent.extras ?: Bundle())
        }
        tryLaunchIntent(originalIntent, context)
    }

    private fun handleRevampedNotificationOpen(intent: Intent, context: Context?) {
        if (context == null) return
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
        private const val EXTRA_REVAMPED_TYPE = "extras.revamped.notification.type"
        private const val EXTRA_CATEGORY = "extras.notification.category"
        private const val EXTRA_ORIGINAL_COMPONENT = "extras.notification.component"
        private const val EXTRA_ORIGINAL_FLAGS = "extras.notification.flags"
        private const val EXTRA_ORIGINAL_ACTION = "extras.notification.action"
        private const val CATEGORY_DEEP_LINK = "DEEP_LINK"
        private const val CATEGORY_EPISODE = "ep"
        private const val CATEGORY_PODCAST = "po"

        fun NotificationType.toBroadcast(context: Context) = Intent(context, NotificationOpenReceiver::class.java).apply {
            action = ACTION_REVAMP_NOTIFICATION_OPENED
            putExtra(EXTRA_REVAMPED_TYPE, this@toBroadcast.subcategory)
        }

        fun toEpisodeIntentRelay(context: Context, intent: Intent) = Intent(context, NotificationOpenReceiver::class.java).apply {
            putExtras(intent)
            putExtra(EXTRA_CATEGORY, CATEGORY_EPISODE)
            putExtra(EXTRA_ORIGINAL_FLAGS, intent.flags)
            intent.component?.let {
                putExtra(EXTRA_ORIGINAL_COMPONENT, it)
            }
            intent.action?.let {
                putExtra(EXTRA_ORIGINAL_ACTION, it)
            }
        }

        fun toPodcastIntentRelay(context: Context, intent: Intent) = Intent(context, NotificationOpenReceiver::class.java).apply {
            putExtras(intent)
            putExtra(EXTRA_CATEGORY, CATEGORY_PODCAST)
            putExtra(EXTRA_ORIGINAL_FLAGS, intent.flags)
            intent.component?.let {
                putExtra(EXTRA_ORIGINAL_COMPONENT, it)
            }
            intent.action?.let {
                putExtra(EXTRA_ORIGINAL_ACTION, it)
            }
        }

        fun toDeeplinkIntentRelay(context: Context, intent: Intent) = Intent(context, NotificationOpenReceiver::class.java).apply {
            putExtras(intent)
            putExtra(EXTRA_CATEGORY, CATEGORY_DEEP_LINK)
            putExtra(EXTRA_ORIGINAL_FLAGS, intent.flags)
            intent.component?.let {
                putExtra(EXTRA_ORIGINAL_COMPONENT, it)
            }
            intent.action?.let {
                putExtra(EXTRA_ORIGINAL_ACTION, it)
            }
        }
    }
}
