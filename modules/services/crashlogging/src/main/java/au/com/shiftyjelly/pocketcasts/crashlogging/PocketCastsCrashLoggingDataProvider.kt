package au.com.shiftyjelly.pocketcasts.crashlogging

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.ErrorSampling
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.automattic.android.tracks.crashlogging.ReleaseName
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class PocketCastsCrashLoggingDataProvider @Inject constructor(
    private val settings: Settings,
    syncManager: SyncManager,
) : CrashLoggingDataProvider {

    override val applicationContextProvider: Flow<Map<String, String>> = flowOf(
        mapOf(
            GLOBAL_TAG_APP_PLATFORM to BuildConfig.BUILD_PLATFORM,
        ),
    )

    override val buildType: String = BuildConfig.BUILD_TYPE

    override val enableCrashLoggingLogs: Boolean = false

    override val locale: Locale = Locale.getDefault()

    override val performanceMonitoringConfig = PerformanceMonitoringConfig.Disabled

    override val releaseName = ReleaseName.SetByTracksLibrary

    override val sentryDSN: String = BuildConfig.SENTRY_DSN

    override val user: Flow<CrashLoggingUser?> = settings.linkCrashReportsToUser.flow.map { linkCrashReportsToUser ->
        if (linkCrashReportsToUser) {
            syncManager.getEmail()?.let { email ->
                CrashLoggingUser(
                    email = email,
                    userID = "",
                    username = "",
                )
            }
        } else {
            null
        }
    }

    override fun crashLoggingEnabled(): Boolean {
        return settings.sendCrashReports.value
    }

    override fun extraKnownKeys(): List<ExtraKnownKey> {
        return emptyList()
    }

    override fun provideExtrasForEvent(
        currentExtras: Map<ExtraKnownKey, String>,
        eventLevel: EventLevel,
    ): Map<ExtraKnownKey, String> {
        return emptyMap()
    }

    override fun shouldDropWrappingException(module: String, type: String, value: String): Boolean {
        return false
    }

    override val errorSampling: ErrorSampling =
        if (BuildConfig.BUILD_PLATFORM == "mobile") {
            ErrorSampling.Enabled(MOBILE_ERROR_SAMPLING)
        } else {
            ErrorSampling.Disabled
        }

    companion object {
        const val GLOBAL_TAG_APP_PLATFORM = "app.platform"
        const val MOBILE_ERROR_SAMPLING = 0.3
    }
}
