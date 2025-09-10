package au.com.shiftyjelly.pocketcasts.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.toDurationFromNow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settings: Settings,
    private val podcastManager: PodcastManager,
    private val statsManager: StatsManager,
    private val userManager: UserManager,
    private val endOfYearManager: EndOfYearManager,
    private val tracker: AnalyticsTracker,
) : ViewModel() {
    private val refreshStatsTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val signInState = userManager.getSignInState().asFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SignInState.SignedOut,
    )

    internal val isSignedIn get() = signInState.value.isSignedIn

    internal val profileHeaderState = signInState.map { state ->
        when (state) {
            is SignInState.SignedIn -> ProfileHeaderState(
                imageUrl = Gravatar.getUrl(state.email),
                subscriptionTier = state.subscription?.tier,
                email = state.email,
                expiresIn = state.subscription?.expiryDate?.toDurationFromNow(),
            )

            is SignInState.SignedOut -> ProfileHeaderState(
                imageUrl = null,
                subscriptionTier = null,
                email = null,
                expiresIn = null,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProfileHeaderState(
            imageUrl = null,
            subscriptionTier = null,
            email = null,
            expiresIn = null,
        ),
    )

    internal val profileStatsState = combine(
        refreshStatsTrigger.onStart { emit(Unit) },
        podcastManager.countSubscribedFlow(),
    ) { _, count ->
        ProfileStatsState(
            podcastsCount = count,
            listenedDuration = statsManager.mergedTotalListeningTimeSec.seconds,
            savedDuration = statsManager.mergedTotalTimeSaved.seconds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProfileStatsState(
            podcastsCount = 0,
            listenedDuration = Duration.ZERO,
            savedDuration = Duration.ZERO,
        ),
    )

    internal val isPlaybackAvailable = flow {
        while (true) {
            emit(endOfYearManager.isEligibleForEndOfYear())
            delay(10_000)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    internal val isFreeAccountBannerVisible = combine(
        signInState.map { it.isSignedIn },
        settings.isFreeAccountProfileBannerDismissed.flow,
    ) { isSignedIn, isBannerDismissed ->
        !isSignedIn && !isBannerDismissed && FeatureFlag.isEnabled(Feature.ENCOURAGE_ACCOUNT_CREATION)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    internal val refreshState = settings.refreshStateFlow

    internal val showUpgradeBanner = combine(
        settings.upgradeProfileClosed.flow,
        signInState.map { it.isSignedInAsPlusOrPatron },
    ) { closedClicked, isPlusOrPatron -> !closedClicked && !isPlusOrPatron }

    internal val miniPlayerInset = settings.bottomInset.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0,
    )

    internal fun onScreenShown() {
        tracker.track(AnalyticsEvent.PROFILE_SHOWN)
    }

    internal fun onSettingsClick() {
        tracker.track(AnalyticsEvent.PROFILE_SETTINGS_BUTTON_TAPPED)
    }

    internal fun onHeaderClick() {
        tracker.track(AnalyticsEvent.PROFILE_ACCOUNT_BUTTON_TAPPED)
    }

    internal fun refreshStats() {
        refreshStatsTrigger.tryEmit(Unit)
    }

    internal fun onPlaybackClick() {
        tracker.track(
            AnalyticsEvent.END_OF_YEAR_PROFILE_CARD_TAPPED,
            mapOf("year" to EndOfYearManager.YEAR_TO_SYNC.value),
        )
        // once stories prompt card is tapped, we don't want to show stories launch modal if not already shown
        if (settings.getEndOfYearShowModal()) {
            settings.setEndOfYearShowModal(false)
        }
    }

    internal fun onSectionClick(section: ProfileSection) {
        val event = when (section) {
            ProfileSection.Stats -> AnalyticsEvent.STATS_SHOWN
            ProfileSection.Downloads -> AnalyticsEvent.DOWNLOADS_SHOWN
            ProfileSection.CloudFiles -> AnalyticsEvent.UPLOADED_FILES_SHOWN
            ProfileSection.Starred -> AnalyticsEvent.STARRED_SHOWN
            ProfileSection.Bookmarks -> AnalyticsEvent.PROFILE_BOOKMARKS_SHOWN
            ProfileSection.ListeningHistory -> AnalyticsEvent.LISTENING_HISTORY_SHOWN
            ProfileSection.Help -> AnalyticsEvent.SETTINGS_HELP_SHOWN
        }
        tracker.track(event)
    }

    internal fun refreshProfile() {
        tracker.track(AnalyticsEvent.PROFILE_REFRESH_BUTTON_TAPPED)
        podcastManager.refreshPodcasts("profile")
    }

    internal fun clearFailedRefresh() {
        val lastSuccess = settings.getLastSuccessRefreshState()
        if (settings.getRefreshState() is RefreshState.Failed && lastSuccess != null) {
            settings.setRefreshState(lastSuccess)
        }
    }

    internal fun closeUpgradeProfile(source: SourceView) {
        tracker.track(AnalyticsEvent.UPGRADE_BANNER_DISMISSED, mapOf("source" to source.analyticsValue))
        settings.upgradeProfileClosed.set(true, updateModifiedAt = false)
    }

    internal fun onCreateFreeAccountClick() {
        tracker.track(AnalyticsEvent.INFORMATIONAL_BANNER_VIEW_CREATE_ACCOUNT_TAP, mapOf("source" to "profile"))
    }

    internal fun dismissFreeAccountBanner() {
        tracker.track(AnalyticsEvent.INFORMATIONAL_BANNER_VIEW_DISMISSED, mapOf("source" to "profile"))
        settings.isFreeAccountProfileBannerDismissed.set(true, updateModifiedAt = true)
    }
}
