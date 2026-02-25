package au.com.shiftyjelly.pocketcasts.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.utils.toDurationFromNow
import com.automattic.eventhorizon.DownloadsShownEvent
import com.automattic.eventhorizon.EndOfYearProfileCardShownEvent
import com.automattic.eventhorizon.EndOfYearProfileCardTappedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.InformationalBannerViewCreateAccountTapEvent
import com.automattic.eventhorizon.InformationalBannerViewDismissedEvent
import com.automattic.eventhorizon.ListeningHistoryShownEvent
import com.automattic.eventhorizon.ProfileAccountButtonTappedEvent
import com.automattic.eventhorizon.ProfileBookmarksShownEvent
import com.automattic.eventhorizon.ProfileRefreshButtonTappedEvent
import com.automattic.eventhorizon.ProfileSettingsButtonTappedEvent
import com.automattic.eventhorizon.ProfileShownEvent
import com.automattic.eventhorizon.SettingsHelpShownEvent
import com.automattic.eventhorizon.StarredShownEvent
import com.automattic.eventhorizon.StatsShownEvent
import com.automattic.eventhorizon.UpgradeBannerDismissedEvent
import com.automattic.eventhorizon.UploadedFilesShownEvent
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
    private val eventHorizon: EventHorizon,
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
        !isSignedIn && !isBannerDismissed
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
        eventHorizon.track(ProfileShownEvent)
    }

    internal fun onSettingsClick() {
        eventHorizon.track(ProfileSettingsButtonTappedEvent)
    }

    internal fun onHeaderClick() {
        eventHorizon.track(ProfileAccountButtonTappedEvent)
    }

    internal fun refreshStats() {
        refreshStatsTrigger.tryEmit(Unit)
    }

    internal fun onEndOfYearCardShown() {
        eventHorizon.track(
            EndOfYearProfileCardShownEvent(
                currentYear = EndOfYearManager.YEAR_TO_SYNC.value.toLong(),
            ),
        )
    }

    internal fun onPlaybackClick() {
        eventHorizon.track(
            EndOfYearProfileCardTappedEvent(
                currentYear = EndOfYearManager.YEAR_TO_SYNC.value.toLong(),
            ),
        )
        // once stories prompt card is tapped, we don't want to show stories launch modal if not already shown
        if (settings.getEndOfYearShowModal()) {
            settings.setEndOfYearShowModal(false)
        }
    }

    internal fun onSectionClick(section: ProfileSection) {
        val event = when (section) {
            ProfileSection.Stats -> StatsShownEvent
            ProfileSection.Downloads -> DownloadsShownEvent
            ProfileSection.CloudFiles -> UploadedFilesShownEvent
            ProfileSection.Starred -> StarredShownEvent
            ProfileSection.Bookmarks -> ProfileBookmarksShownEvent
            ProfileSection.ListeningHistory -> ListeningHistoryShownEvent
            ProfileSection.Help -> SettingsHelpShownEvent
        }
        eventHorizon.track(event)
    }

    internal fun refreshProfile() {
        eventHorizon.track(ProfileRefreshButtonTappedEvent)
        podcastManager.refreshPodcasts("profile")
    }

    internal fun clearFailedRefresh() {
        val lastSuccess = settings.getLastSuccessRefreshState()
        if (settings.getRefreshState() is RefreshState.Failed && lastSuccess != null) {
            settings.setRefreshState(lastSuccess)
        }
    }

    internal fun closeUpgradeProfile(source: SourceView) {
        eventHorizon.track(
            UpgradeBannerDismissedEvent(
                source = source.eventHorizonValue,
            ),
        )
        settings.upgradeProfileClosed.set(true, updateModifiedAt = false)
    }

    internal fun onCreateFreeAccountClick() {
        eventHorizon.track(
            InformationalBannerViewCreateAccountTapEvent(
                source = SourceView.PROFILE.eventHorizonValue,
            ),
        )
    }

    internal fun dismissFreeAccountBanner() {
        eventHorizon.track(
            InformationalBannerViewDismissedEvent(
                source = SourceView.PROFILE.eventHorizonValue,
            ),
        )
        settings.isFreeAccountProfileBannerDismissed.set(true, updateModifiedAt = true)
    }
}
