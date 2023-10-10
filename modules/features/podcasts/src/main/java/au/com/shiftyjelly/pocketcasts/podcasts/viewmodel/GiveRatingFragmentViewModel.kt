package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.Network
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class GiveRatingFragmentViewModel @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val userManager: UserManager,
) : ViewModel() {

    enum class State {
        CanRate,
        Loading,
        MustListenMore,
        NoNetwork,
        NotSignedIn,
    }

    private val _state = MutableStateFlow(State.Loading)
    val state = _state.asStateFlow()

    fun checkIfUserCanRatePodcast(
        context: Context,
        podcastUuid: String,
        onFailure: (String) -> Unit
    ) {
        _state.value = State.Loading
        viewModelScope.launch(Dispatchers.IO) {

            val signInState = userManager.getSignInState().blockingFirst()
            when (signInState) {
                is SignInState.SignedIn -> { /* do nothing and proceed */ }
                SignInState.SignedOut -> {
                    _state.value = State.NotSignedIn
                    return@launch
                }
            }

            // Check network after checking if user is signed in because the user will
            // have to connect to the network to sign in
            if (!Network.isConnected(context)) {
                _state.value = State.NoNetwork
                return@launch
            }

            val podcast = podcastManager.findPodcastByUuid(podcastUuid) ?: run {
                onFailure("${this@GiveRatingFragmentViewModel::class.simpleName} is unable to find podcast with uuid $podcastUuid")
                return@launch
            }

            val episodes = episodeManager.findEpisodesByPodcastOrderedSuspend(podcast)

            val numCompleted = episodes.count {
                it.playingStatus == EpisodePlayingStatus.COMPLETED
            }
            val haveToListenTo = min(2, episodes.size)
            val userCanRatePodcast = numCompleted >= haveToListenTo

            _state.value = if (userCanRatePodcast) {
                State.CanRate
            } else {
                State.MustListenMore
            }
        }
    }
}
