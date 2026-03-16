package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.UserPodcastRating
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel.State.Loaded.Stars
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.ratings.PodcastRatingResult
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.NotAllowedToRateScreenDismissedEvent
import com.automattic.eventhorizon.NotAllowedToRateScreenShownEvent
import com.automattic.eventhorizon.RatingScreenDismissedEvent
import com.automattic.eventhorizon.RatingScreenShownEvent
import com.automattic.eventhorizon.RatingScreenSubmitTappedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class GiveRatingViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val ratingManager: RatingsManager,
    private val eventHorizon: EventHorizon,
    private val syncManager: SyncManager,
) : ViewModel() {

    private var shouldTrackDismissedEvent = false

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

            enum class Stars(
                val value: Int,
            ) {
                Zero(
                    value = 0,
                ),
                One(
                    value = 1,
                ),
                Two(
                    value = 2,
                ),
                Three(
                    value = 3,
                ),
                Four(
                    value = 4,
                ),
                Five(
                    value = 5,
                ),
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

        if (syncManager.isLoggedIn().not()) {
            onUserSignedOut()
            return
        }

        viewModelScope.launch {
            val countPlayedEpisodes = podcastManager.countPlayedEpisodes(podcastUuid)
            if (countPlayedEpisodes < NUMBER_OF_EPISODES_LISTENED_REQUIRED_TO_RATE) {
                val episodes = podcastManager.countEpisodesByPodcast(podcastUuid)
                if (episodes == 1 && countPlayedEpisodes == 1) {
                    onSuccess() // This is the case an user wants to rate a podcast that has only one episode.
                } else {
                    _state.value = State.NotAllowedToRate(podcastUuid)
                }
            } else {
                onSuccess()
            }
        }
    }

    fun loadData(podcastUuid: String) {
        viewModelScope.launch {
            _state.value = State.Loading

            val podcast = podcastManager.findPodcastByUuid(podcastUuid)

            if (podcast == null) {
                _state.value = State.ErrorWhenLoadingPodcast
            } else {
                when (val result = ratingManager.getPodcastRating(podcastUuid)) {
                    is PodcastRatingResult.Success -> {
                        _state.value = State.Loaded(
                            podcastUuid = podcast.uuid,
                            podcastTitle = podcast.title,
                            _currentSelectedRate = ratingToStars(result.rating),
                            previousRate = ratingToStars(result.rating),
                        )
                    }

                    is PodcastRatingResult.NotFound -> {
                        _state.value = State.Loaded(
                            podcastUuid = podcast.uuid,
                            podcastTitle = podcast.title,
                            _currentSelectedRate = null,
                            previousRate = null,
                        )
                    }

                    else -> {
                        LogBuffer.e(TAG, "Error when fetching previous rating for: $podcastUuid")
                        _state.value = State.ErrorWhenLoadingPodcast
                    }
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

        shouldTrackDismissedEvent = false

        val stars = (state.value as State.Loaded).currentSelectedRate

        eventHorizon.track(
            RatingScreenSubmitTappedEvent(
                uuid = (state.value as State.Loaded).podcastUuid,
                stars = stars.value.toLong(),
            ),
        )

        val result = ratingManager.submitPodcastRating(UserPodcastRating(podcastUuid, stars.value, Date()))

        if (result is PodcastRatingResult.Success) {
            LogBuffer.i(TAG, "Submitted a rating of ${result.rating} for $podcastUuid")
            ratingManager.refreshPodcastRatings(podcastUuid = podcastUuid, useCache = false)
            onSuccess()
        } else {
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

    fun trackOnGiveRatingScreenShown(uuid: String) {
        shouldTrackDismissedEvent = true
        eventHorizon.track(
            RatingScreenShownEvent(
                uuid = uuid,
            ),
        )
    }

    fun trackOnNotAllowedToRateScreenShown(uuid: String) {
        shouldTrackDismissedEvent = true
        eventHorizon.track(
            NotAllowedToRateScreenShownEvent(
                uuid = uuid,
            ),
        )
    }

    fun trackRatingDismissedEvent() {
        if (shouldTrackDismissedEvent) {
            eventHorizon.track(RatingScreenDismissedEvent)
        }
    }

    fun trackNotAllowedDismissedEvent() {
        if (shouldTrackDismissedEvent) {
            eventHorizon.track(NotAllowedToRateScreenDismissedEvent)
        }
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
