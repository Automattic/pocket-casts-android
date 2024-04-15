package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import au.com.shiftyjelly.pocketcasts.widget.action.SkipForwardAction
import au.com.shiftyjelly.pocketcasts.widget.data.LocalSource
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun SkipForwardButton(
    size: Dp = 36.dp,
) {
    val contentDescription = LocalContext.current.getString(LR.string.skip_forward)

    Box(
        modifier = GlanceModifier
            .size(size)
            .clickable(SkipForwardAction.action(LocalSource.current))
            .semantics { this.contentDescription = contentDescription },
    ) {
        Image(
            provider = ImageProvider(IR.drawable.ic_circle),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(size),
        )
        Image(
            provider = ImageProvider(IR.drawable.ic_widget_skip_forward),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.size(size).padding(size / 5),
        )
    }
}
