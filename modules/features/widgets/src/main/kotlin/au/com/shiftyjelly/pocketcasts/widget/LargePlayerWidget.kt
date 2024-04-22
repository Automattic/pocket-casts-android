package au.com.shiftyjelly.pocketcasts.widget

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.widget.data.LargePlayerWidgetState
import au.com.shiftyjelly.pocketcasts.widget.data.LocalSource
import au.com.shiftyjelly.pocketcasts.widget.ui.LargePlayer

internal class LargePlayerWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val adapter = LargePlayerWidgetState.Adapter(context)
        val updatedState = adapter.updateState(id) { it }

        provideContent {
            CompositionLocalProvider(LocalSource provides SourceView.WIDGET_PLAYER_LARGE) {
                LargePlayer(adapter.currentState() ?: updatedState)
            }
        }
    }
}
