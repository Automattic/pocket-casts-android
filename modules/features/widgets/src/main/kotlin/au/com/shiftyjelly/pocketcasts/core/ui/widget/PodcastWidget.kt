package au.com.shiftyjelly.pocketcasts.core.ui.widget

import au.com.shiftyjelly.pocketcasts.widget.MediumPlayerWidget
import au.com.shiftyjelly.pocketcasts.widget.PlayerWidgetReceiver

//  This receiver has to be kept in this package due to legacy reasons. Old widget was defined here.
internal class PodcastWidget : PlayerWidgetReceiver() {
    override val glanceAppWidget = MediumPlayerWidget()
    override val widgetTypeAnalyticsValue = "player_medium"
}
