package au.com.shiftyjelly.pocketcasts.sharing

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.IntentCompat
import com.automattic.eventhorizon.EndOfYearStorySharedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class ItemSharedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var eventHorizon: EventHorizon

    @Suppress("UNCHECKED_CAST")
    override fun onReceive(context: Context, intent: Intent) {
        val activity = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)?.className
        val event = IntentCompat.getParcelableExtra(intent, ANALYTICS_EVENT_KEY, EndOfYearStorySharedEvent::class.java)?.copy(activity = activity)
        if (event != null) {
            eventHorizon.track(event)
        }
    }

    companion object {
        private const val ANALYTICS_EVENT_KEY = "analytics_event"

        fun intent(
            context: Context,
            event: EndOfYearStorySharedEvent,
        ): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                0, // requestCode
                Intent(context, ItemSharedReceiver::class.java)
                    .putExtra(ANALYTICS_EVENT_KEY, event),
                when {
                    Build.VERSION.SDK_INT >= 31 -> PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    else -> PendingIntent.FLAG_UPDATE_CURRENT
                },
            )
        }
    }
}
