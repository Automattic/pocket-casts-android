package au.com.shiftyjelly.pocketcasts.models.db

interface DatabaseCorruptionReporter {
    fun onDatabaseCorrupted(databaseName: String, backupPath: String?, databaseSizeBytes: Long)

    companion object {
        val NONE = object : DatabaseCorruptionReporter {
            override fun onDatabaseCorrupted(databaseName: String, backupPath: String?, databaseSizeBytes: Long) = Unit
        }
    }
}
