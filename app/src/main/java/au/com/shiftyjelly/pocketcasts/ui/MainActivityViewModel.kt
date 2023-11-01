package au.com.shiftyjelly.pocketcasts.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkArguments
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewFragment
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.EarlyAccessState
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlagWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion.Companion.comparedToEarlyPatronAccess
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel
@Inject constructor(
    private val playbackManager: PlaybackManager,
    userManager: UserManager,
    private val settings: Settings,
    private val endOfYearManager: EndOfYearManager,
    private val multiSelectBookmarksHelper: MultiSelectBookmarksHelper,
    private val podcastManager: PodcastManager,
    private val bookmarkManager: BookmarkManager,
    private val theme: Theme,
    private val feature: FeatureWrapper,
    private val featureFlag: FeatureFlagWrapper,
    private val releaseVersion: ReleaseVersionWrapper,
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    var isPlayerOpen: Boolean = false
    var lastPlaybackState: PlaybackState? = null
    val shouldShowStoriesModal = MutableStateFlow(false)
    var waitingForSignInToShowStories = false

    init {
        showWhatsNewIfNeeded()
        updateStoriesModalShowState(settings.getEndOfYearShowModal())
    }

    private fun showWhatsNewIfNeeded() {
        viewModelScope.launch {
            val lastSeenVersionCode = settings.getWhatsNewVersionCode()
            val migratedVersion = settings.getMigratedVersionCode()
            if (migratedVersion != 0) { // We don't want to show this to new users, there is a race condition between this and the version migration
                var whatsNewShouldBeShown = WhatsNewFragment.isWhatsNewNewerThan(lastSeenVersionCode)
                val isBookmarksEnabled = featureFlag.isEnabled(feature.bookmarksFeature)
                if (isBookmarksEnabled) {
                    val isUserEntitled = feature.isUserEntitled(feature.bookmarksFeature, settings.userTier)

                    val patronExclusiveAccessRelease = (feature.bookmarksFeature.tier as? FeatureTier.Plus)?.patronExclusiveAccessRelease
                    val relativeToEarlyPatronAccess = patronExclusiveAccessRelease?.let {
                        releaseVersion.currentReleaseVersion.comparedToEarlyPatronAccess(it)
                    }
                    val shouldShowWhatsNewWhenUserNotEntitled = patronExclusiveAccessRelease == null ||
                        relativeToEarlyPatronAccess == EarlyAccessState.After

                    whatsNewShouldBeShown = whatsNewShouldBeShown &&
                        (isUserEntitled || shouldShowWhatsNewWhenUserNotEntitled)
                }
                _state.update { state -> state.copy(shouldShowWhatsNew = whatsNewShouldBeShown) }
            }
        }
    }

    fun onWhatsNewShown() {
        settings.setWhatsNewVersionCode(Settings.WHATS_NEW_VERSION_CODE)
        _state.update { state -> state.copy(shouldShowWhatsNew = false) }
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

    fun closeMultiSelect() {
        multiSelectBookmarksHelper.closeMultiSelect()
    }

    fun buildBookmarkArguments(onSuccess: (BookmarkArguments) -> Unit) {
        viewModelScope.launch {
            val episode = playbackManager.getCurrentEpisode() ?: return@launch
            val timeInSecs = playbackManager.getCurrentTimeMs(episode) / 1000

            // load the existing bookmark
            val bookmark = bookmarkManager.findByEpisodeTime(
                episode = episode,
                timeSecs = timeInSecs
            )

            val podcast =
                bookmark?.let { podcastManager.findPodcastByUuidSuspend(bookmark.podcastUuid) }
            val backgroundColor =
                if (podcast == null) 0xFF000000.toInt() else theme.playerBackgroundColor(podcast)
            val tintColor =
                if (podcast == null) 0xFFFFFFFF.toInt() else theme.playerHighlightColor(podcast)

            val arguments = BookmarkArguments(
                bookmarkUuid = bookmark?.uuid,
                episodeUuid = episode.uuid,
                timeSecs = timeInSecs,
                backgroundColor = backgroundColor,
                tintColor = tintColor,
            )
            onSuccess(arguments)
        }
    }

    data class State(
        val shouldShowWhatsNew: Boolean = false,
    )
}
