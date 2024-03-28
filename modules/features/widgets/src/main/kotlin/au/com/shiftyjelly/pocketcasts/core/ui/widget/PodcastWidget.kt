package au.com.shiftyjelly.pocketcasts.core.ui.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import au.com.shiftyjelly.pocketcasts.widget.MediumPlayerWidget

//  This receiver has to be kept in this package due to legacy reasons. Old widget was defined here.
internal class PodcastWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumPlayerWidget()
}
