package au.com.shiftyjelly.pocketcasts.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.widget.data.ClassicPlayerWidgetState
import au.com.shiftyjelly.pocketcasts.widget.data.LargePlayerWidgetState
import au.com.shiftyjelly.pocketcasts.widget.data.MediumPlayerWidgetState
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode
import au.com.shiftyjelly.pocketcasts.widget.data.SmallPlayerWidgetState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class PlayerWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val smallAdapter = SmallPlayerWidgetState.Adapter(context)
    private val mediumAdapter = MediumPlayerWidgetState.Adapter(context)
    private val largeAdapter = LargePlayerWidgetState.Adapter(context)
    private val classicAdapter = ClassicPlayerWidgetState.Adapter(context)
    private val widgetManager = GlanceAppWidgetManager(context)
    private val updateMutex = Mutex()

    suspend fun updateQueue(queue: List<BaseEpisode>) = updateMutex.withLock {
        val episodes = queue.map(PlayerWidgetEpisode::fromBaseEpisode)
        updateSmallWidgets { state -> state.copy(episode = episodes.firstOrNull()) }
        updateMediumWidgets { state -> state.copy(episode = episodes.firstOrNull()) }
        updateLargeWidgets { state -> state.copy(queue = episodes) }
        updateClassicWidgets { state -> state.copy(episode = episodes.firstOrNull()) }
    }

    suspend fun updateIsPlaying(isPlaying: Boolean) = updateMutex.withLock {
        updateSmallWidgets { state -> state.copy(isPlaying = isPlaying) }
        updateMediumWidgets { state -> state.copy(isPlaying = isPlaying) }
        updateLargeWidgets { state -> state.copy(isPlaying = isPlaying) }
        updateClassicWidgets { state -> state.copy(isPlaying = isPlaying) }
    }

    suspend fun updateUseEpisodeArtwork(
        useEpisodeArtwork: Boolean,
    ) = updateMutex.withLock {
        updateSmallWidgets { state -> state.copy(useEpisodeArtwork = useEpisodeArtwork) }
        updateMediumWidgets { state -> state.copy(useEpisodeArtwork = useEpisodeArtwork) }
        updateLargeWidgets { state -> state.copy(useEpisodeArtwork = useEpisodeArtwork) }
        updateClassicWidgets { state -> state.copy(useEpisodeArtwork = useEpisodeArtwork) }
    }

    suspend fun updateUseDynamicColors(
        useDynamicColors: Boolean,
    ) = updateMutex.withLock {
        updateSmallWidgets { state -> state.copy(useDynamicColors = useDynamicColors) }
        updateMediumWidgets { state -> state.copy(useDynamicColors = useDynamicColors) }
        updateLargeWidgets { state -> state.copy(useDynamicColors = useDynamicColors) }
        updateClassicWidgets { state -> state.copy(useDynamicColors = useDynamicColors) }
    }

    suspend fun updateSkipBackwardDuration(seconds: Int) = updateMutex.withLock {
        updateClassicWidgets { state -> state.copy(skipBackwardSeconds = seconds) }
    }

    suspend fun updateSkipForwardDuration(seconds: Int) = updateMutex.withLock {
        updateClassicWidgets { state -> state.copy(skipForwardSeconds = seconds) }
    }

    private suspend fun updateSmallWidgets(
        update: (SmallPlayerWidgetState) -> SmallPlayerWidgetState,
    ) {
        glanceIds<SmallPlayerWidget>().forEach { glanceId ->
            smallAdapter.updateState(glanceId, update)
        }
        SmallPlayerWidget().updateAll(context)
    }

    private suspend fun updateMediumWidgets(
        update: (MediumPlayerWidgetState) -> MediumPlayerWidgetState,
    ) {
        glanceIds<MediumPlayerWidget>().forEach { glanceId ->
            mediumAdapter.updateState(glanceId, update)
        }
        MediumPlayerWidget().updateAll(context)
    }

    private suspend fun updateLargeWidgets(
        update: (LargePlayerWidgetState) -> LargePlayerWidgetState,
    ) {
        glanceIds<LargePlayerWidget>().forEach { glanceId ->
            largeAdapter.updateState(glanceId, update)
        }
        LargePlayerWidget().updateAll(context)
    }

    private suspend fun updateClassicWidgets(
        update: (ClassicPlayerWidgetState) -> ClassicPlayerWidgetState,
    ) {
        glanceIds<ClassicPlayerWidget>().forEach { glanceId ->
            classicAdapter.updateState(glanceId, update)
        }
        ClassicPlayerWidget().updateAll(context)
    }

    private suspend inline fun <reified T : GlanceAppWidget> glanceIds() = widgetManager.getGlanceIds(T::class.java)

    companion object {
        const val EPISODE_LIMIT = LargePlayerWidgetState.EPISODE_LIMIT
    }
}
