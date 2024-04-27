package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.widget.data.LargePlayerWidgetState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun LargePlayer(state: LargePlayerWidgetState) {
    val upNextEpisodes = state.upNextEpisodes

    WidgetTheme(state.useDynamicColors) {
        Column(
            modifier = GlanceModifier
                .clickable(OpenPocketCastsAction.action())
                .fillMaxWidth()
                .wrapContentHeight()
                .cornerRadiusCompat(6.dp)
                .background(LocalWidgetTheme.current.background)
                .padding(16.dp),
        ) {
            LargePlayerHeader(state = state)
            if (upNextEpisodes.isNotEmpty()) {
                Spacer(
                    modifier = GlanceModifier.height(12.dp),
                )
                val expectedHeight = when (upNextEpisodes.size) {
                    0 -> 0
                    1 -> 58
                    2 -> 124
                    else -> 190
                }.dp
                LargePlayerQueue(
                    queue = upNextEpisodes,
                    useEpisodeArtwork = state.useEpisodeArtwork,
                    useDynamicColors = state.useDynamicColors,
                    modifier = GlanceModifier.height(expectedHeight),
                )
                Spacer(
                    modifier = GlanceModifier.fillMaxWidth().height(190.dp - expectedHeight),
                )
            } else {
                Column(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(190.dp),
                ) {
                    NonScalingText(
                        text = LocalContext.current.getString(LR.string.widget_nothing_in_up_next),
                        textSize = 16.dp,
                        useDynamicColors = state.useDynamicColors,
                        isBold = true,
                    )
                    Spacer(
                        modifier = GlanceModifier.height(4.dp),
                    )
                    NonScalingText(
                        text = LocalContext.current.getString(LR.string.widget_check_out_discover),
                        textSize = 13.dp,
                        useDynamicColors = state.useDynamicColors,
                        isTransparent = true,
                    )
                }
            }
        }
    }
}
