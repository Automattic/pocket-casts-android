package au.com.shiftyjelly.pocketcasts.repositories.user

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.ReadSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.jakewharton.rxrelay2.BehaviorRelay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class UserManagerImplTest {

    private val cachedSubscriptionFlow = MutableStateFlow<Subscription?>(null)
    private val settings = mock<Settings> {
        val cachedSubscription = mock<ReadSetting<Subscription?>> {
            on { flow } doReturn cachedSubscriptionFlow
        }
        on { this.cachedSubscription } doReturn cachedSubscription
    }
    private val isLoggedIn = BehaviorRelay.createDefault(false)
    private val syncManager = mock<SyncManager> {
        on { isLoggedInObservable } doReturn isLoggedIn
        on { emailFlow() } doReturn flowOf("user@pocketcasts.com")
    }
    private val subscriptionManager = mock<SubscriptionManager>()
    private val playbackManager = mock<PlaybackManager>()

    @Test
    fun `server-forced sign out emits onServerSignOut`() = runTest {
        whenever(settings.getFullySignedOut()).thenReturn(false)
        val userManager = createUserManager()

        userManager.onServerSignOut.test {
            userManager.signOut(playbackManager, wasInitiatedByUser = false)
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun `user-initiated sign out does not emit onServerSignOut`() = runTest {
        whenever(settings.getFullySignedOut()).thenReturn(false)
        val userManager = createUserManager()

        userManager.onServerSignOut.test {
            userManager.signOut(playbackManager, wasInitiatedByUser = true)
            expectNoEvents()
        }
    }

    @Test
    fun `forced sign out does not emit when already fully signed out`() = runTest {
        whenever(settings.getFullySignedOut()).thenReturn(true)
        val userManager = createUserManager()

        userManager.onServerSignOut.test {
            userManager.signOut(playbackManager, wasInitiatedByUser = false)
            expectNoEvents()
        }
    }

    @Test
    fun `signInStateFlow emits signed out while logged out`() = runTest {
        val userManager = createUserManager()

        userManager.signInStateFlow().test {
            assertEquals(SignInState.SignedOut, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `signInStateFlow uses the cached subscription without fetching`() = runTest {
        val subscription = Subscription.PlusPreview
        cachedSubscriptionFlow.value = subscription
        isLoggedIn.accept(true)
        val userManager = createUserManager()

        userManager.signInStateFlow().test {
            assertEquals(SignInState.SignedIn("user@pocketcasts.com", subscription), awaitItem())
            expectNoEvents()
            verify(subscriptionManager, never()).fetchFreshSubscriptionResult()
        }
    }

    @Test
    fun `signInStateFlow fetches the subscription when none is cached`() = runTest {
        val subscription = Subscription.PlusPreview
        whenever(subscriptionManager.fetchFreshSubscriptionResult()).thenReturn(Result.success(subscription))
        isLoggedIn.accept(true)
        val userManager = createUserManager()

        userManager.signInStateFlow().test {
            assertEquals(SignInState.SignedIn("user@pocketcasts.com", subscription), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `signInStateFlow falls back to a signed in state without a subscription when the fetch fails`() = runTest {
        whenever(subscriptionManager.fetchFreshSubscriptionResult()).thenThrow(RuntimeException("Fetch failed"))
        whenever(syncManager.getEmail()).thenReturn("fallback@pocketcasts.com")
        isLoggedIn.accept(true)
        val userManager = createUserManager()

        userManager.signInStateFlow().test {
            assertEquals(SignInState.SignedIn("fallback@pocketcasts.com", subscription = null), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `signInStateFlow retries the subscription fetch before giving up`() = runTest {
        whenever(subscriptionManager.fetchFreshSubscriptionResult()).thenReturn(Result.failure(RuntimeException("Fetch failed")))
        isLoggedIn.accept(true)
        val userManager = createUserManager()

        userManager.signInStateFlow().test {
            assertEquals(SignInState.SignedIn("user@pocketcasts.com", subscription = null), awaitItem())
            verify(subscriptionManager, times(3)).fetchFreshSubscriptionResult()
        }
    }

    @Test
    fun `getSignInState bridges the flow to a Flowable`() = runTest {
        val userManager = createUserManager()

        assertEquals(SignInState.SignedOut, userManager.getSignInState().awaitFirst())
    }

    @Test
    fun `signInStateFlow emits signed out while a subscription fetch is in flight`() = runTest {
        val fetchStarted = CompletableDeferred<Unit>()
        subscriptionManager.stub {
            on { fetchFreshSubscriptionResult() } doSuspendableAnswer {
                fetchStarted.complete(Unit)
                awaitCancellation()
            }
        }
        isLoggedIn.accept(true)
        val userManager = createUserManager()

        userManager.signInStateFlow().test {
            fetchStarted.await()
            isLoggedIn.accept(false)
            assertEquals(SignInState.SignedOut, awaitItem())
        }
    }

    private fun TestScope.createUserManager() = UserManagerImpl(
        application = mock(),
        settings = settings,
        syncManager = syncManager,
        subscriptionManager = subscriptionManager,
        podcastManager = mock(),
        userEpisodeManager = mock(),
        playlistDao = mock(),
        playlistsInitializer = mock(),
        analyticsController = mock(),
        eventHorizon = mock(),
        accountStatusInfo = mock(),
        applicationScope = CoroutineScope(StandardTestDispatcher(testScheduler)),
        crashLogging = mock(),
        experimentProvider = mock(),
        endOfYearSync = mock(),
        notificationScheduler = mock(),
        defaultDispatcher = StandardTestDispatcher(testScheduler),
    )
}
