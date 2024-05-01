package au.com.shiftyjelly.pocketcasts.widget.ui

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.unit.ColorProvider
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun RounderCornerBox(
    modifier: GlanceModifier = GlanceModifier,
    modifierCompat: GlanceModifier = GlanceModifier.fillMaxSize(),
    contentAlignment: Alignment = Alignment.Center,
    backgroundTint: ColorProvider? = null,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = contentAlignment,
        modifier = modifier
            .cornerRadiusCompat(6.dp)
            .applyIf(isSystemCornerRadiusSupported && backgroundTint != null) { it.background(backgroundTint!!) },
    ) {
        if (!isSystemCornerRadiusSupported) {
            Image(
                provider = ImageProvider(IR.drawable.rounded_rectangle),
                contentDescription = null,
                colorFilter = backgroundTint?.let(ColorFilter::tint),
                modifier = modifierCompat,
            )
        }
        content()
    }
}

internal val isSystemCornerRadiusSupported get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private fun GlanceModifier.cornerRadiusCompat(radius: Dp) = applyIf(isSystemCornerRadiusSupported) { it.cornerRadius(radius) }
