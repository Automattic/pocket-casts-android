package au.com.shiftyjelly.pocketcasts.widget

import com.automattic.eventhorizon.WidgetType

internal class LargePlayerWidgetReceiver : PlayerWidgetReceiver() {
    override val glanceAppWidget = LargePlayerWidget()
    override val widgetTypeAnalyticsValue = WidgetType.PlayerLarge
}
