package au.com.shiftyjelly.pocketcasts.widget

internal class LargePlayerWidgetReceiver : PlayerWidgetReceiver() {
    override val glanceAppWidget = LargePlayerWidget()
    override val widgetTypeAnalyticsValue = "player_large"
}
