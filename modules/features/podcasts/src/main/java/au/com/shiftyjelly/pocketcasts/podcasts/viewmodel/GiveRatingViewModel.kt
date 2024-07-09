package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel.State.Loaded.Stars
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastRatingAddRequest
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class GiveRatingViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val ratingsManager: RatingsManager,
    private val userManager: UserManager,
    private val syncManager: SyncManager,
) : ViewModel() {

    companion object {
        const val NUMBER_OF_EPISODES_LISTENED_REQUIRED_TO_RATE = 2
    }

    sealed class State {
        data object Loading : State()
        data class Loaded(
            val podcastUuid: String,
            val podcastTitle: String,
            private val _stars: Stars?,
        ) : State() {
            val stars: Stars = _stars
                ?: Stars.Zero

            enum class Stars {
                Zero,
                Half,
                One,
                OneAndHalf,
                Two,
                TwoAndHalf,
                Three,
                ThreeAndHalf,
                Four,
                FourAndHalf,
                Five,
            }
        }
        data class AllowedToRate(val podcastUuid: String) : State()
        data class NotAllowedToRate(val podcastUuid: String) : State()
        data object ErrorWhenLoadingPodcast : State()
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    fun checkIfUserCanRatePodcast(
        podcastUuid: String,
        onUserSignedOut: () -> Unit,
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
                        _state.value = State.AllowedToRate(podcastUuid)
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
                val rating = ratingsManager.podcastRatings(podcastUuid).first()
                val stars: Stars? = getStarsFromRating(rating)

                _state.value = State.Loaded(
                    podcastUuid = podcast.uuid,
                    podcastTitle = podcast.title,
                    _stars = stars,
                )
            }
        }
    }

    private fun getStarsFromRating(podcastRatings: PodcastRatings): Stars? {
        val rating = podcastRatings.average ?: 0.0

        val ratingInt = rating.toInt()
        val half = rating % 1 >= 0.5

        return when (ratingInt) {
            0 -> Stars.Zero
            1 -> if (half) Stars.OneAndHalf else Stars.One
            2 -> if (half) Stars.TwoAndHalf else Stars.Two
            3 -> if (half) Stars.ThreeAndHalf else Stars.Three
            4 -> if (half) Stars.FourAndHalf else Stars.Four
            else -> Stars.Five // if the rating is somehow higher than 5, just return 5
        }
    }

    suspend fun submitRating(podcastUuid: String, context: Context, onSuccess: () -> Unit, onError: () -> Unit) {
        if (!Network.isConnected(context)) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Cannot submit rating, no network connection")
            Toast.makeText(
                context,
                context.getString(LR.string.podcast_submit_rating_no_internet),
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        val stars = (state.value as State.Loaded).stars

        try {
            val response = syncManager.addPodcastRating(PodcastRatingAddRequest(podcastUuid, starsToRating(stars)))
            Timber.e("Submitted a rating of ${response.podcastRating} for ${response.podcastUuid}")
            onSuccess()
        } catch (e: Exception) {
            onError()
        }
    }

    fun setRating(rating: Double) {
        val stars = ratingToStars(rating)
        val stateValue = _state.value
        if (stateValue !is State.Loaded) {
            throw IllegalStateException("Cannot set stars when state is not CanRate")
        }
        _state.value = stateValue.copy(_stars = stars)
    }

    private fun ratingToStars(rating: Double) = when {
        rating <= 0 -> Stars.Zero
        rating <= 0.5 -> Stars.Half
        rating <= 1 -> Stars.One
        rating <= 1.5 -> Stars.OneAndHalf
        rating <= 2 -> Stars.Two
        rating <= 2.5 -> Stars.TwoAndHalf
        rating <= 3 -> Stars.Three
        rating <= 3.5 -> Stars.ThreeAndHalf
        rating <= 4 -> Stars.Four
        rating <= 4.5 -> Stars.FourAndHalf
        else -> Stars.Five
    }

    private fun starsToRating(star: Stars): Int = when (star) {
        Stars.One -> { 1 }
        Stars.Two -> { 2 }
        Stars.Three -> { 3 }
        Stars.Four -> { 4 }
        Stars.Five -> { 5 }
        else -> { 5 }
    }
}
