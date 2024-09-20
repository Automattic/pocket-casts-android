package au.com.shiftyjelly.pocketcasts.widget

internal class SmallPlayerWidgetReceiver : PlayerWidgetReceiver() {
    override val glanceAppWidget = SmallPlayerWidget()
    override val widgetTypeAnalyticsValue = "player_small"
}
