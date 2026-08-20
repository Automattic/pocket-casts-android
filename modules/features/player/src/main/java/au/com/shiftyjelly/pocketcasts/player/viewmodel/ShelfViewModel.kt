package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.Companion.MIN_SHELF_ITEMS_SIZE
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfRowItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfTitle
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PlayerShelfOverflowMenuRearrangeActionMovedEvent
import com.automattic.eventhorizon.PlayerShelfOverflowMenuRearrangeFinishedEvent
import com.automattic.eventhorizon.PlayerShelfOverflowMenuRearrangeStartedEvent
import com.automattic.eventhorizon.ShelfActionSourceType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Collections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel(assistedFactory = ShelfViewModel.Factory::class)
class ShelfViewModel @AssistedInject constructor(
    @Assisted private val episodeId: String,
    @Assisted private val isEditable: Boolean,
    private val transcriptManager: TranscriptManager,
    private val userManager: UserManager,
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
) : ViewModel() {
    private var _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState
    private var hasTranscriptRow = false
    private var isEligiblePaidListener = false

    init {
        viewModelScope.launch {
            combine(
                transcriptManager.observeIsTranscriptAvailable(episodeId),
                userManager.getSignInState().asFlow(),
            ) { isAvailable, signInState ->
                isAvailable to signInState.isSignedInAsPlusOrPatron
            }
                .stateIn(viewModelScope)
                .collectLatest { (isAvailable, isPaid) ->
                    hasTranscriptRow = isAvailable
                    isEligiblePaidListener = isPaid
                    updateTranscriptAvailability()
                }
        }
    }

    fun setData(
        items: List<ShelfItem>,
        episode: BaseEpisode?,
    ) {
        if (items.isEmpty()) return
        _uiState.update {
            it.copy(
                isEditable = isEditable,
                shelfRowItems = if (isEditable) {
                    if (items.size < MIN_SHELF_ITEMS_SIZE) {
                        throw IllegalArgumentException(ERROR_MINIMUM_SHELF_ITEMS)
                    }
                    buildList {
                        addAll(items)
                        add(4, moreActionsTitle)
                        add(0, shortcutTitle)
                    }
                } else {
                    items
                },
                episode = episode,
                isTranscriptAvailable = hasTranscriptRow ||
                    (
                        episode is au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode &&
                            isEligiblePaidListener &&
                            FeatureFlag.isEnabled(Feature.ON_DEMAND_TRANSCRIPTS)
                        ),
            )
        }
    }

    private fun updateTranscriptAvailability() {
        _uiState.update { state ->
            state.copy(
                isTranscriptAvailable = hasTranscriptRow ||
                    (
                        state.episode is au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode &&
                            isEligiblePaidListener &&
                            FeatureFlag.isEnabled(Feature.ON_DEMAND_TRANSCRIPTS)
                        ),
            )
        }
    }

    fun onShelfItemMove(
        fromPosition: Int,
        toPosition: Int,
    ) {
        val listData = _uiState.value.shelfRowItems.toMutableList()
        if (toPosition !in listData.indices ||
            fromPosition !in listData.indices ||
            listData[fromPosition] is ShelfTitle ||
            listData[toPosition] is ShelfTitle
        ) {
            throw IllegalArgumentException("$ERROR_SHELF_ITEM_INVALID_MOVE_POSITION from: $fromPosition to: $toPosition")
        }

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

        _uiState.update { it.copy(shelfRowItems = listData.toList()) }
        onDragComplete(fromPosition, toPosition, selectedShelfItem)
        Timber.d("Swapped: $listData")
    }

    private fun onDragComplete(
        fromIndex: Int,
        toIndex: Int,
        shelfItem: ShelfItem,
    ) {
        trackShelfItemMovedEvent(fromIndex, toIndex, shelfItem)
        settings.shelfItems.set(_uiState.value.shelfRowItems.filterIsInstance<ShelfItem>(), updateModifiedAt = true)
    }

    private fun sectionTitleAt(position: Int) = if (position < _uiState.value.shelfRowItems.indexOf(moreActionsTitle)) {
        ShelfActionSource.Shelf
    } else {
        ShelfActionSource.OverflowMenu
    }

    fun onEditButtonClick() {
        eventHorizon.track(PlayerShelfOverflowMenuRearrangeStartedEvent)
    }

    fun onDismiss() {
        eventHorizon.track(PlayerShelfOverflowMenuRearrangeFinishedEvent)
    }

    fun trackShelfItemMovedEvent(fromPosition: Int, toPosition: Int, shelfItem: ShelfItem) {
        val movedFrom = sectionTitleAt(fromPosition)
        val movedTo = sectionTitleAt(toPosition)
        val newPosition = when (movedTo) {
            ShelfActionSource.Shelf -> toPosition - 1
            ShelfActionSource.OverflowMenu -> toPosition - (_uiState.value.shelfRowItems.indexOf(moreActionsTitle) + 1)
        }
        eventHorizon.track(
            PlayerShelfOverflowMenuRearrangeActionMovedEvent(
                action = shelfItem.analyticsValue,
                position = newPosition.toLong(),
                movedFrom = movedFrom.analyticsValue,
                movedTo = movedTo.analyticsValue,
            ),
        )
    }

    data class UiState(
        val isEditable: Boolean = false,
        val shelfRowItems: List<ShelfRowItem> = emptyList(),
        val episode: BaseEpisode? = null,
        val isTranscriptAvailable: Boolean = false,
    )

    enum class ShelfActionSource(
        val analyticsValue: ShelfActionSourceType,
    ) {
        Shelf(
            analyticsValue = ShelfActionSourceType.Shelf,
        ),
        OverflowMenu(
            analyticsValue = ShelfActionSourceType.OverflowMenu,
        ),
    }

    companion object {
        const val ERROR_MINIMUM_SHELF_ITEMS = "Minimum 4 shelf items should be present"
        const val ERROR_SHELF_ITEM_INVALID_MOVE_POSITION = "Shelf item invalid move position"
        val shortcutTitle = ShelfTitle(LR.string.player_rearrange_actions_shown)
        val moreActionsTitle = ShelfTitle(LR.string.player_rearrange_actions_hidden)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            episodeId: String,
            isEditable: Boolean,
        ): ShelfViewModel
    }
}
