package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class GiveRatingViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val ratingsManager: RatingsManager,
    private val userManager: UserManager,
) : ViewModel() {

    sealed class State {
        object Loading : State()
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
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    fun checkIfUserCanRatePodcast(
        podcastUuid: String,
        onUserSignedOut: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        _state.value = State.Loading

        viewModelScope.launch {

            val signInState = userManager.getSignInState().blockingFirst()
            when (signInState) {

                SignInState.SignedOut -> {
                    onUserSignedOut()
                }

                is SignInState.SignedIn -> {
                    val newState = getPodcastInfo(podcastUuid)
                    if (newState != null) {
                        _state.value = newState
                    } else {
                        onFailure("Cannot give rating, unable to fetch podcast with id $podcastUuid")
                    }
                }
            }
        }
    }

    private suspend fun getPodcastInfo(podcastUuid: String): State.Loaded? =
        withContext(Dispatchers.IO) {
            val podcast = podcastManager.findPodcastByUuidSuspend(podcastUuid)
                ?: return@withContext null
            val rating = ratingsManager.podcastRatings(podcastUuid).first()
            val stars = getStarsFromRating(rating)

            return@withContext State.Loaded(
                podcastUuid = podcast.uuid,
                podcastTitle = podcast.title,
                _stars = stars,
            )
        }

    private fun getStarsFromRating(podcastRatings: PodcastRatings): State.Loaded.Stars? {
        val rating = podcastRatings.average ?: 0.0

        val ratingInt = rating.toInt()
        val half = rating % 1 >= 0.5

        return when (ratingInt) {
            0 -> State.Loaded.Stars.Zero
            1 -> if (half) State.Loaded.Stars.OneAndHalf else State.Loaded.Stars.One
            2 -> if (half) State.Loaded.Stars.TwoAndHalf else State.Loaded.Stars.Two
            3 -> if (half) State.Loaded.Stars.ThreeAndHalf else State.Loaded.Stars.Three
            4 -> if (half) State.Loaded.Stars.FourAndHalf else State.Loaded.Stars.Four
            else -> State.Loaded.Stars.Five // if the rating is somehow higher than 5, just return 5
        }
    }

    fun submitRating(context: Context, onSuccess: () -> Unit) {
        if (!Network.isConnected(context)) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Cannot submit rating, no network connection")
            Toast.makeText(
                context,
                context.getString(LR.string.podcast_submit_rating_no_internet),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val stars = (state.value as State.Loaded).stars
        Timber.e("submitRating function not implemented yet, but would have submitted a rating of $stars")
        onSuccess()
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
        rating <= 0 -> State.Loaded.Stars.Zero
        rating <= 0.5 -> State.Loaded.Stars.Half
        rating <= 1 -> State.Loaded.Stars.One
        rating <= 1.5 -> State.Loaded.Stars.OneAndHalf
        rating <= 2 -> State.Loaded.Stars.Two
        rating <= 2.5 -> State.Loaded.Stars.TwoAndHalf
        rating <= 3 -> State.Loaded.Stars.Three
        rating <= 3.5 -> State.Loaded.Stars.ThreeAndHalf
        rating <= 4 -> State.Loaded.Stars.Four
        rating <= 4.5 -> State.Loaded.Stars.FourAndHalf
        else -> State.Loaded.Stars.Five
    }
}
