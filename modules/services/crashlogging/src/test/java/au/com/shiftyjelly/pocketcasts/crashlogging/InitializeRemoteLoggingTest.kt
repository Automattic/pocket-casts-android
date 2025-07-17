package au.com.shiftyjelly.pocketcasts.crashlogging

import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeCrashLogging
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeEncryptedLogging
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class InitializeRemoteLoggingTest {

    val crashLogging = FakeCrashLogging()
    val encryptedLogging = FakeEncryptedLogging()

    private lateinit var sut: InitializeRemoteLogging

    @Before
    fun setUp() {
        sut = InitializeRemoteLogging(
            crashLogging,
            encryptedLogging,
        )
    }

    @Test
    fun `should initialize crash logging`() {
        sut()

        assertTrue(crashLogging.initialized)
    }

    @Test
    fun `should rest encrypted logging upload states`() {
        encryptedLogging.enqueueSendingEncryptedLogs(
            "",
            File.createTempFile("test", "test"),
            true,
        )

        sut()

        assertTrue(
            encryptedLogging.toUpload.all {
                it.second == FakeEncryptedLogging.UploadState.NOT_STARTED
            },
        )
    }

    @Test
    fun `should send encrypted logging`() {
        encryptedLogging.enqueueSendingEncryptedLogs(
            "",
            File.createTempFile("test", "test"),
            true,
        )
        encryptedLogging.enqueueSendingEncryptedLogs(
            "",
            File.createTempFile("test", "test"),
            false,
        )

        sut()

        assertTrue(encryptedLogging.uploaded.size == 2)
    }
}
