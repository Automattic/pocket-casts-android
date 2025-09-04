package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class NotificationOpenReceiverActivity : AppCompatActivity() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        trackOpen(intent)

        restoreOriginalIntent(intent, this)?.let { restored ->
            tryLaunchIntent(restored)
        }

        finish()
    }

    private fun trackOpen(intent: Intent) {
        val properties = buildMap {
            when (intent.action) {
                ACTION_REVAMP_NOTIFICATION_OPENED -> {
                    intent.getStringExtra(EXTRA_REVAMPED_TYPE)?.let {
                        NotificationType.fromSubCategory(it)?.let { type ->
                            put("type", type.analyticsType)
                            put("category", CATEGORY_DEEP_LINK)
                        }
                    }
                }

                else -> {
                    intent.getStringExtra(EXTRA_CATEGORY)?.let {
                        put("category", it)
                    }
                }
            }
        }
        analyticsTracker.track(AnalyticsEvent.NOTIFICATION_OPENED, properties)
    }

    private fun tryLaunchIntent(intent: Intent) {
        try {
            startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } catch (t: Throwable) {
            Timber.w("Failed to launch activity for intent $intent -- $t")
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

        fun NotificationType.toIntentRelayed(context: Context) = Intent(context, NotificationOpenReceiverActivity::class.java).apply {
            action = ACTION_REVAMP_NOTIFICATION_OPENED
            putExtra(EXTRA_REVAMPED_TYPE, this@toIntentRelayed.subcategory)
        }

        fun toEpisodeIntentRelay(context: Context, intent: Intent) = Intent(context, NotificationOpenReceiverActivity::class.java).copyCommonIntentProps(intent, CATEGORY_EPISODE)

        fun toPodcastIntentRelay(context: Context, intent: Intent) = Intent(context, NotificationOpenReceiverActivity::class.java).copyCommonIntentProps(intent, CATEGORY_PODCAST)

        fun toDeeplinkIntentRelay(context: Context, intent: Intent) = Intent(context, NotificationOpenReceiverActivity::class.java).copyCommonIntentProps(intent, CATEGORY_DEEP_LINK)

        private fun Intent.copyCommonIntentProps(source: Intent, category: String): Intent {
            flags = source.flags
            action = source.action
            data = source.data
            putExtras(source)
            source.component?.let {
                putExtra(EXTRA_ORIGINAL_COMPONENT, it)
            }
            putExtra(EXTRA_CATEGORY, category)
            return this
        }

        fun restoreOriginalIntent(intent: Intent?, context: Context) = when (intent?.action) {
            ACTION_REVAMP_NOTIFICATION_OPENED -> {
                val subCategory = intent.getStringExtra(EXTRA_REVAMPED_TYPE).orEmpty()
                val notificationType = NotificationType.fromSubCategory(subCategory)
                notificationType?.toIntent(context)
            }

            null -> null
            else -> {
                Intent().apply {
                    flags = intent.flags
                    action = intent.action
                    data = intent.data
                    IntentCompat.getParcelableExtra(intent, EXTRA_ORIGINAL_COMPONENT, ComponentName::class.java)?.let {
                        component = it
                    }
                    putExtras(intent)
                    removeExtra(EXTRA_CATEGORY)
                    removeExtra(EXTRA_ORIGINAL_COMPONENT)
                }
            }
        }
    }
}
