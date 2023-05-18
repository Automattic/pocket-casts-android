package au.com.shiftyjelly.pocketcasts.repositories.user

import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

interface UserManager {
    fun beginMonitoringAccountManager(playbackManager: PlaybackManager)
    fun getSignInState(): Flowable<SignInState>
    fun signOut(playbackManager: PlaybackManager, wasInitiatedByUser: Boolean)
    fun signOutAndClearData(playbackManager: PlaybackManager, upNextQueue: UpNextQueue, playlistManager: PlaylistManager, folderManager: FolderManager, searchHistoryManager: SearchHistoryManager, episodeManager: EpisodeManager, wasInitiatedByUser: Boolean,)
}

class UserManagerImpl @Inject constructor(
    @ApplicationContext val application: Context,
    val settings: Settings,
    val syncManager: SyncManager,
    val subscriptionManager: SubscriptionManager,
    val podcastManager: PodcastManager,
    val userEpisodeManager: UserEpisodeManager,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : UserManager, CoroutineScope {

    companion object {
        private const val KEY_USER_INITIATED = "user_initiated"
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun beginMonitoringAccountManager(playbackManager: PlaybackManager) {
        val accountListener = OnAccountsUpdateListener {
            try {
                // Handle sign out from outside of the app
                if (!syncManager.isLoggedIn()) {
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signing out because no account manager account found")
                    signOut(playbackManager, wasInitiatedByUser = false)
                }
            } catch (t: Throwable) {
                SentryHelper.recordException("Account monitoring crash.", t)
            }
        }

        val accountManager = AccountManager.get(application)
        accountManager.addOnAccountsUpdatedListener(accountListener, null, true)
    }

    override fun getSignInState(): Flowable<SignInState> {
        return syncManager.isLoggedInObservable.toFlowable(BackpressureStrategy.LATEST)
            .switchMap { isLoggedIn ->
                if (isLoggedIn) {
                    subscriptionManager.observeSubscriptionStatus()
                        .flatMapSingle {
                            val value = it.get()
                            if (value != null) {
                                Single.just(value)
                            } else {
                                subscriptionManager.getSubscriptionStatus(allowCache = false)
                            }
                        }
                        .map {
                            analyticsTracker.refreshMetadata()
                            SignInState.SignedIn(email = syncManager.getEmail() ?: "", subscriptionStatus = it)
                        }
                        .onErrorReturn {
                            Timber.e(it, "Error getting subscription state")
                            SignInState.SignedIn(syncManager.getEmail() ?: "", SubscriptionStatus.Free())
                        }
                } else {
                    Flowable.just(SignInState.SignedOut())
                }
            }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun signOut(playbackManager: PlaybackManager, wasInitiatedByUser: Boolean) {
        if (wasInitiatedByUser || !settings.getFullySignedOut()) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signing out")
            subscriptionManager.clearCachedStatus()
            syncManager.signOut {
                settings.clearPlusPreferences()
                GlobalScope.launch {
                    userEpisodeManager.removeCloudStatusFromFiles(playbackManager)
                }

                settings.setMarketingOptIn(false)
                settings.setMarketingOptInNeedsSync(false)
                settings.setEndOfYearModalHasBeenShown(false)

                analyticsTracker.track(
                    AnalyticsEvent.USER_SIGNED_OUT,
                    mapOf(KEY_USER_INITIATED to wasInitiatedByUser)
                )
                analyticsTracker.flush()
                analyticsTracker.clearAllData()
                analyticsTracker.refreshMetadata()
            }
        }
        settings.setFullySignedOut(true)
    }

    override fun signOutAndClearData(
        playbackManager: PlaybackManager,
        upNextQueue: UpNextQueue,
        playlistManager: PlaylistManager,
        folderManager: FolderManager,
        searchHistoryManager: SearchHistoryManager,
        episodeManager: EpisodeManager,
        wasInitiatedByUser: Boolean,
    ) {
        // Sign out first to make sure no data changes get synced
        signOut(playbackManager, wasInitiatedByUser = true)

        // Need to stop playback before we start clearing data
        playbackManager.removeEpisode(
            episodeToRemove = playbackManager.getCurrentEpisode(),
            // Unknown is fine here because we don't send analytics when the user did not initiate the action
            source = AnalyticsSource.UNKNOWN,
            userInitiated = false,
        )

        // Block while clearing data so that users cannot interact with the app until we're done clearing data
        runBlocking(Dispatchers.IO) {

            upNextQueue.removeAllIncludingChanges()

            playlistManager.resetDb()
            folderManager.deleteAll()
            searchHistoryManager.clearAll()

            podcastManager.deleteAllPodcasts()

            userEpisodeManager.findUserEpisodes().forEach {
                userEpisodeManager.delete(it, playbackManager)
            }
            episodeManager.deleteAll()
        }
    }
}
