package au.com.shiftyjelly.pocketcasts.crashlogging

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.crashlogging.PocketCastsCrashLoggingDataProvider.Companion.GLOBAL_TAG_APP_PLATFORM
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeBuildDataProvider
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeCrashReportPermissionCheck
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeEncryptedLogging
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeObserveUser
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.ErrorSampling
import java.io.File
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PocketCastsCrashLoggingDataProviderTest {

    private lateinit var sut: PocketCastsCrashLoggingDataProvider
    private val fakeBuildDataProvider = FakeBuildDataProvider()
    private val fakeObserveUser = FakeObserveUser()

    @Before
    fun setUp() {
        sut = PocketCastsCrashLoggingDataProvider(
            fakeObserveUser,
            FakeCrashReportPermissionCheck(),
            fakeBuildDataProvider,
            FakeEncryptedLogging,
            File.createTempFile("test", "test"),
            localeProvider = { null },
            connectionStatusProvider = { true },
        )
    }

    @Test
    fun `should provide specific error sampling for mobile platform`() {
        fakeBuildDataProvider.buildPlatform = "mobile"

        assertEquals(
            ErrorSampling.Enabled(0.3),
            sut.errorSampling,
        )
    }

    @Test
    fun `should disable error sampling for other platforms`() {
        val notMobilePlatforms = listOf("automotive", "watch")

        notMobilePlatforms.forEach { build ->
            fakeBuildDataProvider.buildPlatform = build

            assertEquals(ErrorSampling.Disabled, sut.errorSampling)
        }
    }

    @Test
    fun `should attach platform tag`() {
        fakeBuildDataProvider.buildPlatform = "mobile"

        runBlocking {
            assertEquals(
                sut.applicationContextProvider.last()[GLOBAL_TAG_APP_PLATFORM],
                "mobile",
            )
        }
    }

    @Test
    fun `should provide user if available`() = runTest {
        fakeObserveUser.emitUser(User("mail"))

        sut.user.test {
            assertEquals(CrashLoggingUser(userID = null, email = "mail", username = null), expectMostRecentItem())
        }
    }

    @Test
    fun `should provide null if user is unavailable`() = runTest {
        sut.user.test {
            assertEquals(null, expectMostRecentItem())
        }
    }
}
