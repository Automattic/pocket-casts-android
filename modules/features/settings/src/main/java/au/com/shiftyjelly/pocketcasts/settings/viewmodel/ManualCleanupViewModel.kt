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
        val unplayedDiskSpaceView: DiskSpaceView = DiskSpaceView(title = LR.string.unplayed),
        val inProgressDiskSpaceView: DiskSpaceView = DiskSpaceView(title = LR.string.in_progress),
        val playedDiskSpaceView: DiskSpaceView = DiskSpaceView(title = LR.string.played),
        val totalDownloadSize: Long = 0L,
        val deleteButton: DeleteButton,
    ) {
        data class DiskSpaceView(
            @StringRes val title: Int,
            val episodes: List<Episode> = emptyList(),
        ) {
            val episodesBytesSize = episodes.sumOf { it.sizeInBytes }
            val episodesSize = episodes.size
        }

        data class DeleteButton(
            val title: String,
            @AttrRes val contentColor: Int = UR.attr.primary_interactive_01,
            val isEnabled: Boolean = false,
        )
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
            _state.value.deleteButton.copy(
                title = context.getString(LR.string.settings_select_episodes_to_delete),
                contentColor = UR.attr.primary_interactive_01_disabled,
                isEnabled = false,
            )
        } else {
            _state.value.deleteButton.copy(
                title = context.getString(
                    LR.string.settings_delete_episodes,
                    episodesToDelete.size
                ),
                contentColor = UR.attr.primary_interactive_01,
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
                    val unplayedEpisodes =
                        downloadedAdjustedForStarred.filter { it.playingStatus == EpisodePlayingStatus.NOT_PLAYED }
                    val inProgressEpisodes =
                        downloadedAdjustedForStarred.filter { it.playingStatus == EpisodePlayingStatus.IN_PROGRESS }
                    val playedEpisodes =
                        downloadedAdjustedForStarred.filter { it.playingStatus == EpisodePlayingStatus.COMPLETED }
                    val downloadSize = downloadedAdjustedForStarred.sumOf { it.sizeInBytes }
                    episodesToDelete.clear()
                    _state.value = State(
                        unplayedDiskSpaceView = _state.value.unplayedDiskSpaceView.copy(episodes = unplayedEpisodes),
                        inProgressDiskSpaceView = _state.value.inProgressDiskSpaceView.copy(episodes = inProgressEpisodes),
                        playedDiskSpaceView = _state.value.playedDiskSpaceView.copy(episodes = playedEpisodes),
                        totalDownloadSize = downloadSize,
                        deleteButton = deleteButton
                    )
                }
        }
    }

    fun onDiskSpaceCheckedChanged(isChecked: Boolean, episodes: List<Episode>) {
        updateDeleteList(isChecked, episodes)
        updateDeleteButton()
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

    fun updateDeleteList(isChecked: Boolean, episodes: List<Episode>) {
        if (isChecked) {
            episodesToDelete.addAll(episodes)
        } else {
            episodesToDelete.removeAll(episodes)
        }
    }

    private fun updateDeleteButton() {
        _state.value = _state.value.copy(deleteButton = deleteButton)
    }
}
