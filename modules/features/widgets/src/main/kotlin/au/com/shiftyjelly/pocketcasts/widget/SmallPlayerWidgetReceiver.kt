package au.com.shiftyjelly.pocketcasts.widget

import com.automattic.eventhorizon.WidgetType

internal class SmallPlayerWidgetReceiver : PlayerWidgetReceiver() {
    override val glanceAppWidget = SmallPlayerWidget()
    override val widgetTypeAnalyticsValue = WidgetType.PlayerSmall
}
