package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.settings.ManualCleanupConfirmationDialog
import com.automattic.eventhorizon.DownloadsCleanUpButtonTappedEvent
import com.automattic.eventhorizon.DownloadsCleanUpCompletedEvent
import com.automattic.eventhorizon.DownloadsCleanUpShownEvent
import com.automattic.eventhorizon.EventHorizon
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.rxkotlin.combineLatest
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.collect
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class ManualCleanupViewModel
@Inject constructor(
    private val episodeManager: EpisodeManager,
    private val downloadQueue: DownloadQueue,
    private val eventHorizon: EventHorizon,
) : ViewModel() {
    data class State(
        val diskSpaceViews: List<DiskSpaceView> = listOf(
            DiskSpaceView(title = LR.string.unplayed),
            DiskSpaceView(title = LR.string.in_progress),
            DiskSpaceView(title = LR.string.played),
        ),
        val totalSelectedDownloadSize: Long = 0L,
        val deleteButton: DeleteButton = DeleteButton(),
    ) {
        val unplayed get() = diskSpaceViews.find { it.title == LR.string.unplayed }
        val inProgress get() = diskSpaceViews.find { it.title == LR.string.in_progress }
        val played get() = diskSpaceViews.find { it.title == LR.string.played }

        data class DiskSpaceView(
            @StringRes val title: Int,
            val isChecked: Boolean = false,
            val episodes: List<PodcastEpisode> = emptyList(),
        ) {
            val episodesBytesSize = episodes.sumOf { it.sizeInBytes }
            val episodesSize = episodes.size
        }

        data class DeleteButton(
            val isEnabled: Boolean = false,
        )
    }

    private var isFragmentChangingConfigurations: Boolean = false
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State>
        get() = _state

    private val _snackbarMessage = MutableSharedFlow<Int>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val deleteButton: State.DeleteButton
        get() = State.DeleteButton(isEnabled = episodesToDelete.isNotEmpty())

    private val episodesToDelete: MutableList<PodcastEpisode> = mutableListOf()
    private val switchState: BehaviorRelay<Boolean> = BehaviorRelay.createDefault(false)
    private var deleteButtonAction: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            episodeManager.findDownloadedEpisodesRxFlowable()
                .combineLatest(switchState.toFlowable(BackpressureStrategy.LATEST))
                .collect { result ->
                    val (downloadedEpisodes, isStarredSwitchChecked) = result
                    val downloadedAdjustedForStarred =
                        downloadedEpisodes.filter { !it.isStarred || isStarredSwitchChecked }
                    episodesToDelete.clear()
                    val updatedDiskSpaceViews = EpisodePlayingStatus.values()
                        .mapToDiskSpaceViewsForEpisodes(downloadedAdjustedForStarred)
                    updatedDiskSpaceViews.forEach { updateDeleteList(it.isChecked, it.episodes) }
                    val downloadSize = updatedDiskSpaceViews.filter { it.isChecked }.sumOf { it.episodesBytesSize }
                    _state.value = State(
                        diskSpaceViews = updatedDiskSpaceViews,
                        totalSelectedDownloadSize = downloadSize,
                        deleteButton = deleteButton,
                    )
                }
        }
    }

    fun setup(deleteButtonClickAction: () -> Unit) {
        this.deleteButtonAction = deleteButtonClickAction
        if (!isFragmentChangingConfigurations) {
            eventHorizon.track(DownloadsCleanUpShownEvent)
        }
    }

    fun onDiskSpaceCheckedChanged(
        isChecked: Boolean,
        diskSpaceView: State.DiskSpaceView,
    ) {
        updateDeleteList(isChecked, diskSpaceView.episodes)
        updateDeleteButton()
        updateDiskSpaceCheckedStateAndDownloadSize(diskSpaceView, isChecked)
    }

    fun onStarredSwitchClicked(isChecked: Boolean) {
        switchState.accept(isChecked)
    }

    fun onDeleteButtonClicked() {
        eventHorizon.track(DownloadsCleanUpButtonTappedEvent)
        deleteButtonAction?.invoke()
    }

    private fun onDeleteConfirmed() {
        if (episodesToDelete.isNotEmpty()) {
            trackCleanupCompleted()
            val episodeUuids = episodesToDelete.map(BaseEpisode::uuid)

            viewModelScope.launch(NonCancellable) {
                _snackbarMessage.emit(LR.string.settings_manage_downloads_deleting)
                downloadQueue.cancelAll(episodeUuids, SourceView.DOWNLOADS).join()
                episodeManager.disableAutoDownload(episodesToDelete)
            }
        }
    }

    private fun updateDeleteList(isChecked: Boolean, episodes: List<PodcastEpisode>) {
        if (isChecked) {
            episodesToDelete.addAll(episodes)
        } else {
            episodesToDelete.removeAll(episodes)
        }
    }

    private fun updateDeleteButton() {
        _state.value = _state.value.copy(deleteButton = deleteButton)
    }

    private fun updateDiskSpaceCheckedStateAndDownloadSize(
        diskSpaceView: State.DiskSpaceView,
        isChecked: Boolean,
    ) {
        val updatedDiskSpaceViews = _state.value.diskSpaceViews.map {
            if (it == diskSpaceView) {
                it.copy(isChecked = isChecked)
            } else {
                it
            }
        }
        val downloadSize = updatedDiskSpaceViews.filter { it.isChecked }.sumOf { it.episodesBytesSize }
        _state.value = _state.value.copy(
            diskSpaceViews = updatedDiskSpaceViews,
            totalSelectedDownloadSize = downloadSize,
        )
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun cleanupConfirmationDialog(context: Context) = ManualCleanupConfirmationDialog(context = context, onConfirm = ::onDeleteConfirmed)

    private fun Array<EpisodePlayingStatus>.mapToDiskSpaceViewsForEpisodes(
        episodes: List<PodcastEpisode>,
    ) = map { episodePlayingStatus ->
        _state.value.diskSpaceViews.first { it.title == episodePlayingStatus.mapToDiskSpaceViewTitle() }
            .copy(episodes = episodes.filter { it.playingStatus == episodePlayingStatus })
    }

    private fun EpisodePlayingStatus.mapToDiskSpaceViewTitle() = when (this) {
        EpisodePlayingStatus.NOT_PLAYED -> LR.string.unplayed
        EpisodePlayingStatus.IN_PROGRESS -> LR.string.in_progress
        EpisodePlayingStatus.COMPLETED -> LR.string.played
    }

    private fun trackCleanupCompleted() {
        eventHorizon.track(
            DownloadsCleanUpCompletedEvent(
                unplayed = state.value.unplayed?.isChecked == true,
                inProgress = state.value.inProgress?.isChecked == true,
                played = state.value.played?.isChecked == true,
                includeStarred = switchState.value == true,
            ),
        )
    }
}
