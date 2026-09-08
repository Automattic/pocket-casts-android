package au.com.shiftyjelly.pocketcasts.repositories.database

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.automattic.android.tracks.crashlogging.CrashLogging
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class DatabaseCorruptionReporterImplTest {

    private val settings = mock<Settings> {
        on { getVersion() } doReturn "8.16"
        on { getVersionCode() } doReturn 9441
        on { getMigratedVersionCode() } doReturn 9441
        on { getFullySignedOut() } doReturn true
    }
    private val syncManager = mock<SyncManager> {
        on { isLoggedIn() } doReturn false
    }
    private val crashLogging = mock<CrashLogging>()

    private val reporter = DatabaseCorruptionReporterImpl(settings, syncManager, crashLogging)

    @Test
    fun `reports corruption with context tags`() {
        reporter.onDatabaseCorrupted("pocketcasts", "/databases/pocketcasts.corrupt-123", databaseSizeBytes = 4096L)

        val tags = argumentCaptor<Map<String, String>>()
        verify(crashLogging).sendReport(any(), tags.capture(), any())

        val captured = tags.firstValue
        assertEquals("pocketcasts", captured["database_name"])
        assertEquals("4096", captured["database_size_bytes"])
        assertEquals("true", captured["database_backed_up"])
        assertEquals("false", captured["logged_in"])
        assertEquals("true", captured["processed_sign_out"])
        assertEquals("9441", captured["migrated_version_code"])
    }

    @Test
    fun `marks the report when no backup was taken`() {
        reporter.onDatabaseCorrupted("pocketcasts", backupPath = null, databaseSizeBytes = 0L)

        val tags = argumentCaptor<Map<String, String>>()
        verify(crashLogging).sendReport(any(), tags.capture(), any())

        assertEquals("false", tags.firstValue["database_backed_up"])
    }
}
