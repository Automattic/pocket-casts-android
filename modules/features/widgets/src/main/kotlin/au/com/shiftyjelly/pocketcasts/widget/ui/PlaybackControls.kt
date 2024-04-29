package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.width

@Composable
internal fun PlaybackControls(
    isPlaying: Boolean,
    buttonHeight: Dp,
    iconPadding: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        verticalAlignment = Alignment.Vertical.Bottom,
        modifier = modifier.fillMaxWidth(),
    ) {
        SkipForwardButton(
            height = buttonHeight,
            iconPadding = iconPadding,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(
            modifier = GlanceModifier.width(4.dp),
        )
        PlaybackButton(
            isPlaying = isPlaying,
            height = buttonHeight,
            iconPadding = iconPadding,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(
            modifier = GlanceModifier.width(4.dp),
        )
        SkipBackButton(
            height = buttonHeight,
            iconPadding = iconPadding,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}
