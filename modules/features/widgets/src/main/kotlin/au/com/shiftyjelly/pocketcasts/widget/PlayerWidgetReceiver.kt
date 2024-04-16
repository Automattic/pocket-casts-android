package au.com.shiftyjelly.pocketcasts.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.widget.di.widgetEntryPoint

internal abstract class PlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    abstract val widgetTypeAnalyticsValue: String

    override fun onEnabled(context: Context) {
        val tracker = context.widgetEntryPoint().analyticsTracker()
        tracker.track(AnalyticsEvent.WIDGET_INSTALLED, mapOf("widget_type" to widgetTypeAnalyticsValue))
    }

    override fun onDisabled(context: Context) {
        val tracker = context.widgetEntryPoint().analyticsTracker()
        tracker.track(AnalyticsEvent.WIDGET_UNINSTALLED, mapOf("widget_type" to widgetTypeAnalyticsValue))
    }
}
