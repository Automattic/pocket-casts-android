package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.rxkotlin.combineLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.collect
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@HiltViewModel
class ManualCleanupViewModel
@Inject constructor(
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    data class State(
        val diskSpaceViews: List<DiskSpaceView> = listOf(
            DiskSpaceView(title = LR.string.unplayed),
            DiskSpaceView(title = LR.string.in_progress),
            DiskSpaceView(title = LR.string.played)
        ),
        val totalDownloadSize: Long = 0L,
        val deleteButton: DeleteButton,
    ) {
        data class DiskSpaceView(
            @StringRes val title: Int,
            val isChecked: Boolean = false,
            val episodes: List<Episode> = emptyList(),
        ) {
            val episodesBytesSize = episodes.sumOf { it.sizeInBytes }
            val episodesSize = episodes.size
        }

        data class DeleteButton(
            val title: String,
            val isEnabled: Boolean = false,
        ) {
            @AttrRes val contentColor =
                if (isEnabled) UR.attr.primary_interactive_01 else UR.attr.primary_interactive_01_disabled
        }
    }

    private val _state = MutableStateFlow(
        State(
            deleteButton = State.DeleteButton(
                title = context.getString(LR.string.settings_select_episodes_to_delete)
            )
        )
    )
    val state: StateFlow<State>
        get() = _state

    private val _snackbarMessage = MutableSharedFlow<Int>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val deleteButton: State.DeleteButton
        get() = if (episodesToDelete.isEmpty()) {
            State.DeleteButton(
                title = context.getString(LR.string.settings_select_episodes_to_delete),
                isEnabled = false,
            )
        } else {
            State.DeleteButton(
                title = context.getString(
                    LR.string.settings_delete_episodes,
                    episodesToDelete.size
                ),
                isEnabled = true,
            )
        }
    private val episodesToDelete: MutableList<Episode> = mutableListOf()
    private val switchState: BehaviorRelay<Boolean> = BehaviorRelay.createDefault(false)

    init {
        viewModelScope.launch {
            episodeManager.observeDownloadedEpisodes()
                .combineLatest(switchState.toFlowable(BackpressureStrategy.LATEST))
                .collect { result ->
                    val (downloadedEpisodes, isStarredSwitchChecked) = result
                    val downloadedAdjustedForStarred =
                        downloadedEpisodes.filter { !it.isStarred || isStarredSwitchChecked }
                    val downloadSize = downloadedAdjustedForStarred.sumOf { it.sizeInBytes }
                    episodesToDelete.clear()
                    val updatedDiskSpaceViews = EpisodePlayingStatus.values()
                        .mapToDiskSpaceViewsForEpisodes(downloadedAdjustedForStarred)
                    updatedDiskSpaceViews.forEach { updateDeleteList(it.isChecked, it.episodes) }
                    _state.value = State(
                        diskSpaceViews = updatedDiskSpaceViews,
                        totalDownloadSize = downloadSize,
                        deleteButton = deleteButton
                    )
                }
        }
    }

    fun onDiskSpaceCheckedChanged(
        isChecked: Boolean,
        diskSpaceView: State.DiskSpaceView,
    ) {
        updateDeleteList(isChecked, diskSpaceView.episodes)
        updateDeleteButton()
        updateDiskSpaceCheckedState(diskSpaceView, isChecked)
    }

    fun onStarredSwitchClicked(isChecked: Boolean) {
        switchState.accept(isChecked)
    }

    fun onDeleteButtonClicked() {
        if (episodesToDelete.isNotEmpty()) {
            viewModelScope.launch {
                episodeManager.deleteEpisodeFiles(episodesToDelete, playbackManager)
                _snackbarMessage.emit(LR.string.settings_manage_downloads_deleting)
            }
        }
    }

    private fun updateDeleteList(isChecked: Boolean, episodes: List<Episode>) {
        if (isChecked) {
            episodesToDelete.addAll(episodes)
        } else {
            episodesToDelete.removeAll(episodes)
        }
    }

    private fun updateDeleteButton() {
        _state.value = _state.value.copy(deleteButton = deleteButton)
    }

    private fun updateDiskSpaceCheckedState(
        diskSpaceView: State.DiskSpaceView,
        isChecked: Boolean,
    ) {
        _state.value = _state.value.copy(
            diskSpaceViews = _state.value.diskSpaceViews.map {
                if (it == diskSpaceView) {
                    it.copy(isChecked = isChecked)
                } else {
                    it
                }
            }
        )
    }

    private fun Array<EpisodePlayingStatus>.mapToDiskSpaceViewsForEpisodes(
        episodes: List<Episode>,
    ) = map { episodePlayingStatus ->
        _state.value.diskSpaceViews.first { it.title == episodePlayingStatus.mapToDiskSpaceViewTitle() }
            .copy(episodes = episodes.filter { it.playingStatus == episodePlayingStatus })
    }

    private fun EpisodePlayingStatus.mapToDiskSpaceViewTitle() =
        when (this) {
            EpisodePlayingStatus.NOT_PLAYED -> LR.string.unplayed
            EpisodePlayingStatus.IN_PROGRESS -> LR.string.in_progress
            EpisodePlayingStatus.COMPLETED -> LR.string.played
        }
}
