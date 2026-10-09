package au.com.shiftyjelly.pocketcasts.models.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.io.File
import java.io.IOException

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
            val databaseFile = if (configuration.useNoBackupDirectory) {
                File(configuration.context.noBackupFilesDir, name)
            } else {
                configuration.context.getDatabasePath(name)
            }
            val sizeBytes = if (databaseFile.exists()) databaseFile.length() else 0L
            val backup = runCatching { backUp(databaseFile) }
                .onFailure { LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, it, "Failed to back up corrupt database $name") }
                .getOrNull()
            reporter().onDatabaseCorrupted(name, backup?.absolutePath, sizeBytes)
        }

        private fun backUp(databaseFile: File): File? {
            if (!databaseFile.exists()) {
                return null
            }
            val directory = databaseFile.parentFile ?: return null

            val existingBackup = directory.listFiles { file -> file.name.startsWith("${databaseFile.name}.$BACKUP_MARKER-") }
                ?.maxByOrNull(File::length)
            if (existingBackup != null && existingBackup.length() >= databaseFile.length()) {
                return existingBackup
            }
            if (directory.usableSpace < databaseFile.length()) {
                return null
            }

            val suffix = "$BACKUP_MARKER-${System.currentTimeMillis()}"
            val auxiliaries = AUXILIARY_EXTENSIONS.map { File(directory, "${databaseFile.name}$it") }.filter(File::exists)
            val copies = listOf(databaseFile) + auxiliaries
            val targets = copies.map { File(directory, "${it.name}.$suffix") }
            try {
                copies.zip(targets).forEach { (source, target) -> source.copyTo(target, overwrite = true) }
            } catch (e: IOException) {
                targets.forEach(File::delete)
                throw e
            }

            pruneOlderBackups(directory, databaseFile, keepSuffix = suffix)
            return targets.first()
        }

        private fun pruneOlderBackups(directory: File, databaseFile: File, keepSuffix: String) {
            val backupBaseNames = listOf(databaseFile.name) + AUXILIARY_EXTENSIONS.map { "${databaseFile.name}$it" }
            directory.listFiles { file ->
                !file.name.endsWith(".$keepSuffix") &&
                    backupBaseNames.any { baseName -> file.name.startsWith("$baseName.$BACKUP_MARKER-") }
            }?.forEach { it.delete() }
        }
    }

    companion object {
        private const val BACKUP_MARKER = "corrupt"
        private val AUXILIARY_EXTENSIONS = listOf("-wal", "-shm")
    }
}
