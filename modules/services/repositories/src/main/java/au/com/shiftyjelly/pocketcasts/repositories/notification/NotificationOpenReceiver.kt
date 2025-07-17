package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.deeplink.ShowEpisodeDeepLink
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
            ACTION_OLD_NOTIFICATION_OPENED -> handleOtherNotificationOpened(intent, context)
            else -> Unit
        }
    }

    private fun handleOtherNotificationOpened(intent: Intent, context: Context?) {
        analyticsTracker.track(
            event = AnalyticsEvent.NOTIFICATION_OPENED,
            properties = buildMap {
                intent.getStringExtra(EXTRA_CATEGORY)?.let { category ->
                    put("category", category)
                }
            }
        )

        if (context == null) return

        when (val deeplink = intent.extras?.get(EXTRA_DEEPLINK)) {
            is ShowEpisodeDeepLink -> {
                val launchIntent = deeplink.toIntent(context).apply {
                    intent.getStringExtra(EXTRA_APPENDIX)?.let { appendix ->
                        action += appendix
                    }
                }
                tryLaunchIntent(launchIntent, context)
            }
            else -> {
                context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    action = Settings.INTENT_OPEN_APP_NEW_EPISODES
                }?.let {
                    tryLaunchIntent(it, context)
                }
            }
        }
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
                    "category" to CATEGORY_DEEP_LINK
                ),
            )
            val target = it.toIntent(context)
            tryLaunchIntent(target, context)
        }
    }

    private fun tryLaunchIntent(intent: Intent, context: Context) {
        try {
            context.startActivity(intent.apply {  flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } catch (t: Throwable) {
            Timber.w("Failed to launch activity for intent $intent")
        }
    }

    companion object {
        private const val ACTION_REVAMP_NOTIFICATION_OPENED = "au.com.shiftyjelly.pocketcasts.ACTION_REVAMPED_NOTIFICATION_OPENED"
        private const val ACTION_OLD_NOTIFICATION_OPENED = "au.com.shiftyjelly.pocketcasts.ACTION_OLD_NOTIFICATION_OPENED"
        private const val EXTRA_REVAMPED_TYPE = "extras.revamped.notification.type"
        private const val EXTRA_DEEPLINK = "extras.other.notification.payload"
        private const val EXTRA_CATEGORY = "extras.other.category"
        private const val EXTRA_APPENDIX = "extras.other.appendix"
        private const val CATEGORY_DEEP_LINK = "DEEP_LINK"
        private const val CATEGORY_EPISODE = "ep"
        private const val CATEGORY_PODCAST = "po"

        fun NotificationType.toBroadcast(context: Context) = Intent(context, NotificationOpenReceiver::class.java).apply {
            action = ACTION_REVAMP_NOTIFICATION_OPENED
            putExtra(EXTRA_REVAMPED_TYPE, this@toBroadcast.subcategory)
        }

        fun ShowEpisodeDeepLink.toBroadcast(context: Context, actionAppendix: String) = Intent(context, NotificationOpenReceiver::class.java).apply {
            action = ACTION_OLD_NOTIFICATION_OPENED
            putExtra(EXTRA_DEEPLINK, this@toBroadcast)
            putExtra(EXTRA_CATEGORY, CATEGORY_EPISODE)
            putExtra(EXTRA_APPENDIX, actionAppendix)
        }

        fun groupedNotificationsBroadcast(context: Context) = Intent(context, NotificationOpenReceiver::class.java).apply {
            action = ACTION_OLD_NOTIFICATION_OPENED
            putExtra(EXTRA_CATEGORY, CATEGORY_PODCAST)
        }
    }
}
