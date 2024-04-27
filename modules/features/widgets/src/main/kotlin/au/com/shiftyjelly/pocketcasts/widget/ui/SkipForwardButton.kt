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
import au.com.shiftyjelly.pocketcasts.widget.action.SkipForwardAction
import au.com.shiftyjelly.pocketcasts.widget.data.LocalSource
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun SkipForwardButton(
    iconPadding: Dp,
    modifier: GlanceModifier = GlanceModifier,
    isClickable: Boolean = true,
) {
    val contentDescription = LocalContext.current.getString(LR.string.skip_forward)
    val source = LocalSource.current

    RounderCornerBox(
        contentAlignment = Alignment.Center,
        backgroundTint = LocalWidgetTheme.current.buttonBackground,
        modifier = modifier.applyIf(isClickable) { ifModifier ->
            ifModifier
                .clickable(SkipForwardAction.action(source))
                .semantics { this.contentDescription = contentDescription }
        },
    ) {
        Image(
            provider = ImageProvider(IR.drawable.ic_widget_skip_forward),
            contentDescription = null,
            colorFilter = ColorFilter.tint(LocalWidgetTheme.current.icon),
            modifier = GlanceModifier.fillMaxSize().padding(vertical = iconPadding),
        )
    }
}
