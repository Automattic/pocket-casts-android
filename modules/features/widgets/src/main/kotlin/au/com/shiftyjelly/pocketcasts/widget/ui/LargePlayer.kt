package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetState

@Composable
internal fun LargePlayer(state: PlayerWidgetState) {
    WidgetTheme(state.useDynamicColors) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.primaryContainer)
                .padding(12.dp),
        ) {
            LargePlayerHeader(state = state)
        }
    }
}
