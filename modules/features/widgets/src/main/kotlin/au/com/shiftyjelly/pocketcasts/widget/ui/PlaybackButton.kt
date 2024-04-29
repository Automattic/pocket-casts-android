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
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import au.com.shiftyjelly.pocketcasts.widget.action.controlPlaybackAction
import au.com.shiftyjelly.pocketcasts.widget.data.LocalSource
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PlaybackButton(
    isPlaying: Boolean,
    height: Dp,
    iconPadding: Dp,
    modifier: GlanceModifier = GlanceModifier,
    isClickable: Boolean = true,
) {
    val contentDescription = LocalContext.current.getString(if (isPlaying) LR.string.play_episode else LR.string.pause_episode)
    val source = LocalSource.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(height)
            .applyIf(isClickable) { continuation ->
                continuation
                    .clickable(controlPlaybackAction(isPlaying, source))
                    .semantics { this.contentDescription = contentDescription }
            },
    ) {
        Image(
            provider = ImageProvider(IR.drawable.rounded_rectangle),
            contentDescription = null,
            colorFilter = ColorFilter.tint(LocalWidgetTheme.current.buttonBackground),
            modifier = GlanceModifier.fillMaxSize(),
        )
        Image(
            provider = ImageProvider(if (isPlaying) IR.drawable.ic_widget_pause else IR.drawable.ic_widget_play),
            contentDescription = null,
            colorFilter = ColorFilter.tint(LocalWidgetTheme.current.icon),
            modifier = GlanceModifier.size(height).padding(iconPadding),
        )
    }
}
