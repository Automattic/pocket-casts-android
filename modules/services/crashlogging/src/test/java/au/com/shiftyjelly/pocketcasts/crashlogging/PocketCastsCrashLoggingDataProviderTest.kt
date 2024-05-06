package au.com.shiftyjelly.pocketcasts.crashlogging

import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeBuildDataProvider
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeCrashReportPermissionCheck
import au.com.shiftyjelly.pocketcasts.crashlogging.fakes.FakeObserveUser
import com.automattic.android.tracks.crashlogging.ErrorSampling
import org.junit.Assert.assertEquals
import org.junit.Test

class PocketCastsCrashLoggingDataProviderTest {

    private lateinit var sut: PocketCastsCrashLoggingDataProvider
    private val fakeBuildDataProvider = FakeBuildDataProvider()

    private fun setUp() {
        sut = PocketCastsCrashLoggingDataProvider(
            FakeObserveUser(),
            FakeCrashReportPermissionCheck(),
            fakeBuildDataProvider,
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
}
