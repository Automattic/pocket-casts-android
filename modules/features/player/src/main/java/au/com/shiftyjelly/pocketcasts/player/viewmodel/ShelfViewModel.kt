package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfRowItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfTitle
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Collections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel(assistedFactory = ShelfViewModel.Factory::class)
class ShelfViewModel @AssistedInject constructor(
    @Assisted private val episodeId: String?,
    private val transcriptsManager: TranscriptsManager,
    private val analyticsTracker: AnalyticsTracker,
    private val settings: Settings,
) : ViewModel() {
    private var items = emptyList<ShelfRowItem>()

    private var _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch {
            episodeId?.let {
                transcriptsManager.observerTranscriptForEpisode(episodeId)
                    .distinctUntilChangedBy { it?.episodeUuid }
                    .stateIn(viewModelScope)
                    .collectLatest { transcript ->
                        _uiState.update { it.copy(transcript = transcript) }
                    }
            } ?: _uiState.update { it.copy(transcript = null) }
        }
    }

    fun onShelfItemMove(
        items: List<ShelfRowItem>,
        fromPosition: Int,
        toPosition: Int,
    ): List<ShelfRowItem> {
        val listData = items.toMutableList()

        Timber.d("Swapping $fromPosition to $toPosition")
        Timber.d("List: $listData")
        val selectedShelfItem = listData[fromPosition] as ShelfItem
        if (fromPosition < toPosition) {
            for (index in fromPosition until toPosition) {
                Collections.swap(listData, index, index + 1)
            }
        } else {
            for (index in fromPosition downTo toPosition + 1) {
                Collections.swap(listData, index, index - 1)
            }
        }

        // Make sure the titles are in the right spot
        listData.apply {
            remove(moreActionsTitle)
            remove(shortcutTitle)
            add(4, moreActionsTitle)
            add(0, shortcutTitle)
        }

        this.items = listData.toList()
        onDragComplete(fromPosition, toPosition, selectedShelfItem)
        Timber.d("Swapped: $listData")
        return this.items
    }

    private fun onDragComplete(
        fromIndex: Int,
        toIndex: Int,
        shelfItem: ShelfItem,
    ) {
        trackShelfItemMovedEvent(fromIndex, toIndex, shelfItem)
        settings.shelfItems.set(items.filterIsInstance<ShelfItem>(), updateModifiedAt = true)
    }

    private fun sectionTitleAt(position: Int) =
        if (position < items.indexOf(moreActionsTitle)) AnalyticsProp.Value.SHELF else AnalyticsProp.Value.OVERFLOW_MENU

    fun trackShelfItemMovedEvent(fromPosition: Int, toPosition: Int, shelfItem: ShelfItem) {
        val title = shelfItem.analyticsValue
        val movedFrom = sectionTitleAt(fromPosition)
        val movedTo = sectionTitleAt(toPosition)
        val newPosition = if (movedTo == AnalyticsProp.Value.SHELF) {
            toPosition - 1
        } else {
            toPosition - (items.indexOf(moreActionsTitle) + 1)
        }
        analyticsTracker.track(
            AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_ACTION_MOVED,
            mapOf(
                AnalyticsProp.Key.ACTION to title,
                AnalyticsProp.Key.POSITION to newPosition, // it is the new position in section it was moved to
                AnalyticsProp.Key.MOVED_FROM to movedFrom,
                AnalyticsProp.Key.MOVED_TO to movedTo,
            ),
        )
    }

    fun trackRearrangeFinishedEvent() {
        analyticsTracker.track(AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_FINISHED)
    }

    data class UiState(
        val transcript: Transcript? = null,
    ) {
        val isTranscriptAvailable: Boolean
            get() = if (FeatureFlag.isEnabled(Feature.TRANSCRIPTS)) { transcript != null } else false
    }

    companion object {
        val shortcutTitle = ShelfTitle(LR.string.player_rearrange_actions_shown)
        val moreActionsTitle = ShelfTitle(LR.string.player_rearrange_actions_hidden)
        object AnalyticsProp {
            object Key {
                const val FROM = "from"
                const val ACTION = "action"
                const val MOVED_FROM = "moved_from"
                const val MOVED_TO = "moved_to"
                const val POSITION = "position"
                const val SOURCE = "source"
                const val EPISODE_UUID = "episode_uuid"
            }
            object Value {
                const val SHELF = "shelf"
                const val OVERFLOW_MENU = "overflow_menu"
                const val UNKNOWN = "unknown"
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(episodeId: String?): ShelfViewModel
    }
}
