package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import au.com.shiftyjelly.pocketcasts.widget.action.PlayEpisodeAction
import au.com.shiftyjelly.pocketcasts.widget.data.LocalSource
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PlayButton(
    episode: PlayerWidgetEpisode,
    iconPadding: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    val contentDescription = LocalContext.current.getString(LR.string.play_episode)

    RounderCornerBox(
        contentAlignment = Alignment.Center,
        backgroundTint = LocalWidgetTheme.current.buttonBackground,
        modifier = modifier
            .clickable(PlayEpisodeAction.action(episode.uuid, LocalSource.current))
            .semantics { this.contentDescription = contentDescription },
    ) {
        Image(
            provider = ImageProvider(IR.drawable.ic_widget_play),
            contentDescription = null,
            colorFilter = ColorFilter.tint(LocalWidgetTheme.current.icon),
            modifier = GlanceModifier.fillMaxSize().padding(vertical = iconPadding),
        )
    }
}
