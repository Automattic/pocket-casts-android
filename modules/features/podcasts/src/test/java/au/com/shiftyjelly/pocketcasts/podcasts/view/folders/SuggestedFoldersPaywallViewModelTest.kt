package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersPaywallBottomSheet.Companion.CREATE_FOLDER_SOURCE
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersPaywallBottomSheet.Companion.PODCASTS_SOURCE
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
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
        viewModel.onDismissed(PODCASTS_SOURCE)

        verify(analyticsTracker).track(
            eq(AnalyticsEvent.SUGGESTED_FOLDERS_PAYWALL_MODAL_MAYBE_LATER_TAPPED),
            eq(mapOf("source" to PODCASTS_SOURCE)),
        )
        verify(settings).setDismissedSuggestedFolderPaywallTime()
        verify(settings).updateDismissedSuggestedFolderPaywallCount()
    }

    @Test
    fun `onDismissed should track event and not update settings`() {
        viewModel.onDismissed(CREATE_FOLDER_SOURCE)

        verify(analyticsTracker).track(
            eq(AnalyticsEvent.SUGGESTED_FOLDERS_PAYWALL_MODAL_MAYBE_LATER_TAPPED),
            eq(mapOf("source" to CREATE_FOLDER_SOURCE)),
        )
        verify(settings, times(0)).setDismissedSuggestedFolderPaywallTime()
        verify(settings, times(0)).updateDismissedSuggestedFolderPaywallCount()
    }

    @Test
    fun `onShown should track event`() {
        viewModel.onShown(PODCASTS_SOURCE)

        verify(analyticsTracker).track(
            eq(AnalyticsEvent.SUGGESTED_FOLDERS_PAYWALL_MODAL_SHOWN),
            eq(mapOf("source" to PODCASTS_SOURCE)),
        )
    }

    @Test
    fun `onUseTheseFolders should track event`() {
        viewModel.onUseTheseFolders(PODCASTS_SOURCE)

        verify(analyticsTracker).track(
            eq(AnalyticsEvent.SUGGESTED_FOLDERS_PAYWALL_MODAL_USE_THESE_FOLDERS_TAPPED),
            eq(mapOf("source" to PODCASTS_SOURCE)),
        )
    }
}
