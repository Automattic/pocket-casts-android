package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import kotlin.collections.List

@HiltViewModel
class ShelfSharedViewModel @Inject constructor(
    episodeManager: EpisodeManager,
    playbackManager: PlaybackManager,
    podcastManager: PodcastManager,
    settings: Settings,
) : ViewModel() {
    private val upNextStateObservable: Observable<UpNextQueue.State> =
        playbackManager.upNextQueue.getChangesObservableWithLiveCurrentEpisode(
            episodeManager,
            podcastManager,
        )
            .observeOn(Schedulers.io())

    private val shelfUpNextObservable = upNextStateObservable
        .distinctUntilChanged { t1, t2 ->
            val entry1 = t1 as? UpNextQueue.State.Loaded ?: return@distinctUntilChanged false
            val entry2 = t2 as? UpNextQueue.State.Loaded ?: return@distinctUntilChanged false
            return@distinctUntilChanged (entry1.episode as? PodcastEpisode)?.isStarred == (entry2.episode as? PodcastEpisode)?.isStarred && entry1.episode.episodeStatus == entry2.episode.episodeStatus && entry1.podcast?.isUsingEffects == entry2.podcast?.isUsingEffects
        }

    val uiState = combine(
        settings.shelfItems.flow,
        shelfUpNextObservable.asFlow(),
        ::createUiState,
    ).stateIn(viewModelScope, SharingStarted.Lazily, UiState())

    private fun createUiState(
        shelfItems: List<ShelfItem>,
        shelfUpNext: UpNextQueue.State,
    ): UiState {
        val episode = (shelfUpNext as? UpNextQueue.State.Loaded)?.episode
        return uiState.value.copy(
            shelfItems = shelfItems
                .filter { item ->
                    when (item) {
                        ShelfItem.Report -> FeatureFlag.isEnabled(Feature.REPORT_VIOLATION)
                        ShelfItem.Transcript -> FeatureFlag.isEnabled(Feature.TRANSCRIPTS)
                        else -> true
                    }
                }
                .filter { it.showIf(episode) },
            episode = episode,
        )
    }

    data class UiState(
        val shelfItems: List<ShelfItem> = emptyList(),
        val episode: BaseEpisode? = null,
    )
}
