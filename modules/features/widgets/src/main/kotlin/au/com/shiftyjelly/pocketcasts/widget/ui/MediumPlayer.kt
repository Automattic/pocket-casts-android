package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
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
import au.com.shiftyjelly.pocketcasts.widget.action.OpenEpisodeDetailsAction
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.widget.data.MediumPlayerWidgetState

@Composable
internal fun MediumPlayer(state: MediumPlayerWidgetState) {
    WidgetTheme(state.useDynamicColors) {
        val action = if (state.episode == null) {
            OpenPocketCastsAction.action()
        } else {
            OpenEpisodeDetailsAction.action(state.episode.uuid)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(90.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .padding(16.dp),
        ) {
            EpisodeImage(
                episode = state.episode,
                useEpisodeArtwork = state.useEpisodeArtwork,
                modifier = GlanceModifier
                    .size(58.dp)
                    .clickable(action),
            )

            if (state.episode != null) {
                Spacer(
                    modifier = GlanceModifier.width(16.dp),
                )
                Column(
                    verticalAlignment = Alignment.Vertical.Top,
                    modifier = GlanceModifier.height(58.dp),
                ) {
                    Text(
                        text = state.episode.title,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
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
}