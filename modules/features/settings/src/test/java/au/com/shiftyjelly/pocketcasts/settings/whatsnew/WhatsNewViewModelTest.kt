package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.ReadSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WhatsNewViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var settings: Settings

    private fun createViewModel(subscription: Subscription?): WhatsNewViewModel {
        val cachedSubscription = mock<ReadSetting<Subscription?>> {
            on { flow } doReturn MutableStateFlow(subscription)
        }
        whenever(settings.cachedSubscription).thenReturn(cachedSubscription)
        return WhatsNewViewModel(settings)
    }

    @Test
    fun `Plus user sees Got it button`() = runTest {
        val viewModel = createViewModel(subscription = Subscription.PlusPreview)

        val state = viewModel.state.value as WhatsNewViewModel.UiState.Loaded
        assertTrue(state.feature.isUserEntitled)
        assertEquals(LR.string.got_it, state.feature.confirmButtonTitle)
    }

    @Test
    fun `Plus user confirm force closes`() = runTest {
        val viewModel = createViewModel(subscription = Subscription.PlusPreview)

        viewModel.navigationState.test {
            viewModel.onConfirm()
            assertEquals(WhatsNewViewModel.NavigationState.ForceClose, awaitItem())
        }
    }

    @Test
    fun `free user sees Start Free Trial button`() = runTest {
        val viewModel = createViewModel(subscription = null)

        val state = viewModel.state.value as WhatsNewViewModel.UiState.Loaded
        assertEquals(false, state.feature.isUserEntitled)
        assertEquals(LR.string.profile_start_free_trial, state.feature.confirmButtonTitle)
    }

    @Test
    fun `free user confirm starts upsell flow`() = runTest {
        val viewModel = createViewModel(subscription = null)

        viewModel.navigationState.test {
            viewModel.onConfirm()
            val target = awaitItem() as WhatsNewViewModel.NavigationState.StartUpsellFlow
            assertEquals(OnboardingUpgradeSource.SYNCED_TRANSCRIPTS, target.source)
        }
    }
}
