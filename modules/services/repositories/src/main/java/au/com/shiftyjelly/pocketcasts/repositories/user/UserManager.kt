package au.com.shiftyjelly.pocketcasts.repositories.user

import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsPropValue
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.pocketCastsAccount
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

interface UserManager {
    fun beginMonitoringAccountManager(playbackManager: PlaybackManager)
    fun getSignInState(): Flowable<SignInState>
    fun signOut(playbackManager: PlaybackManager, wasInitiatedByUser: Boolean)
}

class UserManagerImpl @Inject constructor(
    @ApplicationContext val application: Context,
    val settings: Settings,
    val syncServerManager: SyncServerManager,
    val subscriptionManager: SubscriptionManager,
    val podcastManager: PodcastManager,
    val userEpisodeManager: UserEpisodeManager,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : UserManager {

    companion object {
        private const val KEY_USER_INITIATED = "user_initiated"
    }

    override fun beginMonitoringAccountManager(playbackManager: PlaybackManager) {
        val accountListener = OnAccountsUpdateListener {
            try {
                // Handle sign out from outside of the app
                if (settings.getUsedAccountManager()) {
                    val accountManager = AccountManager.get(application)
                    if (accountManager.pocketCastsAccount() == null && settings.isLoggedIn()) {
                        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signing out because no account manager account found")
                        signOut(playbackManager, wasInitiatedByUser = false)
                    }
                }

                (settings.isLoggedInObservable as? BehaviorRelay<Boolean>)?.accept(settings.isLoggedIn())
            } catch (t: Throwable) {
                SentryHelper.recordException("Account monitoring crash.", t)
            }
        }

        val accountManager = AccountManager.get(application)
        accountManager.addOnAccountsUpdatedListener(accountListener, null, true)
    }

    override fun getSignInState(): Flowable<SignInState> {
        return settings.isLoggedInObservable.toFlowable(BackpressureStrategy.LATEST)
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
                            SignInState.SignedIn(email = settings.getSyncEmail() ?: "", subscriptionStatus = it)
                        }
                        .onErrorReturn {
                            Timber.e(it, "Error getting subscription state")
                            SignInState.SignedIn(settings.getSyncEmail() ?: "", SubscriptionStatus.Free())
                        }
                } else {
                    Flowable.just(SignInState.SignedOut())
                }
            }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun signOut(playbackManager: PlaybackManager, wasInitiatedByUser: Boolean) {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signing out")
        subscriptionManager.clearCachedStatus()
        syncServerManager.signOut()
        settings.clearPlusPreferences()
        GlobalScope.launch {
            userEpisodeManager.removeCloudStatusFromFiles(playbackManager)
        }

        settings.setMarketingOptIn(false)
        settings.setMarketingOptInNeedsSync(false)
        analyticsTracker.track(
            AnalyticsEvent.USER_SIGNED_OUT,
            mapOf(KEY_USER_INITIATED to AnalyticsPropValue(wasInitiatedByUser))
        )
        analyticsTracker.flush()
        analyticsTracker.clearAllData()
        analyticsTracker.refreshMetadata()

        val accountManager = AccountManager.get(application)
        val account = accountManager.pocketCastsAccount() ?: return
        accountManager.removeAccountExplicitly(account)
    }
}
