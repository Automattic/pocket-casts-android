package au.com.shiftyjelly.pocketcasts.crashlogging

import au.com.shiftyjelly.pocketcasts.crashlogging.PocketCastsCrashLoggingDataProvider.Companion.GLOBAL_TAG_APP_PLATFORM
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeBuildDataProvider
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeCrashReportPermissionCheck
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeObserveUser
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.ErrorSampling
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PocketCastsCrashLoggingDataProviderTest {

    private lateinit var sut: PocketCastsCrashLoggingDataProvider
    private val fakeBuildDataProvider = FakeBuildDataProvider()
    private val fakeObserveUser = FakeObserveUser()

    private fun setUp() {
        sut = PocketCastsCrashLoggingDataProvider(
            fakeObserveUser,
            FakeCrashReportPermissionCheck(),
            fakeBuildDataProvider,
            localeProvider = { null },
        )
    }

    @Test
    fun `should provide specific error sampling for mobile platform`() {
        fakeBuildDataProvider.buildPlatform = "mobile"
        setUp()

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
            setUp()

            assertEquals(ErrorSampling.Disabled, sut.errorSampling)
        }
    }

    @Test
    fun `should attach platform tag`() {
        fakeBuildDataProvider.buildPlatform = "mobile"
        setUp()

        runBlocking {
            assertEquals(
                sut.applicationContextProvider.last()[GLOBAL_TAG_APP_PLATFORM],
                "mobile",
            )
        }
    }

    @Test
    fun `should provide user if available`() {
        fakeObserveUser.user = User("mail")
        setUp()

        runBlocking {
            assertEquals(
                CrashLoggingUser(userID = null, email = "mail", username = null),
                sut.user.last(),
            )
        }
    }

    @Test
    fun `should provide null if user is unavailable`() {
        fakeObserveUser.user = null
        setUp()

        runBlocking {
            assertEquals(null, sut.user.last())
        }
    }
}
