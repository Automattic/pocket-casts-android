package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.podcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class PodcastRatingsViewModel
@Inject constructor(
    private val ratingsManager: RatingsManager,
) : ViewModel(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private var _stateFlow: MutableStateFlow<RatingState> = MutableStateFlow(RatingState.Loading)
    val stateFlow: StateFlow<RatingState> = _stateFlow

    fun loadRatings(podcastUuid: String) {
        if (BuildConfig.SHOW_RATINGS) {
            viewModelScope.launch {
                try {
                    ratingsManager.podcastRatings(podcastUuid)
                        .stateIn(viewModelScope)
                        .collect { ratings -> _stateFlow.update { RatingState.Loaded(ratings = ratings) } }
                } catch (e: IOException) {
                    Timber.e(e, "Failed to load podcast ratings")
                    _stateFlow.update { RatingState.Error }
                }
            }
        }
    }

    fun refreshPodcastRatings(uuid: String) {
        launch(Dispatchers.IO) {
            try {
                ratingsManager.refreshPodcastRatings(uuid)
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh podcast ratings")
                _stateFlow.update { RatingState.Error }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    sealed class RatingState {
        object Loading : RatingState()
        data class Loaded(
            val ratings: PodcastRatings,
        ) : RatingState()

        object Error : RatingState()
    }
}
