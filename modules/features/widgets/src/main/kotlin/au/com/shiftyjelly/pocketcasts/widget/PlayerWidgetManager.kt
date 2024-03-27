package au.com.shiftyjelly.pocketcasts.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PlayerWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val adapter = PlayerWidgetStateAdapter(context)

    fun updateQueue(queue: List<BaseEpisode>) {
        updateWidgetStates { state ->
            state.copy(queue = queue.map(PlayerWidgetEpisode::fromBaseEpisode))
        }
    }

    fun updateIsPlaying(isPlaying: Boolean) {
        updateWidgetStates { state ->
            state.copy(isPlaying = isPlaying)
        }
    }

    fun updateUseRssArtwork(useRssArtwork: Boolean) {
        updateWidgetStates { state ->
            state.copy(useRssArtwork = useRssArtwork)
        }
    }

    fun updateUseDynamicColors(useDynamicColors: Boolean) {
        updateWidgetStates { state ->
            state.copy(useDynamicColors = useDynamicColors)
        }
    }

    private fun updateWidgetStates(update: (PlayerWidgetState) -> PlayerWidgetState) {
        scope.launch {
            glaceIds().forEach { glanceId -> adapter.updateState(glanceId, update) }
            refreshAllWidgets()
        }
    }

    private suspend fun glaceIds() = buildList {
        val manager = GlanceAppWidgetManager(context)
        addAll(manager.getGlanceIds(SmallPlayerWidget::class.java))
    }

    private suspend fun refreshAllWidgets() {
        SmallPlayerWidget().updateAll(context)
    }
}
