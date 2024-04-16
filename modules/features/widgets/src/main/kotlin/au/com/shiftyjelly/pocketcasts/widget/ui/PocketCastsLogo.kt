package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.layout.Box
import androidx.glance.layout.size
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun PocketCastsLogo(
    size: Dp = 28.dp,
) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .clickable(OpenPocketCastsAction.action()),
    ) {
        Image(
            provider = ImageProvider(IR.drawable.ic_circle),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(size),
        )
        Image(
            provider = ImageProvider(IR.drawable.ic_logo_foreground),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.size(size),
        )
    }
}
