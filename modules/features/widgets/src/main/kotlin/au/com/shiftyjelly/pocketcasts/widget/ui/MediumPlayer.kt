package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import au.com.shiftyjelly.pocketcasts.widget.data.MediumPlayerWidgetState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun MediumPlayer(state: MediumPlayerWidgetState) {
    WidgetTheme(state.useDynamicColors) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(90.dp)
                .cornerRadiusCompat(6.dp)
                .background(LocalWidgetTheme.current.background)
                .padding(16.dp),
        ) {
            EpisodeImage(
                episode = state.episode,
                useEpisodeArtwork = state.useEpisodeArtwork,
                size = 58.dp,
            )

            Spacer(
                modifier = GlanceModifier.width(15.dp),
            )
            Column(
                verticalAlignment = Alignment.Vertical.Top,
                modifier = GlanceModifier.height(58.dp),
            ) {
                NonScalingText(
                    text = state.episode?.title ?: LocalContext.current.getString(LR.string.widget_no_episode_playing),
                    textSize = 13.dp,
                    useDynamicColors = state.useDynamicColors,
                    isBold = true,
                )
                PlaybackControls(
                    isPlaying = state.isPlaying,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}
