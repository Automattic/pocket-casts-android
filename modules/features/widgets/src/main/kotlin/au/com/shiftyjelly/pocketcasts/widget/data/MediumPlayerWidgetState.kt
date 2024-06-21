package au.com.shiftyjelly.pocketcasts.widget.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import au.com.shiftyjelly.pocketcasts.widget.di.widgetEntryPoint
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MediumPlayerWidgetState(
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
            .adapter(MediumPlayerWidgetState::class.java)

        @Composable
        fun currentState() = androidx.glance.currentState(Key)?.let(adapter::fromJson)

        suspend fun updateState(glanceId: GlanceId, update: (MediumPlayerWidgetState) -> MediumPlayerWidgetState): MediumPlayerWidgetState {
            lateinit var updatedState: MediumPlayerWidgetState
            updateAppWidgetState(context, glanceId) { preferences ->
                val currentState = preferences[Key]?.let(adapter::fromJson) ?: getInitialState(context)
                updatedState = update(currentState)
                preferences[Key] = adapter.toJson(updatedState)
            }
            return updatedState
        }

        private companion object {
            val Key = stringPreferencesKey("MediumPlayerWidgetState")
        }
    }

    companion object {
        suspend fun getInitialState(context: Context): MediumPlayerWidgetState {
            val upNextDao = context.widgetEntryPoint().upNextDao()
            val settings = context.widgetEntryPoint().settings()
            val playbackManager = context.widgetEntryPoint().playbackManager()
            val episode = upNextDao.getUpNextBaseEpisodes(limit = 1).map(PlayerWidgetEpisode::fromBaseEpisode).firstOrNull()
            return MediumPlayerWidgetState(
                episode = episode,
                isPlaying = playbackManager.isPlaying(),
                useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
                useDynamicColors = settings.useDynamicColorsForWidget.value,
            )
        }
    }
}
