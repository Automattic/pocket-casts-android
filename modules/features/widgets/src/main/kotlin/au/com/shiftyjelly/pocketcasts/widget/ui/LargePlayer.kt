package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.widget.data.LargePlayerWidgetState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun LargePlayer(state: LargePlayerWidgetState) {
    val upNextEpisodes = state.upNextEpisodes

    WidgetTheme(state.useDynamicColors) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(350.dp)
                .cornerRadius(6.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .padding(16.dp),
        ) {
            LargePlayerHeader(state = state)
            if (upNextEpisodes.isNotEmpty()) {
                Spacer(
                    modifier = GlanceModifier.height(12.dp),
                )
                LargePlayerQueue(
                    queue = upNextEpisodes,
                    useEpisodeArtwork = state.useEpisodeArtwork,
                    useDynamicColors = state.useDynamicColors,
                )
            } else {
                val secondaryTextColor = if (state.useDynamicColors) {
                    GlanceTheme.colors.onPrimaryContainer
                } else {
                    GlanceTheme.colors.onSecondaryContainer
                }

                Column(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.fillMaxSize().clickable(OpenPocketCastsAction.action()),
                ) {
                    Text(
                        text = LocalContext.current.getString(LR.string.widget_nothing_in_up_next),
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(
                        modifier = GlanceModifier.height(4.dp),
                    )
                    Text(
                        text = LocalContext.current.getString(LR.string.widget_check_out_discover),
                        maxLines = 1,
                        style = TextStyle(color = secondaryTextColor, fontSize = 13.sp),
                    )
                }
            }
        }
    }
}
