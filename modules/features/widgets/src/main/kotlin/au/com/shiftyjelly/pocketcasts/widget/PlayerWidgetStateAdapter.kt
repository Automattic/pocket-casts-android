package au.com.shiftyjelly.pocketcasts.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetState
import au.com.shiftyjelly.pocketcasts.widget.di.widgetEntryPoint

internal class PlayerWidgetStateAdapter(
    private val context: Context,
) {
    private val adapter = context.widgetEntryPoint()
        .moshi()
        .newBuilder()
        .add(PlayerWidgetEpisode.AdapterFactory)
        .build()
        .adapter(PlayerWidgetState::class.java)

    @Composable
    fun currentState() = currentState(Key)?.let(adapter::fromJson)

    suspend fun updateState(glanceId: GlanceId, update: (PlayerWidgetState) -> PlayerWidgetState): PlayerWidgetState {
        lateinit var updatedState: PlayerWidgetState
        updateAppWidgetState(context, glanceId) { preferences ->
            val currentState = preferences[Key]?.let(adapter::fromJson) ?: PlayerWidgetState.getInitialState(context)
            updatedState = update(currentState)
            preferences[Key] = adapter.toJson(updatedState)
        }
        return updatedState
    }

    private companion object {
        val Key = stringPreferencesKey("PlayerWidgetState")
    }
}
