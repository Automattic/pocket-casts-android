package au.com.shiftyjelly.pocketcasts.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import au.com.shiftyjelly.pocketcasts.widget.ui.SmallPlayer

class SmallPlayerWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val adapter = PlayerWidgetStateAdapter(context)
        val updatedState = adapter.updateState(id) { it }

        provideContent {
            SmallPlayer(adapter.currentState() ?: updatedState)
        }
    }
}
