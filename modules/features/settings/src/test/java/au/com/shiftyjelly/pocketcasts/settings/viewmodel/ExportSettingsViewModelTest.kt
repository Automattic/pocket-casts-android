package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsImportByUrlEvent
import com.automattic.eventhorizon.SettingsImportExportEmailTappedEvent
import com.automattic.eventhorizon.SettingsImportExportFileTappedEvent
import com.automattic.eventhorizon.SettingsImportSelectFileEvent
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportSettingsViewModelTest {

    private val eventSink = TestEventSink()

    private lateinit var testSubject: ExportSettingsViewModel

    @Before
    fun setup() {
        testSubject = ExportSettingsViewModel(
            eventHorizon = EventHorizon(eventSink),
        )
    }

    @Test
    fun `onImportSelectFile should track analytics event`() = runTest {
        testSubject.onImportSelectFile()

        val event = eventSink.pollEvent()

        assertEquals(SettingsImportSelectFileEvent, event)
    }

    @Test
    fun `onImportByUrlClicked should track analytics event`() = runTest {
        testSubject.onImportByUrlClicked()

        val event = eventSink.pollEvent()

        assertEquals(SettingsImportByUrlEvent, event)
    }

    @Test
    fun `onExportByEmail should track analytics event`() = runTest {
        testSubject.onExportByEmail()

        val event = eventSink.pollEvent()

        assertEquals(SettingsImportExportEmailTappedEvent, event)
    }

    @Test
    fun `onExportFile should track analytics event`() = runTest {
        testSubject.onExportFile()

        val event = eventSink.pollEvent()

        assertEquals(SettingsImportExportFileTappedEvent, event)
    }
}
