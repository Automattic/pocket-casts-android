package au.com.shiftyjelly.pocketcasts.widget.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import au.com.shiftyjelly.pocketcasts.widget.di.widgetEntryPoint
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class LargePlayerWidgetState(
    val queue: List<PlayerWidgetEpisode> = emptyList(),
    val isPlaying: Boolean = false,
    val useEpisodeArtwork: Boolean = true,
    val useDynamicColors: Boolean = false,
) {
    val currentEpisode get() = queue.firstOrNull()
    val upNextEpisodes get() = queue.drop(1)

    class Adapter(
        private val context: Context,
    ) {
        private val adapter = context.widgetEntryPoint()
            .moshi()
            .newBuilder()
            .add(PlayerWidgetEpisode.AdapterFactory)
            .build()
            .adapter(LargePlayerWidgetState::class.java)

        @Composable
        fun currentState() = androidx.glance.currentState(Key)?.let(adapter::fromJson)

        suspend fun updateState(glanceId: GlanceId, update: (LargePlayerWidgetState) -> LargePlayerWidgetState): LargePlayerWidgetState {
            lateinit var updatedState: LargePlayerWidgetState
            updateAppWidgetState(context, glanceId) { preferences ->
                val currentState = preferences[Key]?.let(adapter::fromJson) ?: getInitialState(context)
                updatedState = update(currentState)
                preferences[Key] = adapter.toJson(updatedState)
            }
            return updatedState
        }

        private companion object {
            val Key = stringPreferencesKey("LargePlayerWidgetState")
        }
    }

    companion object {
        suspend fun getInitialState(context: Context): LargePlayerWidgetState {
            val upNextDao = context.widgetEntryPoint().upNextDao()
            val settings = context.widgetEntryPoint().settings()
            val playbackManager = context.widgetEntryPoint().playbackManager()
            val queue = upNextDao.findUpNextEpisodes(limit = 10).map(PlayerWidgetEpisode::fromBaseEpisode)
            return LargePlayerWidgetState(
                queue = queue,
                isPlaying = playbackManager.isPlaying(),
                useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
                useDynamicColors = settings.useDynamicColorsForWidget.value,
            )
        }
    }
}
