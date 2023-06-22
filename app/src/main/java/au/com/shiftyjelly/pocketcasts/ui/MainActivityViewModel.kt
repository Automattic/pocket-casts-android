package au.com.shiftyjelly.pocketcasts.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel
@Inject constructor(
    playbackManager: PlaybackManager,
    userManager: UserManager,
    val settings: Settings,
    private val endOfYearManager: EndOfYearManager
) : ViewModel() {

    var isPlayerOpen: Boolean = false
    var lastPlaybackState: PlaybackState? = null
    val shouldShowStoriesModal = MutableStateFlow(false)
    var waitingForSignInToShowStories = false

    init {
        updateStoriesModalShowState(!settings.getEndOfYearModalHasBeenShown())
    }

    private val playbackStateRx = playbackManager.playbackStateRelay
        .doOnNext {
            Timber.d("Updated playback state from ${it.lastChangeFrom} is playing ${it.isPlaying}")
        }
        .toFlowable(BackpressureStrategy.LATEST)
    val playbackState = playbackStateRx.toLiveData()

    val signInState: LiveData<SignInState> = userManager.getSignInState().toLiveData()
    val isSignedIn: Boolean
        get() = signInState.value?.isSignedIn ?: false

    fun shouldShowCancelled(subscriptionStatus: SubscriptionStatus): Boolean {
        val paidStatus = (subscriptionStatus as? SubscriptionStatus.Paid) ?: return false
        val renewing = subscriptionStatus.autoRenew
        val cancelAcknowledged = settings.getCancelledAcknowledged()
        val giftDays = paidStatus.giftDays
        val expired = paidStatus.expiry.before(Date())

        return !renewing && !cancelAcknowledged && giftDays == 0 && expired
    }

    fun shouldShowTrialFinished(signInState: SignInState): Boolean {
        return signInState.isExpiredTrial && !settings.getTrialFinishedSeen()
    }

    suspend fun isEndOfYearStoriesEligible() = endOfYearManager.isEligibleForStories()
    fun updateStoriesModalShowState(show: Boolean) {
        viewModelScope.launch {
            shouldShowStoriesModal.value = show && isEndOfYearStoriesEligible()
        }
    }
}
