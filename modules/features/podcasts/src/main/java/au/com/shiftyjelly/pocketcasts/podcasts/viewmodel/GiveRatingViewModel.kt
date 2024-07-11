package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel.State.Loaded.Stars
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastRatingAddRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastRatingShowRequest
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class GiveRatingViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val userManager: UserManager,
    private val syncManager: SyncManager,
) : ViewModel() {

    companion object {
        const val TAG = "GiveRating"
        const val NUMBER_OF_EPISODES_LISTENED_REQUIRED_TO_RATE = 2
    }

    sealed class State {
        data object Loading : State()
        data class Loaded(
            val podcastUuid: String,
            val podcastTitle: String,
            val previousRate: Stars?,
            private val _currentSelectedRate: Stars?,
        ) : State() {
            val currentSelectedRate: Stars = _currentSelectedRate ?: Stars.Zero

            enum class Stars {
                Zero,
                One,
                Two,
                Three,
                Four,
                Five,
            }
        }
        data class NotAllowedToRate(val podcastUuid: String) : State()
        data object ErrorWhenLoadingPodcast : State()
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    fun checkIfUserCanRatePodcast(
        podcastUuid: String,
        onUserSignedOut: () -> Unit,
        onSuccess: () -> Unit,
    ) {
        _state.value = State.Loading

        viewModelScope.launch {
            val signInState = userManager.getSignInState().blockingFirst()
            if (signInState == SignInState.SignedOut) {
                onUserSignedOut()
            } else if (signInState is SignInState.SignedIn) {
                withContext(Dispatchers.IO) {
                    val countPlayedEpisodes = podcastManager.countPlayedEpisodes(podcastUuid)

                    if (countPlayedEpisodes < NUMBER_OF_EPISODES_LISTENED_REQUIRED_TO_RATE) {
                        _state.value = State.NotAllowedToRate(podcastUuid)
                    } else {
                        onSuccess()
                    }
                }
            }
        }
    }

    fun loadData(podcastUuid: String) {
        viewModelScope.launch {
            _state.value = State.Loading

            val podcast = podcastManager.findPodcastByUuidSuspend(podcastUuid)

            if (podcast == null) {
                _state.value = State.ErrorWhenLoadingPodcast
            } else {
                try {
                    val ratedPodcast = syncManager.getPodcastRating(PodcastRatingShowRequest(podcastUuid))

                    _state.value = State.Loaded(
                        podcastUuid = podcast.uuid,
                        podcastTitle = podcast.title,
                        _currentSelectedRate = ratingToStars(ratedPodcast.podcastRating.toDouble()),
                        previousRate = ratingToStars(ratedPodcast.podcastRating.toDouble()),
                    )
                } catch (e: HttpException) {
                    if (e.code() == 404) { // Does not have previous rate
                        _state.value = State.Loaded(
                            podcastUuid = podcast.uuid,
                            podcastTitle = podcast.title,
                            _currentSelectedRate = null,
                            previousRate = null,
                        )
                    } else {
                        LogBuffer.e(TAG, "HTTP error when fetching previous rating for $podcastUuid: " + e.message())
                        _state.value = State.ErrorWhenLoadingPodcast
                    }
                } catch (e: Exception) {
                    LogBuffer.e(TAG, "Error when fetching previous rating for: $podcastUuid")
                    _state.value = State.ErrorWhenLoadingPodcast
                }
            }
        }
    }

    suspend fun submitRating(podcastUuid: String, context: Context, onSuccess: () -> Unit, onError: () -> Unit) {
        if (!Network.isConnected(context)) {
            LogBuffer.i(TAG, "Cannot submit rating, no network connection")
            Toast.makeText(
                context,
                context.getString(LR.string.podcast_submit_rating_no_internet),
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        val stars = (state.value as State.Loaded).currentSelectedRate

        try {
            val response = syncManager.addPodcastRating(PodcastRatingAddRequest(podcastUuid, starsToRating(stars)))
            LogBuffer.i(TAG, "Submitted a rating of ${response.podcastRating} for ${response.podcastUuid}")
            onSuccess()
        } catch (e: Exception) {
            LogBuffer.e(TAG, "Error when submitting rating for: $podcastUuid")
            onError()
        }
    }

    fun setRating(rating: Double) {
        val stars = ratingToStars(rating)
        val stateValue = _state.value
        if (stateValue !is State.Loaded) {
            throw IllegalStateException("Cannot set stars when state is not Loaded")
        }
        _state.value = stateValue.copy(_currentSelectedRate = stars)
    }
}

fun ratingToStars(rating: Double) = when {
    rating <= 0 -> Stars.Zero
    rating <= 1 -> Stars.One
    rating <= 2 -> Stars.Two
    rating <= 3 -> Stars.Three
    rating <= 4 -> Stars.Four
    else -> Stars.Five
}

fun starsToRating(star: Stars): Int = when (star) {
    Stars.One -> { 1 }
    Stars.Two -> { 2 }
    Stars.Three -> { 3 }
    Stars.Four -> { 4 }
    Stars.Five -> { 5 }
    else -> { 5 }
}
