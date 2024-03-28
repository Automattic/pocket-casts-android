package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun LargePlayerHeader(
    state: PlayerWidgetState,
    modifier: GlanceModifier = GlanceModifier,
) {
    val currentEpisode = state.currentEpisode
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .background(GlanceTheme.colors.primaryContainer),

    ) {
        EpisodeImage(
            episode = state.currentEpisode,
            useRssArtwork = state.useRssArtwork,
            modifier = GlanceModifier.size(132.dp),
        )

        if (currentEpisode != null) {
            Spacer(modifier = GlanceModifier.width(12.dp))
            Column(
                verticalAlignment = Alignment.Vertical.Top,
                modifier = GlanceModifier.height(132.dp),
            ) {
                Text(
                    text = LocalContext.current.getString(LR.string.player_tab_playing_wide),
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 16.sp,
                    ),
                )
                Text(
                    text = currentEpisode.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(
                    modifier = GlanceModifier.height(4.dp),
                )
                Text(
                    text = TimeHelper.getTimeLeft(
                        currentEpisode.playedUpToMs,
                        currentEpisode.durationMs,
                        inProgress = true,
                        LocalContext.current,
                    ).text,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 16.sp,
                    ),
                )

                PlaybackControls(
                    isPlaying = state.isPlaying,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}
