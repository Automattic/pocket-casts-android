package au.com.shiftyjelly.pocketcasts.profile.cloud

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.ReadWriteSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsFilesAutoAddUpNextToggledEvent
import com.automattic.eventhorizon.SettingsFilesAutoDownloadFromCloudToggledEvent
import com.automattic.eventhorizon.SettingsFilesAutoUploadToCloudToggledEvent
import com.automattic.eventhorizon.SettingsFilesDeleteCloudFileAfterPlayingToggledEvent
import com.automattic.eventhorizon.SettingsFilesDeleteLocalFileAfterPlayingToggledEvent
import com.automattic.eventhorizon.SettingsFilesOnlyOnWifiToggledEvent
import com.automattic.eventhorizon.SettingsFilesShownEvent
import com.automattic.eventhorizon.UpgradeBannerDismissedEvent
import io.reactivex.Flowable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CloudSettingsViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val cloudAddToUpNextFlow = MutableStateFlow(false)
    private val deleteLocalFileAfterPlayingFlow = MutableStateFlow(false)
    private val deleteCloudFileAfterPlayingFlow = MutableStateFlow(false)
    private val cloudAutoUploadFlow = MutableStateFlow(false)
    private val cloudAutoDownloadFlow = MutableStateFlow(false)
    private val cloudDownloadOnlyOnWifiFlow = MutableStateFlow(false)

    private val cloudAddToUpNextSetting = mock<UserSetting<Boolean>> { on { flow } doReturn cloudAddToUpNextFlow }
    private val deleteLocalFileAfterPlayingSetting = mock<UserSetting<Boolean>> { on { flow } doReturn deleteLocalFileAfterPlayingFlow }
    private val deleteCloudFileAfterPlayingSetting = mock<UserSetting<Boolean>> { on { flow } doReturn deleteCloudFileAfterPlayingFlow }
    private val cloudAutoUploadSetting = mock<UserSetting<Boolean>> { on { flow } doReturn cloudAutoUploadFlow }
    private val cloudAutoDownloadSetting = mock<ReadWriteSetting<Boolean>> { on { flow } doReturn cloudAutoDownloadFlow }
    private val cloudDownloadOnlyOnWifiSetting = mock<UserSetting<Boolean>> { on { flow } doReturn cloudDownloadOnlyOnWifiFlow }

    private val settings = mock<Settings> {
        on { cloudAddToUpNext } doReturn cloudAddToUpNextSetting
        on { deleteLocalFileAfterPlaying } doReturn deleteLocalFileAfterPlayingSetting
        on { deleteCloudFileAfterPlaying } doReturn deleteCloudFileAfterPlayingSetting
        on { cloudAutoUpload } doReturn cloudAutoUploadSetting
        on { cloudAutoDownload } doReturn cloudAutoDownloadSetting
        on { cloudDownloadOnlyOnWifi } doReturn cloudDownloadOnlyOnWifiSetting
        on { getUpgradeClosedCloudSettings() } doReturn false
    }

    private val userManager = mock<UserManager>()

    private lateinit var eventSink: TestEventSink

    @Before
    fun setUp() {
        eventSink = TestEventSink()
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(SignInState.SignedOut))
    }

    private fun createViewModel() = CloudSettingsViewModel(
        eventHorizon = EventHorizon(eventSink),
        settings = settings,
        userManager = userManager,
    )

    private fun signedInAs(subscription: Subscription?) {
        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(SignInState.SignedIn(email = "user@example.com", subscription = subscription)),
        )
    }

    @Test
    fun `uiState reflects the initial preference values from settings`() = runTest {
        cloudAddToUpNextFlow.value = true
        deleteLocalFileAfterPlayingFlow.value = true
        deleteCloudFileAfterPlayingFlow.value = false
        cloudAutoUploadFlow.value = true
        cloudAutoDownloadFlow.value = false
        cloudDownloadOnlyOnWifiFlow.value = true

        createViewModel().uiState.test {
            val state = awaitItem()
            assertTrue(state.cloudAddToUpNext)
            assertTrue(state.deleteLocalFileAfterPlaying)
            assertFalse(state.deleteCloudFileAfterPlaying)
            assertTrue(state.cloudAutoUpload)
            assertFalse(state.cloudAutoDownload)
            assertTrue(state.cloudDownloadOnlyOnWifi)
        }
    }

    @Test
    fun `uiState emits updated preference values as settings flows change`() = runTest {
        createViewModel().uiState.test {
            assertFalse(awaitItem().cloudAutoUpload)

            cloudAutoUploadFlow.value = true
            assertTrue(awaitItem().cloudAutoUpload)

            cloudDownloadOnlyOnWifiFlow.value = true
            assertTrue(awaitItem().cloudDownloadOnlyOnWifi)
        }
    }

    @Test
    fun `isSignedInAsPlusOrPatron is true when signed in as Plus`() = runTest {
        signedInAs(Subscription.PlusPreview)

        createViewModel().uiState.test {
            assertTrue(awaitItem().isSignedInAsPlusOrPatron)
        }
    }

    @Test
    fun `isSignedInAsPlusOrPatron is true when signed in as Patron`() = runTest {
        signedInAs(Subscription.PatronPreview)

        createViewModel().uiState.test {
            assertTrue(awaitItem().isSignedInAsPlusOrPatron)
        }
    }

    @Test
    fun `isSignedInAsPlusOrPatron is false when signed out`() = runTest {
        // Flip a preference so the assertion can only pass if the combine actually emitted,
        // rather than matching the UiState() default.
        cloudAutoUploadFlow.value = true

        createViewModel().uiState.test {
            val state = awaitItem()
            assertTrue(state.cloudAutoUpload)
            assertFalse(state.isSignedInAsPlusOrPatron)
        }
    }

    @Test
    fun `isSignedInAsPlusOrPatron is false when signed in as a free user`() = runTest {
        signedInAs(subscription = null)
        cloudAutoUploadFlow.value = true

        createViewModel().uiState.test {
            val state = awaitItem()
            assertTrue(state.cloudAutoUpload)
            assertFalse(state.isSignedInAsPlusOrPatron)
        }
    }

    @Test
    fun `isUpgradeBannerVisible is true when not Plus or Patron and not previously dismissed`() = runTest {
        createViewModel().uiState.test {
            assertTrue(awaitItem().isUpgradeBannerVisible)
        }
    }

    @Test
    fun `isUpgradeBannerVisible is false when signed in as Plus even if not previously dismissed`() = runTest {
        signedInAs(Subscription.PlusPreview)

        createViewModel().uiState.test {
            assertFalse(awaitItem().isUpgradeBannerVisible)
        }
    }

    @Test
    fun `isUpgradeBannerVisible is false when previously dismissed even if not Plus or Patron`() = runTest {
        whenever(settings.getUpgradeClosedCloudSettings()).thenReturn(true)
        cloudAutoUploadFlow.value = true

        createViewModel().uiState.test {
            val state = awaitItem()
            assertTrue(state.cloudAutoUpload)
            assertFalse(state.isSignedInAsPlusOrPatron)
            assertFalse(state.isUpgradeBannerVisible)
        }
    }

    @Test
    fun `onUpgradeBannerDismissed persists the setting, hides the banner, and tracks the event`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().isUpgradeBannerVisible)

            viewModel.onUpgradeBannerDismissed(SourceView.FILES_SETTINGS)

            assertFalse(awaitItem().isUpgradeBannerVisible)
        }

        verify(settings).setUpgradeClosedCloudSettings(true)
        assertEquals(
            UpgradeBannerDismissedEvent(source = SourceView.FILES_SETTINGS.analyticsValue),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `setAddToUpNext persists the preference with updateModifiedAt and tracks the event`() {
        createViewModel().setAddToUpNext(true)

        verify(cloudAddToUpNextSetting).set(true, updateModifiedAt = true)
        assertEquals(SettingsFilesAutoAddUpNextToggledEvent(enabled = true), eventSink.pollEvent())
    }

    @Test
    fun `setDeleteLocalFileAfterPlaying persists the preference with updateModifiedAt and tracks the event`() {
        createViewModel().setDeleteLocalFileAfterPlaying(true)

        verify(deleteLocalFileAfterPlayingSetting).set(true, updateModifiedAt = true)
        assertEquals(SettingsFilesDeleteLocalFileAfterPlayingToggledEvent(enabled = true), eventSink.pollEvent())
    }

    @Test
    fun `setDeleteCloudFileAfterPlaying persists the preference with updateModifiedAt and tracks the event`() {
        createViewModel().setDeleteCloudFileAfterPlaying(true)

        verify(deleteCloudFileAfterPlayingSetting).set(true, updateModifiedAt = true)
        assertEquals(SettingsFilesDeleteCloudFileAfterPlayingToggledEvent(enabled = true), eventSink.pollEvent())
    }

    @Test
    fun `setCloudAutoUpload persists the preference with updateModifiedAt and tracks the event`() {
        createViewModel().setCloudAutoUpload(true)

        verify(cloudAutoUploadSetting).set(true, updateModifiedAt = true)
        assertEquals(SettingsFilesAutoUploadToCloudToggledEvent(enabled = true), eventSink.pollEvent())
    }

    @Test
    fun `setCloudAutoDownload persists the preference with updateModifiedAt and tracks the event`() {
        createViewModel().setCloudAutoDownload(true)

        verify(cloudAutoDownloadSetting).set(true, updateModifiedAt = true)
        assertEquals(SettingsFilesAutoDownloadFromCloudToggledEvent(enabled = true), eventSink.pollEvent())
    }

    @Test
    fun `setCloudOnlyWifi persists the preference with updateModifiedAt and tracks the event`() {
        createViewModel().setCloudOnlyWifi(true)

        verify(cloudDownloadOnlyOnWifiSetting).set(true, updateModifiedAt = true)
        assertEquals(SettingsFilesOnlyOnWifiToggledEvent(enabled = true), eventSink.pollEvent())
    }

    @Test
    fun `setters track the disabled state when a preference is turned off`() {
        createViewModel().setAddToUpNext(false)

        verify(cloudAddToUpNextSetting).set(false, updateModifiedAt = true)
        assertEquals(SettingsFilesAutoAddUpNextToggledEvent(enabled = false), eventSink.pollEvent())
    }

    @Test
    fun `onShown tracks SettingsFilesShownEvent`() {
        createViewModel().onShown()

        assertEquals(SettingsFilesShownEvent, eventSink.pollEvent())
    }

    @Test
    fun `onShown does not track SettingsFilesShownEvent while the fragment is changing configurations`() {
        val viewModel = createViewModel()

        viewModel.onFragmentPause(true)
        viewModel.onShown()

        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `onShown tracks again once the fragment is no longer changing configurations`() {
        val viewModel = createViewModel()

        viewModel.onFragmentPause(true)
        viewModel.onShown()
        viewModel.onFragmentPause(false)
        viewModel.onShown()

        assertEquals(SettingsFilesShownEvent, eventSink.pollEvent())
        assertTrue(eventSink.isEmpty())
    }
}
