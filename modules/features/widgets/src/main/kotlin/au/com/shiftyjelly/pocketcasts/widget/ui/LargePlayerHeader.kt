package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import au.com.shiftyjelly.pocketcasts.widget.data.LargePlayerWidgetState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun LargePlayerHeader(
    state: LargePlayerWidgetState,
    modifier: GlanceModifier = GlanceModifier,
) {
    val episode = state.currentEpisode

    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier.fillMaxSize(),
        ) {
            EpisodeImage(
                episode = episode,
                useEpisodeArtwork = state.useEpisodeArtwork,
                size = 116.dp,
            )
            Spacer(
                modifier = GlanceModifier.width(12.dp),
            )
            Column(
                verticalAlignment = Alignment.Vertical.Top,
                modifier = GlanceModifier.defaultWeight().height(116.dp),
            ) {
                NonScalingText(
                    text = episode?.title ?: LocalContext.current.getString(LR.string.widget_no_episode_playing),
                    textSize = 16.dp,
                    useDynamicColors = state.useDynamicColors,
                    isBold = true,
                    modifier = GlanceModifier.padding(end = 32.dp),
                )
                NonScalingText(
                    text = episode?.getTimeLeft(LocalContext.current) ?: " ",
                    textSize = 13.dp,
                    useDynamicColors = state.useDynamicColors,
                    isTransparent = true,
                )
                PlaybackControls(
                    isPlaying = state.isPlaying,
                    buttonHeight = 58.dp,
                    iconPadding = 16.dp,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
        PocketCastsLogo()
    }
}
