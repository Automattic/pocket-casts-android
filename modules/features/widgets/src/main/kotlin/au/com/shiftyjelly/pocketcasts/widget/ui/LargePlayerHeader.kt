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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
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

        val secondaryTextColor = if (state.useDynamicColors) {
            GlanceTheme.colors.onPrimaryContainer
        } else {
            GlanceTheme.colors.onSecondaryContainer
        }
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
            Text(
                text = LocalContext.current.getString(LR.string.player_tab_playing_wide),
                maxLines = 1,
                style = TextStyle(color = secondaryTextColor, fontSize = 14.sp),
            )
            Text(
                text = episode?.title ?: LocalContext.current.getString(LR.string.widget_no_episode_playing),
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.padding(end = 16.dp),
            )
            Spacer(
                modifier = GlanceModifier.height(4.dp),
            )
            Text(
                text = episode?.getTimeLeft(LocalContext.current) ?: " ",
                maxLines = 1,
                style = TextStyle(color = secondaryTextColor, fontSize = 14.sp),
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
