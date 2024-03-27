package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.background
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun PlaybackButton(
    isPlaying: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    Image(
        provider = ImageProvider(if (isPlaying) IR.drawable.widget_pause else IR.drawable.widget_play),
        contentDescription = null,
        modifier = modifier.background(ImageProvider(IR.drawable.ic_circle)),
        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
    )
}
