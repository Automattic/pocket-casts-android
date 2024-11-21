package au.com.shiftyjelly.pocketcasts.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlow
import java.time.Duration as JavaDuration

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val settings: Settings,
    val podcastManager: PodcastManager,
    val statsManager: StatsManager,
    val userManager: UserManager,
    private val endOfYearManager: EndOfYearManager,
) : ViewModel() {
    var isFragmentChangingConfigurations: Boolean = false
    private val refreshStatsTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val isSignedIn: Boolean
        get() = signInState.value?.isSignedIn ?: false

    val signInState: LiveData<SignInState> = userManager.getSignInState().toLiveData()

    internal val profileHeaderState = userManager.getSignInState().asFlow().map { state ->
        when (state) {
            is SignInState.SignedIn -> ProfileHeaderState(
                imageUrl = Gravatar.getUrl(state.email),
                subscriptionTier = when {
                    state.isSignedInAsPatron -> SubscriptionTier.PATRON
                    state.isSignedInAsPlus -> SubscriptionTier.PLUS
                    else -> SubscriptionTier.NONE
                },
                email = state.email,
                expiresIn = state.subscriptionStatus.expiryDate?.let { expiryDate -> expiresIn(expiryDate.toInstant()) },
            )

            is SignInState.SignedOut -> ProfileHeaderState(
                imageUrl = null,
                subscriptionTier = SubscriptionTier.NONE,
                email = null,
                expiresIn = null,
            )
        }
    }

    internal val profileStatsState = combine(
        refreshStatsTrigger.onStart { emit(Unit) },
        podcastManager.observeCountSubscribed().asFlow(),
    ) { _, count ->
        ProfileStatsState(
            podcastsCount = count,
            listenedDuration = statsManager.mergedTotalListeningTimeSec.seconds,
            savedDuration = statsManager.mergedTotalTimeSaved.seconds,
        )
    }

    val refreshState = settings.refreshStateObservable.asFlow()

    val showUpgradeBanner = combine(
        settings.upgradeProfileClosed.flow,
        signInState.asFlow().map { it.isSignedInAsPlusOrPatron },
    ) { closedClicked, isPlusOrPatron -> !closedClicked && !isPlusOrPatron }

    fun closeUpgradeProfile() {
        settings.upgradeProfileClosed.set(true, updateModifiedAt = false)
    }

    suspend fun isEndOfYearStoriesEligible() = endOfYearManager.isEligibleForEndOfYear()

    fun clearFailedRefresh() {
        val lastSuccess = settings.getLastSuccessRefreshState()
        if (settings.getRefreshState() is RefreshState.Failed && lastSuccess != null) {
            settings.setRefreshState(lastSuccess)
        }
    }

    fun refreshStats() {
        refreshStatsTrigger.tryEmit(Unit)
    }

    private fun expiresIn(expiryDate: Instant): Duration {
        return JavaDuration.between(Instant.now(), expiryDate)
            .toKotlinDuration()
            .coerceAtLeast(Duration.ZERO)
    }
}
