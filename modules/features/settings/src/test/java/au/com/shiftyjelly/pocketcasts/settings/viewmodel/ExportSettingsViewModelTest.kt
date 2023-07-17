package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ExportSettingsViewModelTest {
    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var testSubject: ExportSettingsViewModel

    @Before
    fun setup() {
        testSubject = ExportSettingsViewModel(analyticsTracker)
    }

    @Test
    fun `onImportSelectFile should track analytics event`() = runTest {
        testSubject.onImportSelectFile()
        verify(analyticsTracker).track(AnalyticsEvent.SETTINGS_IMPORT_SELECT_FILE)
    }

    @Test
    fun `onImportByUrlClicked should track analytics event`() = runTest {
        testSubject.onImportByUrlClicked()
        verify(analyticsTracker).track(AnalyticsEvent.SETTINGS_IMPORT_BY_URL)
    }
    @Test
    fun `onExportByEmail should track analytics event`() = runTest {
        testSubject.onExportByEmail()
        verify(analyticsTracker).track(AnalyticsEvent.SETTINGS_IMPORT_EXPORT_EMAIL_TAPPED)
    }
    @Test
    fun `onExportFile should track analytics event`() = runTest {
        testSubject.onExportFile()
        verify(analyticsTracker).track(AnalyticsEvent.SETTINGS_IMPORT_EXPORT_FILE_TAPPED)
    }
}
