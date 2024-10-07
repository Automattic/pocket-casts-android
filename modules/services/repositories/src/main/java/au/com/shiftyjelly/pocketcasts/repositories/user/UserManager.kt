package au.com.shiftyjelly.pocketcasts.repositories.user

import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentProvider
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.AccountManagerStatusInfo
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

interface UserManager {
    fun beginMonitoringAccountManager(playbackManager: PlaybackManager)
    fun getSignInState(): Flowable<SignInState>
    fun signOut(playbackManager: PlaybackManager, wasInitiatedByUser: Boolean)
    fun signOutAndClearData(playbackManager: PlaybackManager, upNextQueue: UpNextQueue, playlistManager: PlaylistManager, folderManager: FolderManager, searchHistoryManager: SearchHistoryManager, episodeManager: EpisodeManager, wasInitiatedByUser: Boolean)
}

class UserManagerImpl @Inject constructor(
    @ApplicationContext val application: Context,
    val settings: Settings,
    val syncManager: SyncManager,
    val subscriptionManager: SubscriptionManager,
    val podcastManager: PodcastManager,
    val userEpisodeManager: UserEpisodeManager,
    private val analyticsTracker: AnalyticsTracker,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val crashLogging: CrashLogging,
    private val experimentProvider: ExperimentProvider,
    private val accountManager: AccountManagerStatusInfo,
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
                crashLogging.sendReport(t, message = "Account monitoring crash.")
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
                    Flowable.just(SignInState.SignedOut)
                }
            }
    }

    override fun signOut(playbackManager: PlaybackManager, wasInitiatedByUser: Boolean) {
        if (wasInitiatedByUser || !settings.getFullySignedOut()) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signing out")
            subscriptionManager.clearCachedStatus()
            syncManager.signOut {
                applicationScope.launch {
                    settings.clearPlusPreferences()

                    userEpisodeManager.removeCloudStatusFromFiles(playbackManager)

                    settings.marketingOptIn.set(false, updateModifiedAt = false)
                    settings.setEndOfYearShowModal(true)

                    analyticsTracker.track(
                        AnalyticsEvent.USER_SIGNED_OUT,
                        mapOf(KEY_USER_INITIATED to wasInitiatedByUser),
                    )
                    analyticsTracker.flush()
                    analyticsTracker.clearAllData()
                    analyticsTracker.refreshMetadata()
                    experimentProvider.refreshExperiments()
                }
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
        signOut(playbackManager = playbackManager, wasInitiatedByUser = wasInitiatedByUser)

        // Need to stop playback before we start clearing data
        playbackManager.removeEpisode(
            episodeToRemove = playbackManager.getCurrentEpisode(),
            // Unknown is fine here because we don't send analytics when the user did not initiate the action
            source = SourceView.UNKNOWN,
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
                userEpisodeManager.delete(episode = it, playbackManager = playbackManager)
            }
            episodeManager.deleteAll()
        }
    }
}
