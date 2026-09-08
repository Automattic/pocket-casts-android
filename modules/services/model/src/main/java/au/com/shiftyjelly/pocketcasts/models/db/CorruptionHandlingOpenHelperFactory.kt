package au.com.shiftyjelly.pocketcasts.models.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.io.File

class CorruptionHandlingOpenHelperFactory(
    private val delegate: SupportSQLiteOpenHelper.Factory,
    private val reporter: () -> DatabaseCorruptionReporter,
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val wrapped = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
            .name(configuration.name)
            .noBackupDirectory(configuration.useNoBackupDirectory)
            .allowDataLossOnRecovery(configuration.allowDataLossOnRecovery)
            .callback(BackupOnCorruptionCallback(configuration, reporter))
            .build()
        return delegate.create(wrapped)
    }

    private class BackupOnCorruptionCallback(
        private val configuration: SupportSQLiteOpenHelper.Configuration,
        private val reporter: () -> DatabaseCorruptionReporter,
    ) : SupportSQLiteOpenHelper.Callback(configuration.callback.version) {

        private val delegate = configuration.callback

        override fun onConfigure(db: SupportSQLiteDatabase) = delegate.onConfigure(db)

        override fun onCreate(db: SupportSQLiteDatabase) = delegate.onCreate(db)

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = delegate.onUpgrade(db, oldVersion, newVersion)

        override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = delegate.onDowngrade(db, oldVersion, newVersion)

        override fun onOpen(db: SupportSQLiteDatabase) = delegate.onOpen(db)

        override fun onCorruption(db: SupportSQLiteDatabase) {
            val name = configuration.name
            if (name != null) {
                runCatching { preserveAndReport(name) }
                    .onFailure { LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, it, "Failed to preserve corrupt database $name") }
            }
            delegate.onCorruption(db)
        }

        private fun preserveAndReport(name: String) {
            val databaseFile = configuration.context.getDatabasePath(name)
            val sizeBytes = if (databaseFile.exists()) databaseFile.length() else 0L
            val backup = backUp(databaseFile)
            reporter().onDatabaseCorrupted(name, backup?.absolutePath, sizeBytes)
        }

        private fun backUp(databaseFile: File): File? {
            if (!databaseFile.exists()) {
                return null
            }
            val directory = databaseFile.parentFile ?: return null
            val prefix = "${databaseFile.name}.$BACKUP_MARKER-"
            directory.listFiles { file -> file.name.startsWith(prefix) }?.forEach { it.delete() }

            val suffix = "$BACKUP_MARKER-${System.currentTimeMillis()}"
            val backup = File(directory, "${databaseFile.name}.$suffix")
            databaseFile.copyTo(backup, overwrite = true)
            for (extension in AUXILIARY_EXTENSIONS) {
                val auxiliary = File(directory, "${databaseFile.name}$extension")
                if (auxiliary.exists()) {
                    auxiliary.copyTo(File(directory, "${databaseFile.name}$extension.$suffix"), overwrite = true)
                }
            }
            return backup
        }
    }

    companion object {
        private const val BACKUP_MARKER = "corrupt"
        private val AUXILIARY_EXTENSIONS = listOf("-wal", "-shm")
    }
}
