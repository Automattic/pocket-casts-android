@file:OptIn(ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.notifications

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import io.reactivex.Flowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class EnableNotificationsPromptViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var settings: Settings

    @Mock
    lateinit var analyticsTracker: AnalyticsTracker

    @Mock
    lateinit var userManager: UserManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(settings.marketingOptIn).thenReturn(UserSetting.Mock(initialValue = false, mock()))
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(SignInState.SignedIn(email = "", subscription = null)))
    }

    @Test
    fun `should return new state with defaults when ff is enabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.stateFlow.test {
            val state = awaitItem()
            val expectedState = EnableNotificationsPromptViewModel.UiState.NewOnboarding(subscribedToNewsletter = true, notificationsEnabled = true, showNewsletterOptIn = true)
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should return new state without newsletter when ff is enabled and user is not signed in`() = runTest {
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(SignInState.SignedOut))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.stateFlow.test {
            val state = awaitItem()
            val expectedState = EnableNotificationsPromptViewModel.UiState.NewOnboarding(subscribedToNewsletter = true, notificationsEnabled = true, showNewsletterOptIn = false)
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should return new state without newsletter when ff is enabled and user has already subscribed to newsletter`() = runTest {
        whenever(settings.marketingOptIn).thenReturn(UserSetting.Mock(initialValue = true, mock()))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.stateFlow.test {
            val state = awaitItem()
            val expectedState = EnableNotificationsPromptViewModel.UiState.NewOnboarding(subscribedToNewsletter = true, notificationsEnabled = true, showNewsletterOptIn = false)
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should update state when newsletter changes`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.changeNewsletterSubscription(false)

        viewModel.stateFlow.test {
            val state = awaitItem()
            val expectedState = EnableNotificationsPromptViewModel.UiState.NewOnboarding(subscribedToNewsletter = false, notificationsEnabled = true, showNewsletterOptIn = true)
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should update state when notification setting changes`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.changeNotificationsEnabled(false)

        viewModel.stateFlow.test {
            val state = awaitItem()
            val expectedState = EnableNotificationsPromptViewModel.UiState.NewOnboarding(subscribedToNewsletter = true, notificationsEnabled = false, showNewsletterOptIn = true)
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should send message to request notifications when ff is on and notifications are enabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.messagesFlow.test {
            viewModel.handleCtaClick()
            val message = awaitItem()
            assertEquals(EnableNotificationsPromptViewModel.UiMessage.RequestPermission, message)
        }
    }

    @Test
    fun `should send message to dismiss when ff is on and notifications are disabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.messagesFlow.test {
            viewModel.changeNotificationsEnabled(false)
            viewModel.handleCtaClick()
            val message = awaitItem()
            assertEquals(EnableNotificationsPromptViewModel.UiMessage.Dismiss, message)
        }
    }

    private fun createViewModel() = EnableNotificationsPromptViewModel(
        settings = settings,
        analyticsTracker = analyticsTracker,
        userManager = userManager,
    )
}
