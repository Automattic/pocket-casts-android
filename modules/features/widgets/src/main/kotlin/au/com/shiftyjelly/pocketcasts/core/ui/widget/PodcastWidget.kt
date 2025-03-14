package au.com.shiftyjelly.pocketcasts.core.ui.widget

import au.com.shiftyjelly.pocketcasts.widget.ClassicPlayerWidget
import au.com.shiftyjelly.pocketcasts.widget.PlayerWidgetReceiver

// This receiver needs to be defined in this package due to legacy reasons.
internal class PodcastWidget : PlayerWidgetReceiver() {
    override val glanceAppWidget = ClassicPlayerWidget()
    override val widgetTypeAnalyticsValue = "player_old"
}
