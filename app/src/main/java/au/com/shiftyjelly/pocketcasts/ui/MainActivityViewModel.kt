package au.com.shiftyjelly.pocketcasts.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkArguments
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewFragment
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class MainActivityViewModel
@Inject constructor(
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    userManager: UserManager,
    private val settings: Settings,
    private val endOfYearManager: EndOfYearManager,
    private val multiSelectBookmarksHelper: MultiSelectBookmarksHelper,
    private val podcastManager: PodcastManager,
    private val bookmarkManager: BookmarkManager,
    private val theme: Theme,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<Int>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    var isPlayerOpen: Boolean = false
    var lastPlaybackState: PlaybackState? = null
    val shouldShowStoriesModal = MutableStateFlow(false)
    var waitingForSignInToShowStories = false

    init {
        viewModelScope.launch {
            showWhatsNewIfNeeded()
            if (!state.value.shouldShowWhatsNew) {
                updateStoriesModalShowState(settings.getEndOfYearShowModal())
            }
        }
    }

    private fun showWhatsNewIfNeeded() {
        val lastSeenVersionCode = settings.getWhatsNewVersionCode()
        val migratedVersion = settings.getMigratedVersionCode()
        if (migratedVersion != 0) { // We don't want to show this to new users, there is a race condition between this and the version migration
            val whatsNewShouldBeShown = WhatsNewFragment.isWhatsNewNewerThan(lastSeenVersionCode) &&
                FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS)
            _state.update { state -> state.copy(shouldShowWhatsNew = whatsNewShouldBeShown) }
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
    val playbackState = playbackStateRx.asFlow()

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
            shouldShowStoriesModal.value = show &&
                isEndOfYearStoriesEligible() &&
                !state.value.shouldShowWhatsNew
        }
    }

    fun closeMultiSelect() {
        multiSelectBookmarksHelper.closeMultiSelect()
    }

    fun buildBookmarkArguments(bookmarkUuid: String? = null, onSuccess: (BookmarkArguments) -> Unit) {
        viewModelScope.launch {
            // load the existing bookmark
            val bookmark = bookmarkUuid?.let { bookmarkManager.findBookmark(it) }
            if (bookmarkUuid != null && bookmark == null) {
                _snackbarMessage.emit(LR.string.bookmark_not_found)
                return@launch
            }
            val currentEpisode = playbackManager.getCurrentEpisode()
            val episodeUuid = bookmark?.episodeUuid ?: currentEpisode?.uuid ?: return@launch
            val timeInSecs = bookmark?.timeSecs ?: currentEpisode?.let { playbackManager.getCurrentTimeMs(currentEpisode) / 1000 } ?: 0

            val podcast =
                bookmark?.let { podcastManager.findPodcastByUuidSuspend(bookmark.podcastUuid) }
            val backgroundColor =
                if (podcast == null) 0xFF000000.toInt() else theme.playerBackgroundColor(podcast)
            val tintColor =
                if (podcast == null) 0xFFFFFFFF.toInt() else theme.playerHighlightColor(podcast)

            val arguments = BookmarkArguments(
                bookmarkUuid = bookmark?.uuid,
                episodeUuid = episodeUuid,
                timeSecs = timeInSecs,
                backgroundColor = backgroundColor,
                tintColor = tintColor,
            )
            onSuccess(arguments)
        }
    }

    fun viewBookmark(bookmarkUuid: String) {
        viewModelScope.launch {
            val bookmark = bookmarkManager.findBookmark(bookmarkUuid)
            if (bookmark == null) {
                _snackbarMessage.emit(LR.string.bookmark_not_found)
            } else {
                val currentEpisode = playbackManager.getCurrentEpisode()
                val isBookmarkForCurrentlyPlayingEpisode = bookmark.episodeUuid == currentEpisode?.uuid
                if (isBookmarkForCurrentlyPlayingEpisode) {
                    _navigationState.emit(NavigationState.BookmarksForCurrentlyPlaying)
                } else {
                    episodeManager.findEpisodeByUuid(bookmark.episodeUuid)?.let {
                        when (it) {
                            is PodcastEpisode -> _navigationState.emit(NavigationState.BookmarksForPodcastEpisode(it))
                            is UserEpisode -> _navigationState.emit(NavigationState.BookmarksForUserEpisode(it))
                        }
                    }
                }
            }
        }
    }

    fun deleteBookmark(bookmarkUuid: String) {
        viewModelScope.launch {
            val bookmark = bookmarkManager.findBookmark(bookmarkUuid)
            if (bookmark == null) {
                _snackbarMessage.emit(LR.string.bookmark_not_found)
            } else {
                _snackbarMessage.emit(LR.string.bookmarks_deleted_singular)
                bookmarkManager.deleteToSync(bookmarkUuid)
                analyticsTracker.track(
                    AnalyticsEvent.BOOKMARK_DELETED,
                    mapOf("source" to SourceView.NOTIFICATION_BOOKMARK.analyticsValue),
                )
            }
        }
    }

    data class State(
        val shouldShowWhatsNew: Boolean = false,
    )

    sealed class NavigationState {
        object BookmarksForCurrentlyPlaying : NavigationState()
        data class BookmarksForPodcastEpisode(val episode: PodcastEpisode) : NavigationState()
        data class BookmarksForUserEpisode(val episode: UserEpisode) : NavigationState()
    }
}
