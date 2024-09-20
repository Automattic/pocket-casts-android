package au.com.shiftyjelly.pocketcasts.crashlogging

import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeCrashLogging
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeEncryptedLogging
import java.io.File
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class InitializeRemoteLoggingTest {

    val crashLogging = FakeCrashLogging()
    val encryptedLogging = FakeEncryptedLogging()
    val testScope = TestScope()

    private lateinit var sut: InitializeRemoteLogging

    @Before
    fun setUp() {
        sut = InitializeRemoteLogging(
            crashLogging,
            encryptedLogging,
        ) { testScope }
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

        testScope.runTest {
            sut()
            testScheduler.runCurrent()

            assertTrue(encryptedLogging.uploaded.size == 2)
        }
    }
}
