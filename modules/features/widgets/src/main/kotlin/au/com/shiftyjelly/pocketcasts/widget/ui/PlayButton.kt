package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.widget.action.PlayEpisodeAction
import au.com.shiftyjelly.pocketcasts.widget.data.LocalSource
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun PlayButton(
    episode: PlayerWidgetEpisode,
    modifier: GlanceModifier = GlanceModifier,
) {
    val contentDescription = LocalContext.current.getString(R.string.play_episode)

    Image(
        provider = ImageProvider(IR.drawable.ic_widget_play),
        contentDescription = null,
        modifier = modifier
            .background(ImageProvider(IR.drawable.ic_circle))
            .clickable(PlayEpisodeAction.action(episode.uuid, LocalSource.current))
            .semantics { this.contentDescription = contentDescription },
        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
    )
}
