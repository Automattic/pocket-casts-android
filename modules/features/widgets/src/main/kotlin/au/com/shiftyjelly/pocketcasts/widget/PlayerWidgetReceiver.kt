package au.com.shiftyjelly.pocketcasts.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import au.com.shiftyjelly.pocketcasts.widget.di.widgetEntryPoint
import com.automattic.eventhorizon.WidgetInstalledEvent
import com.automattic.eventhorizon.WidgetType
import com.automattic.eventhorizon.WidgetUninstalledEvent

internal abstract class PlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    abstract val widgetTypeAnalyticsValue: WidgetType

    override fun onEnabled(context: Context) {
        val eventHorizon = context.widgetEntryPoint().eventHorizon()
        eventHorizon.track(
            WidgetInstalledEvent(
                widgetType = widgetTypeAnalyticsValue,
            ),
        )
    }

    override fun onDisabled(context: Context) {
        val eventHorizon = context.widgetEntryPoint().eventHorizon()
        eventHorizon.track(
            WidgetUninstalledEvent(
                widgetType = widgetTypeAnalyticsValue,
            ),
        )
    }
}
