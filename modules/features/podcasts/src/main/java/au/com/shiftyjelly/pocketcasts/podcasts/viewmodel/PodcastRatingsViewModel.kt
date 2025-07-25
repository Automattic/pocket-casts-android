package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.images.PocketCastsIcons
import au.com.shiftyjelly.pocketcasts.images.icons.StarEmpty
import au.com.shiftyjelly.pocketcasts.images.icons.StarFull
import au.com.shiftyjelly.pocketcasts.images.icons.StarHalf
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings.GiveRatingFragment
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val MAX_STARS = 5

@HiltViewModel
class PodcastRatingsViewModel
@Inject constructor(
    private val ratingsManager: RatingsManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel(),
    CoroutineScope {
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
            ratingsManager.refreshPodcastRatings(podcastUuid = uuid, useCache = true)
        }
    }

    fun onRatingStarsTapped(
        podcastUuid: String,
        fragmentManager: FragmentManager,
        source: RatingTappedSource,
    ) {
        analyticsTracker.track(
            AnalyticsEvent.RATING_STARS_TAPPED,
            mapOf(
                "uuid" to podcastUuid,
                "source" to source.analyticsValue,
            ),
        )
        val fragment = GiveRatingFragment.newInstance(podcastUuid)
        fragment.show(fragmentManager, "give_rating")
    }

    sealed class RatingState {
        data object Loading : RatingState()

        data class Loaded(
            private val ratings: PodcastRatings,
        ) : RatingState() {

            val podcastUuid = ratings.podcastUuid
            val total = ratings.total
            private val average = ratings.average

            val noRatings: Boolean
                get() = total == null || total == 0

            val stars: List<Star> = starsList()

            val roundedAverage: String
                get() {
                    val rating = average ?: 0.0
                    return (Math.round(rating * 10) / 10.0).toString()
                }

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
        }

        data object Error : RatingState()
    }

    enum class Star(val icon: ImageVector) {
        FilledStar(PocketCastsIcons.StarFull),
        HalfStar(PocketCastsIcons.StarHalf),
        BorderedStar(PocketCastsIcons.StarEmpty),
    }

    enum class RatingTappedSource(val analyticsValue: String) {
        BUTTON("button"),
        STARS("stars"),
    }
}
