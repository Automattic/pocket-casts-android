package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class SuggestedFoldersPaywallViewModelTest {

    @Mock
    lateinit var settings: Settings

    @Mock
    lateinit var analyticsTracker: AnalyticsTracker

    @Mock
    lateinit var userManager: UserManager

    lateinit var viewModel: SuggestedFoldersPaywallViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        viewModel = SuggestedFoldersPaywallViewModel(userManager, settings, analyticsTracker)
    }

    @Test
    fun `onDismissed should track event and update settings`() {
        viewModel.onDismissed()

        verify(analyticsTracker).track(AnalyticsEvent.SUGGESTED_FOLDERS_PAYWALL_MODAL_MAYBE_LATER_TAPPED)
        verify(settings).setDismissedSuggestedFolderPaywallTime()
        verify(settings).updateDismissedSuggestedFolderPaywallCount()
    }

    @Test
    fun `onShown should track event`() {
        viewModel.onShown()

        verify(analyticsTracker).track(AnalyticsEvent.SUGGESTED_FOLDERS_PAYWALL_MODAL_SHOWN)
    }

    @Test
    fun `onUseTheseFolders should track event`() {
        viewModel.onUseTheseFolders()

        verify(analyticsTracker).track(AnalyticsEvent.SUGGESTED_FOLDERS_PAYWALL_MODAL_USE_THESE_FOLDERS_TAPPED)
    }
}
