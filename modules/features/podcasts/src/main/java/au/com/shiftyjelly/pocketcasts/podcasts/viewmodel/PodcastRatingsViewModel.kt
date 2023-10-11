package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings.GiveRatingFragment
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.abbreviated
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private const val MAX_STARS = 5

@HiltViewModel
class PodcastRatingsViewModel
@Inject constructor(
    private val ratingsManager: RatingsManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private var _stateFlow: MutableStateFlow<RatingState> = MutableStateFlow(RatingState.Loading)
    val stateFlow: StateFlow<RatingState> = _stateFlow

    fun loadRatings(podcastUuid: String) {
        viewModelScope.launch {
            try {
                ratingsManager.podcastRatings(podcastUuid)
                    .stateIn(viewModelScope)
                    .collect { ratings ->
                        _stateFlow.value = RatingState.Loaded(ratings)
                    }
            } catch (e: IOException) {
                Timber.e(e, "Failed to load podcast ratings")
                _stateFlow.update { RatingState.Error }
            }
        }
    }

    fun refreshPodcastRatings(uuid: String) {
        launch(Dispatchers.IO) {
            try {
                ratingsManager.refreshPodcastRatings(uuid)
            } catch (e: Exception) {
                val message = "Failed to refresh podcast ratings"
                // don't report missing rating or network errors to Sentry
                if (e is HttpException || e is IOException) {
                    Timber.i(e, message)
                } else {
                    Timber.e(e, message)
                }
            }
        }
    }

    fun onRatingStarsTapped(
        podcastUuid: String,
        fragmentHostListener: FragmentHostListener,
    ) {
        analyticsTracker.track(
            AnalyticsEvent.RATING_STARS_TAPPED,
            AnalyticsProp.ratingStarsTapped(podcastUuid)
        )
        if (FeatureFlag.isEnabled(Feature.GIVE_RATINGS)) {
            fragmentHostListener.addFragment(GiveRatingFragment())
        }
    }

    sealed class RatingState {
        object Loading : RatingState()

        data class Loaded(
            private val ratings: PodcastRatings,
        ) : RatingState() {

            val podcastUuid = ratings.podcastUuid
            private val total = ratings.total
            private val average = ratings.average

            val stars: List<Star> = starsList()
            val ratingText: RatingText = ratingText()

            private fun starsList(): List<Star> {
                val rating = average ?: 0.0
                // truncate the floating points off without rounding
                val ratingInt = rating.toInt()
                // Get the float value
                val half = rating % 1

                val stars = (0 until MAX_STARS).map { index ->
                    starFor(index, ratingInt, half)
                }
                return stars
            }

            private fun starFor(index: Int, rating: Int, half: Double) = when {
                index < rating -> Star.FilledStar
                (index == rating) && (half >= 0.5) -> Star.HalfStar
                else -> Star.BorderedStar
            }

            private fun ratingText() = when (total) {
                null ->
                    if (average == null) {
                        RatingText.NotEnoughToRate
                    } else {
                        LogBuffer.e(
                            LogBuffer.TAG_INVALID_STATE,
                            "Rating total is null but the average is not. This should never happen."
                        )
                        RatingText.ShowNothing
                    }

                0 ->
                    if (average == null || average == 0.0) {
                        RatingText.NotEnoughToRate
                    } else {
                        LogBuffer.e(
                            LogBuffer.TAG_INVALID_STATE,
                            "Rating total is 0 but the average is not 0. This should never happen."
                        )
                        RatingText.ShowNothing
                    }

                else ->
                    if (average != null) {
                        RatingText.ShowTotal(total.abbreviated)
                    } else {
                        LogBuffer.e(
                            LogBuffer.TAG_INVALID_STATE,
                            "Has ratings but the average is null. This should never happen."
                        )
                        RatingText.ShowNothing
                    }
            }

            sealed class RatingText {
                object NotEnoughToRate : RatingText()
                class ShowTotal(val text: String) : RatingText()
                object ShowNothing : RatingText()
            }
        }

        object Error : RatingState()
    }

    enum class Star(val icon: ImageVector) {
        FilledStar(Icons.Filled.Star),
        HalfStar(Icons.Default.StarHalf),
        BorderedStar(Icons.Filled.StarBorder),
    }

    companion object {
        private object AnalyticsProp {
            private const val UUID_KEY = "uuid"
            fun ratingStarsTapped(podcastUuid: String) =
                mapOf(UUID_KEY to podcastUuid)
        }
    }
}
