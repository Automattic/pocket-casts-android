package au.com.shiftyjelly.pocketcasts.widget.data

import android.content.Context
import au.com.shiftyjelly.pocketcasts.widget.di.widgetEntryPoint
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.firstOrNull

@JsonClass(generateAdapter = true)
internal data class PlayerWidgetState(
    val queue: List<PlayerWidgetEpisode> = emptyList(),
    val isPlaying: Boolean = false,
    val useEpisodeArtwork: Boolean = true,
    val useDynamicColors: Boolean = false,
) {
    val currentEpisode get() = queue.firstOrNull()
    val upNextEpisodes get() = queue.drop(1)

    companion object {
        suspend fun getInitialState(context: Context): PlayerWidgetState {
            val upNextDao = context.widgetEntryPoint().upNextDao()
            val settings = context.widgetEntryPoint().settings()
            val playbackManager = context.widgetEntryPoint().playbackManager()
            val queue = upNextDao.findUpNextEpisodes(limit = 10).map(PlayerWidgetEpisode::fromBaseEpisode)
            return PlayerWidgetState(
                queue = queue,
                isPlaying = playbackManager.isPlaying(),
                useEpisodeArtwork = settings.useEpisodeArtwork.value,
                useDynamicColors = settings.useDynamicColorsForWidget.value,
            )
        }
    }
}
