package au.com.shiftyjelly.pocketcasts.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.ErrorSampling
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.automattic.android.tracks.crashlogging.ReleaseName
import com.automattic.encryptedlogging.EncryptedLogging
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class PocketCastsCrashLoggingDataProvider @Inject constructor(
    observeUser: ObserveUser,
    private val crashReportPermissionCheck: CrashReportPermissionCheck,
    buildDataProvider: BuildDataProvider,
    private val encryptedLogging: EncryptedLogging,
    private val applicationFilesDir: File,
    private val connectionStatusProvider: ConnectionStatusProvider,
    localeProvider: LocaleProvider,
) : CrashLoggingDataProvider {

    override val applicationContextProvider: Flow<Map<String, String>> = flowOf(
        mapOf(
            GLOBAL_TAG_APP_PLATFORM to BuildConfig.BUILD_PLATFORM,
        ),
    )

    override val buildType: String = BuildConfig.BUILD_TYPE

    override val enableCrashLoggingLogs: Boolean = false

    override val locale: Locale? = localeProvider.provideLocale()

    override val performanceMonitoringConfig = PerformanceMonitoringConfig.Enabled(
        sampleRate = 0.01,
        profilesSampleRate = 0.01,
    )

    override val releaseName = ReleaseName.SetByTracksLibrary

    override val sentryDSN: String = BuildConfig.SENTRY_DSN

    override val user: Flow<CrashLoggingUser?> = observeUser.invoke().map { userMail ->
        userMail?.let {
            CrashLoggingUser(email = it.email)
        }
    }

    override fun crashLoggingEnabled(): Boolean {
        return crashReportPermissionCheck.invoke()
    }

    override fun extraKnownKeys(): List<ExtraKnownKey> {
        return listOf(EXTRA_UUID)
    }

    override fun provideExtrasForEvent(
        currentExtras: Map<ExtraKnownKey, String>,
        eventLevel: EventLevel,
    ): Map<ExtraKnownKey, String> {
        return currentExtras + if (currentExtras[EXTRA_UUID] == null) {
            appendEncryptedLogsUuid(eventLevel)
        } else {
            emptyMap()
        }
    }

    private fun appendEncryptedLogsUuid(eventLevel: EventLevel): Map<ExtraKnownKey, String> {
        val uuid = UUID.randomUUID().toString()
        encryptedLogging.enqueueSendingEncryptedLogs(
            uuid,
            File(applicationFilesDir, "debug.log"),
            shouldUploadImmediately = eventLevel != EventLevel.FATAL &&
                connectionStatusProvider.isConnected(),
        )
        return mapOf(EXTRA_UUID to uuid)
    }

    override fun shouldDropWrappingException(module: String, type: String, value: String): Boolean {
        return false
    }

    override val errorSampling: ErrorSampling =
        if (buildDataProvider.buildPlatform == "mobile") {
            ErrorSampling.Enabled(MOBILE_ERROR_SAMPLING)
        } else {
            ErrorSampling.Disabled
        }

    companion object {
        const val GLOBAL_TAG_APP_PLATFORM = "app.platform"
        const val MOBILE_ERROR_SAMPLING = 0.3
        const val EXTRA_UUID = "uuid"
    }
}
