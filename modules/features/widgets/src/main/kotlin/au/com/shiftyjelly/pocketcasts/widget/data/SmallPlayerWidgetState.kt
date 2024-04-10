package au.com.shiftyjelly.pocketcasts.widget.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import au.com.shiftyjelly.pocketcasts.widget.di.widgetEntryPoint
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.firstOrNull

@JsonClass(generateAdapter = true)
internal data class SmallPlayerWidgetState(
    val episode: PlayerWidgetEpisode?,
    val isPlaying: Boolean = false,
    val useEpisodeArtwork: Boolean = true,
    val useDynamicColors: Boolean = false,
) {
    class Adapter(
        private val context: Context,
    ) {
        private val adapter = context.widgetEntryPoint()
            .moshi()
            .newBuilder()
            .add(PlayerWidgetEpisode.AdapterFactory)
            .build()
            .adapter(SmallPlayerWidgetState::class.java)

        @Composable
        fun currentState() = androidx.glance.currentState(Key)?.let(adapter::fromJson)

        suspend fun updateState(glanceId: GlanceId, update: (SmallPlayerWidgetState) -> SmallPlayerWidgetState): SmallPlayerWidgetState {
            lateinit var updatedState: SmallPlayerWidgetState
            updateAppWidgetState(context, glanceId) { preferences ->
                val currentState = preferences[Key]?.let(adapter::fromJson) ?: getInitialState(context)
                updatedState = update(currentState)
                preferences[Key] = adapter.toJson(updatedState)
            }
            return updatedState
        }

        private companion object {
            val Key = stringPreferencesKey("SmallPlayerWidgetState")
        }
    }

    companion object {
        suspend fun getInitialState(context: Context): SmallPlayerWidgetState {
            val upNextDao = context.widgetEntryPoint().upNextDao()
            val settings = context.widgetEntryPoint().settings()
            val playbackManager = context.widgetEntryPoint().playbackManager()
            val episode = upNextDao.findUpNextEpisodes(limit = 1).map(PlayerWidgetEpisode::fromBaseEpisode).firstOrNull()
            return SmallPlayerWidgetState(
                episode = episode,
                isPlaying = playbackManager.isPlaying(),
                useEpisodeArtwork = settings.useEpisodeArtwork.value,
                useDynamicColors = settings.useDynamicColorsForWidget.value,
            )
        }
    }
}
