package au.com.shiftyjelly.pocketcasts.widget

internal class MediumPlayerWidgetReceiver : PlayerWidgetReceiver() {
    override val glanceAppWidget = MediumPlayerWidget()
    override val widgetTypeAnalyticsValue = "player_medium"
}
