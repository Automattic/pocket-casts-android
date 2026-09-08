package au.com.shiftyjelly.pocketcasts.repositories.database

import au.com.shiftyjelly.pocketcasts.models.db.DatabaseCorruptionReporter
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.tracks.crashlogging.CrashLogging
import javax.inject.Inject

class DatabaseCorruptionReporterImpl @Inject constructor(
    private val settings: Settings,
    private val syncManager: SyncManager,
    private val crashLogging: CrashLogging,
) : DatabaseCorruptionReporter {

    override fun onDatabaseCorrupted(databaseName: String, backupPath: String?, databaseSizeBytes: Long) {
        val loggedIn = runCatching { syncManager.isLoggedIn() }.getOrDefault(false)
        val message = "Database $databaseName was corrupt and recreated empty (sizeBytes=$databaseSizeBytes, backedUp=${backupPath != null}, loggedIn=$loggedIn)"
        LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, message)
        crashLogging.sendReport(
            exception = DatabaseCorruptionException(databaseName),
            tags = mapOf(
                "database_name" to databaseName,
                "database_size_bytes" to databaseSizeBytes.toString(),
                "database_backed_up" to (backupPath != null).toString(),
                "logged_in" to loggedIn.toString(),
                "processed_sign_out" to settings.getFullySignedOut().toString(),
                "app_version" to settings.getVersion(),
                "app_version_code" to settings.getVersionCode().toString(),
                "migrated_version_code" to settings.getMigratedVersionCode().toString(),
            ),
            message = message,
        )
    }

    private class DatabaseCorruptionException(databaseName: String) : Exception("Room database corrupted: $databaseName")
}
