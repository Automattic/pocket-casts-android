package au.com.shiftyjelly.pocketcasts.models.db

interface DatabaseCorruptionReporter {
    fun onDatabaseCorrupted(databaseName: String, backupPath: String?, databaseSizeBytes: Long)
}
