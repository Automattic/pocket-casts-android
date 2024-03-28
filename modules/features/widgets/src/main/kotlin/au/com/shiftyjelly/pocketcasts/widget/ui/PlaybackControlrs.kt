package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width

@Composable
internal fun PlaybackControls(
    isPlaying: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        verticalAlignment = Alignment.Vertical.Bottom,
        modifier = modifier.fillMaxWidth(),
    ) {
        SkipBackButton(
            modifier = GlanceModifier.size(42.dp).padding(8.dp),
        )
        Spacer(
            modifier = GlanceModifier.width(8.dp),
        )
        PlaybackButton(
            isPlaying,
            modifier = GlanceModifier.size(42.dp).padding(6.dp),
        )
        Spacer(
            modifier = GlanceModifier.width(8.dp),
        )
        SkipForwardButton(
            modifier = GlanceModifier.size(42.dp).padding(8.dp),
        )
    }
}
