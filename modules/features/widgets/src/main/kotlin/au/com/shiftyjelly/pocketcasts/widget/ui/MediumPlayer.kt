package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.widget.data.MediumPlayerWidgetState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun MediumPlayer(state: MediumPlayerWidgetState) {
    val expectedHeight = 90.dp
    val widgetHeight = min(expectedHeight, LocalSize.current.height)
    val (coverSize, padding) = if (widgetHeight == expectedHeight) {
        56.dp to 16.dp
    } else {
        (widgetHeight - 24.dp) to 12.dp
    }

    WidgetTheme(state.useDynamicColors) {
        RounderCornerBox(
            contentAlignment = Alignment.TopCenter,
            backgroundTint = LocalWidgetTheme.current.background,
            modifier = GlanceModifier
                .clickable(OpenPocketCastsAction.action())
                .fillMaxWidth()
                .height(widgetHeight),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(padding),
            ) {
                EpisodeImage(
                    episode = state.episode,
                    useEpisodeArtwork = state.useEpisodeArtwork,
                    size = coverSize,
                )
                Spacer(
                    modifier = GlanceModifier.width(4.dp),
                )
                if (state.episode != null) {
                    PlaybackControls(
                        isPlaying = state.isPlaying,
                        buttonHeight = coverSize,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxSize(),
                    ) {
                        NonScalingText(
                            text = LocalContext.current.getString(LR.string.widget_no_episode_playing),
                            textSize = 16.dp,
                            useDynamicColors = state.useDynamicColors,
                            isBold = true,
                        )
                        Spacer(
                            modifier = GlanceModifier.height(2.dp),
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
}
