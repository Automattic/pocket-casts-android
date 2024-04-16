package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp)
            .background(GlanceTheme.colors.primaryContainer),

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
            Spacer(
                modifier = GlanceModifier.height(4.dp),
            )
            NonScalingText(
                text = LocalContext.current.getString(LR.string.player_tab_playing_wide),
                textSize = 13.dp,
                useDynamicColors = state.useDynamicColors,
                alpha = 0.8,
            )
            NonScalingText(
                text = episode?.title ?: LocalContext.current.getString(LR.string.widget_no_episode_playing),
                textSize = 16.dp,
                useDynamicColors = state.useDynamicColors,
                isBold = true,
                modifier = GlanceModifier.padding(end = 16.dp),
            )
            Spacer(
                modifier = GlanceModifier.height(4.dp),
            )
            NonScalingText(
                text = episode?.getTimeLeft(LocalContext.current) ?: " ",
                textSize = 13.dp,
                useDynamicColors = state.useDynamicColors,
                alpha = 0.8,
            )
            PlaybackControls(
                isPlaying = state.isPlaying,
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(
                modifier = GlanceModifier.height(4.dp),
            )
        }
        PocketCastsLogo()
    }
}
