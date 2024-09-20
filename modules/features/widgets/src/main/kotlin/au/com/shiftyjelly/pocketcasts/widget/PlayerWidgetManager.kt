package au.com.shiftyjelly.pocketcasts.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.widget.data.LargePlayerWidgetState
import au.com.shiftyjelly.pocketcasts.widget.data.MediumPlayerWidgetState
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode
import au.com.shiftyjelly.pocketcasts.widget.data.SmallPlayerWidgetState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PlayerWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val smallAdapter = SmallPlayerWidgetState.Adapter(context)
    private val mediumAdapter = MediumPlayerWidgetState.Adapter(context)
    private val largeAdapter = LargePlayerWidgetState.Adapter(context)
    private val widgetManager = GlanceAppWidgetManager(context)

    fun updateQueue(queue: List<BaseEpisode>) {
        val episodes = queue.map(PlayerWidgetEpisode::fromBaseEpisode)
        updateSmallWidgets { state -> state.copy(episode = episodes.firstOrNull()) }
        updateMediumWidgets { state -> state.copy(episode = episodes.firstOrNull()) }
        updateLargeWidgets { state -> state.copy(queue = episodes) }
    }

    fun updateIsPlaying(isPlaying: Boolean) {
        updateSmallWidgets { state -> state.copy(isPlaying = isPlaying) }
        updateMediumWidgets { state -> state.copy(isPlaying = isPlaying) }
        updateLargeWidgets { state -> state.copy(isPlaying = isPlaying) }
    }

    fun updateUseEpisodeArtwork(useEpisodeArtwork: Boolean) {
        updateSmallWidgets { state -> state.copy(useEpisodeArtwork = useEpisodeArtwork) }
        updateMediumWidgets { state -> state.copy(useEpisodeArtwork = useEpisodeArtwork) }
        updateLargeWidgets { state -> state.copy(useEpisodeArtwork = useEpisodeArtwork) }
    }

    fun updateUseDynamicColors(useDynamicColors: Boolean) {
        updateSmallWidgets { state -> state.copy(useDynamicColors = useDynamicColors) }
        updateMediumWidgets { state -> state.copy(useDynamicColors = useDynamicColors) }
        updateLargeWidgets { state -> state.copy(useDynamicColors = useDynamicColors) }
    }

    private fun updateSmallWidgets(update: (SmallPlayerWidgetState) -> SmallPlayerWidgetState) {
        scope.launch {
            glanceIds<SmallPlayerWidget>().forEach { glanceId -> smallAdapter.updateState(glanceId, update) }
            SmallPlayerWidget().updateAll(context)
        }
    }

    private fun updateMediumWidgets(update: (MediumPlayerWidgetState) -> MediumPlayerWidgetState) {
        scope.launch {
            glanceIds<MediumPlayerWidget>().forEach { glanceId -> mediumAdapter.updateState(glanceId, update) }
            MediumPlayerWidget().updateAll(context)
        }
    }

    private fun updateLargeWidgets(update: (LargePlayerWidgetState) -> LargePlayerWidgetState) {
        scope.launch {
            glanceIds<LargePlayerWidget>().forEach { glanceId -> largeAdapter.updateState(glanceId, update) }
            LargePlayerWidget().updateAll(context)
        }
    }

    private suspend inline fun <reified T : GlanceAppWidget> glanceIds() = widgetManager.getGlanceIds(T::class.java)

    companion object {
        const val EPISODE_LIMIT = LargePlayerWidgetState.EPISODE_LIMIT
    }
}
