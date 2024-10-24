package au.com.shiftyjelly.pocketcasts.sharing

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.IntentCompat
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class ItemSharedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Suppress("UNCHECKED_CAST")
    override fun onReceive(context: Context, intent: Intent) {
        val activity = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)?.className
        val event = IntentCompat.getSerializableExtra(intent, ANALYTICS_EVENT_KEY, AnalyticsEvent::class.java)
        val values = IntentCompat.getSerializableExtra(intent, ANALYTICS_VALUES_KEY, HashMap::class.java) as? HashMap<String, Any>
        if (event != null) {
            analyticsTracker.track(
                AnalyticsEvent.END_OF_YEAR_STORY_SHARED,
                buildMap {
                    if (activity != null) {
                        put("activity", activity)
                    }
                    if (values != null) {
                        putAll(values)
                    }
                },
            )
        }
    }

    companion object {
        private const val ANALYTICS_EVENT_KEY = "analytics_event"
        private const val ANALYTICS_VALUES_KEY = "analytics_values"

        fun intent(
            context: Context,
            event: AnalyticsEvent,
            values: Map<String, Any> = emptyMap(),
        ): PendingIntent {
            val serializableMap = HashMap<String, Any>().apply { putAll(values) }
            return PendingIntent.getBroadcast(
                context,
                0, // requestCode
                Intent(context, ItemSharedReceiver::class.java)
                    .putExtra(ANALYTICS_EVENT_KEY, event)
                    .putExtra(ANALYTICS_VALUES_KEY, serializableMap),
                when {
                    Build.VERSION.SDK_INT >= 31 -> PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    else -> PendingIntent.FLAG_UPDATE_CURRENT
                },
            )
        }
    }
}
