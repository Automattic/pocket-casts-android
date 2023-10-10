package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GiveRatingFragmentViewModel @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager
) : ViewModel() {

    enum class State {
        Loading,
        CanRate,
        MustListenMore,
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    fun checkIfUserCanRatePodcast(
        podcastUuid: String,
        onFailure: (String) -> Unit
    ) {
        _state.value = State.Loading
        viewModelScope.launch(Dispatchers.IO) {

            val podcast = podcastManager.findPodcastByUuid(podcastUuid) ?: run {
                onFailure("${this@GiveRatingFragmentViewModel::class.simpleName} is unable to find podcast with uuid $podcastUuid")
                return@launch
            }

            val episodes = episodeManager.findEpisodesByPodcastOrderedSuspend(podcast)
            val numCompleted = episodes.count {
                it.playingStatus == EpisodePlayingStatus.COMPLETED
            }

            val userCanRatePodcast = run {
                val numEpisodes = episodes.size
                if (numEpisodes > 1) {
                    numCompleted > 1
                } else {
                    numCompleted == numEpisodes
                }
            }

            _state.value = if (userCanRatePodcast) {
                State.CanRate
            } else {
                State.MustListenMore
            }
        }
    }
}
