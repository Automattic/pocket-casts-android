package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsPropValue
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.settings.ManualCleanupConfirmationDialog
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class ManualCleanupViewModel
@Inject constructor(
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {
    data class State(
        val diskSpaceViews: List<DiskSpaceView> = listOf(
            DiskSpaceView(title = LR.string.unplayed),
            DiskSpaceView(title = LR.string.in_progress),
            DiskSpaceView(title = LR.string.played)
        ),
        val totalSelectedDownloadSize: Long = 0L,
        val deleteButton: DeleteButton = DeleteButton(),
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

    private val episodesToDelete: MutableList<Episode> = mutableListOf()
    private val switchState: BehaviorRelay<Boolean> = BehaviorRelay.createDefault(false)
    private var deleteButtonAction: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            episodeManager.observeDownloadedEpisodes()
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
                        deleteButton = deleteButton
                    )
                }
        }
    }

    fun setup(deleteButtonClickAction: () -> Unit) {
        this.deleteButtonAction = deleteButtonClickAction
        if (!isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.DOWNLOADS_CLEAN_UP_SHOWN)
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
        analyticsTracker.track(AnalyticsEvent.DOWNLOADS_CLEAN_UP_BUTTON_TAPPED)
        deleteButtonAction?.invoke()
    }

    private fun onDeleteConfirmed() {
        if (episodesToDelete.isNotEmpty()) {
            trackCleanupCompleted()
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
            totalSelectedDownloadSize = downloadSize
        )
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun cleanupConfirmationDialog(context: Context) =
        ManualCleanupConfirmationDialog(context = context, onConfirm = ::onDeleteConfirmed)

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

    private fun trackCleanupCompleted() {
        val properties = HashMap<String, AnalyticsPropValue>()
        state.value.diskSpaceViews.forEach {
            when (it.title) {
                LR.string.unplayed -> properties[UNPLAYED_KEY] = AnalyticsPropValue(it.isChecked)
                LR.string.played -> properties[PLAYED_KEY] = AnalyticsPropValue(it.isChecked)
                LR.string.in_progress -> properties[IN_PROGRESS_KEY] = AnalyticsPropValue(it.isChecked)
            }
        }
        properties[INCLUDE_STARRED_KEY] = AnalyticsPropValue(switchState.value ?: false)
        analyticsTracker.track(AnalyticsEvent.DOWNLOADS_CLEAN_UP_COMPLETED, properties)
    }

    companion object {
        private const val INCLUDE_STARRED_KEY = "include_starred"
        private const val PLAYED_KEY = "played"
        private const val IN_PROGRESS_KEY = "in_progress"
        private const val UNPLAYED_KEY = "unplayed"
    }
}
