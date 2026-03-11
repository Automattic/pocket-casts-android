package au.com.shiftyjelly.pocketcasts.widget

import com.automattic.eventhorizon.WidgetType

internal class MediumPlayerWidgetReceiver : PlayerWidgetReceiver() {
    override val glanceAppWidget = MediumPlayerWidget()
    override val widgetTypeAnalyticsValue = WidgetType.PlayerMedium
}
