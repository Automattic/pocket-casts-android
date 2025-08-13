package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.notifications

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EnableNotificationsPromptViewModelTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Mock
    lateinit var settings: Settings

    @Mock
    lateinit var mockMarketingSetting: UserSetting<Boolean>

    @Mock
    lateinit var analyticsTracker: AnalyticsTracker

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(settings.marketingOptIn).thenReturn(mockMarketingSetting)
    }

    @Test
    fun `should return old state when ff is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION, false)

        createViewModel().stateFlow.test {
            val state = awaitItem()

            assert(state is EnableNotificationsPromptViewModel.UiState.PreNewOnboarding)
        }
    }

    @Test
    fun `should return new state with defaults when ff is enabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION, true)

        createViewModel().stateFlow.test {
            val state = awaitItem()
            val expectedState = EnableNotificationsPromptViewModel.UiState.NewOnboarding(subscribedToNewsletter = true, notificationsEnabled = true)
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should update state when newsletter changes`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION, true)

        val viewModel = createViewModel()
        viewModel.changeNewsletterSubscription(false)

        viewModel.stateFlow.test {
            val state = awaitItem()
            val expectedState = EnableNotificationsPromptViewModel.UiState.NewOnboarding(subscribedToNewsletter = false, notificationsEnabled = true)
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should update state when notification setting changes`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION, true)

        val viewModel = createViewModel()
        viewModel.changeNotificationsEnabled(false)

        viewModel.stateFlow.test {
            val state = awaitItem()
            val expectedState = EnableNotificationsPromptViewModel.UiState.NewOnboarding(subscribedToNewsletter = true, notificationsEnabled = false)
            assertEquals(expectedState, state)
        }
    }

    @Test
    fun `should send message to request notifications when ff is off`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION, false)

        val viewModel = createViewModel()
        viewModel.messagesFlow.test {
            viewModel.handleCtaClick()
            val message = awaitItem()
            assertEquals(EnableNotificationsPromptViewModel.UiMessage.RequestPermission, message)
        }
    }

    @Test
    fun `should send message to request notifications when ff is on and notifications are enabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION, true)

        val viewModel = createViewModel()

        viewModel.messagesFlow.test {
            viewModel.handleCtaClick()
            val message = awaitItem()
            assertEquals(EnableNotificationsPromptViewModel.UiMessage.RequestPermission, message)
        }
        verify(mockMarketingSetting).set(true, true)
    }

    @Test
    fun `should send message to dismiss when ff is on and notifications are disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION, true)

        val viewModel = createViewModel()
        viewModel.changeNotificationsEnabled(false)

        viewModel.messagesFlow.test {
            viewModel.handleCtaClick()
            val message = awaitItem()
            assertEquals(EnableNotificationsPromptViewModel.UiMessage.Dismiss, message)
        }
    }

    private fun createViewModel() = EnableNotificationsPromptViewModel(
        settings = settings,
        analyticsTracker = analyticsTracker,
    )
}
