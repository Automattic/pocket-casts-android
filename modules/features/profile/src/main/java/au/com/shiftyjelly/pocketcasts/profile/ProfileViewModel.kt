package au.com.shiftyjelly.pocketcasts.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val settings: Settings,
    val podcastManager: PodcastManager,
    val statsManager: StatsManager,
    val userManager: UserManager,
    private val endOfYearManager: EndOfYearManager
) : ViewModel() {
    var isFragmentChangingConfigurations: Boolean = false
    val podcastCount: LiveData<Int> = LiveDataReactiveStreams.fromPublisher(podcastManager.observeCountSubscribed())
    val daysListenedCount: MutableLiveData<Long> = MutableLiveData()
    val daysSavedCount: MutableLiveData<Long> = MutableLiveData()

    val isSignedIn: Boolean
        get() = signInState.value?.isSignedIn ?: false

    val signInState: LiveData<SignInState> = LiveDataReactiveStreams.fromPublisher(userManager.getSignInState())

    val refreshObservable: LiveData<RefreshState> = LiveDataReactiveStreams.fromPublisher(
        settings.refreshStateObservable.toFlowable(BackpressureStrategy.LATEST)
    )

    suspend fun isEndOfYearStoriesEligible() = endOfYearManager.isEligibleForStories()

    fun clearFailedRefresh() {
        val lastSuccess = settings.getLastSuccessRefreshState()
        if (settings.getRefreshState() is RefreshState.Failed && lastSuccess != null) {
            settings.setRefreshState(lastSuccess)
        }
    }

    fun updateState() {
        val timeSaved = statsManager.mergedTotalTimeSaved
        daysSavedCount.value = timeSaved

        val daysListened = statsManager.mergedTotalListeningTimeSec
        daysListenedCount.value = daysListened
    }
}
