package au.com.shiftyjelly.pocketcasts.models.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import java.util.Calendar
import timber.log.Timber

class DataOpenHelper(
    context: Context,
    databaseName: String,
) : SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(PODCAST_TABLE_CREATE)
        db.execSQL(EPISODE_TABLE_CREATE)
        db.execSQL(PLAYLISTS_TABLE_CREATE)
        db.execSQL(PLAYLIST_EPISODES_TABLE_CREATE)
        db.execSQL(PLAYER_EPISODES_TABLE_CREATE)
        db.execSQL(ADD_INDEX_TO_EPISODE_ON_PUBLISHED_DATE)
        db.execSQL(ADD_INDEX_TO_EPISODE_ON_UUID)
        db.execSQL(ADD_INDEX_TO_EPISODE_ON_PODCAST_ID)
        db.execSQL(ADD_INDEX_TO_PODCAST_ON_UUID)
        db.execSQL(ADD_INDEX_TO_EPISODE_ON_LAST_DOWNLOAD_ATTEMPT_DATE)
        addDefaultPlaylists(db)
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Timber.i("Downgrading database from version $oldVersion to $newVersion")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Timber.i("Upgrading database from version $oldVersion to $newVersion")

        // Example: Upgrading database from version 20 to 23
        var version = oldVersion
        // upgrade to version 2
        if (version == 1) {
            setNullAddedDates(db)
            updateLatestEpisodeFields(db)
            version = 2
        }
        // upgrade to version 3
        if (version == 2) version = 3
        // upgrade to version 4
        if (version == 3) {
            db.execSQL(PLAYLISTS_TABLE_CREATE)
            version = 4
        }
        // upgrade to version 5
        if (version == 4) {
            db.execSQL(ADD_EPISODES_TO_KEEP_TO_PODCASTS)
            db.execSQL(ADD_OVERRIDE_GLOBAL_SETTINGS_TO_PODCASTS)
            version = 5
        }
        // upgrade to version 6
        if (version == 5) {
            db.execSQL(ADD_ADDED_DATE_TO_EPISODES)
            version = 6
        }
        // upgrade to version 7
        if (version == 6) version = 7
        // upgrade to version 8
        if (version == 7) version = 8
        // upgrade to version 9
        if (version == 8) {
            db.execSQL("DROP TABLE playlist")
            // if the old version of the table create isn't used then the next migration breaks
            db.execSQL(
                """
                CREATE TABLE playlists (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid VARCHAR,
                title VARCHAR,
                sortPosition INTEGER,
                manual INTEGER DEFAULT 0,
                unplayed INTEGER,
                partiallyPlayed INTEGER,
                finished INTEGER,
                audioVideo INTEGER,
                allPodcasts INTEGER,
                podcastUuids VARCHAR,
                downloaded INTEGER,
                downloading INTEGER,
                notDownloaded INTEGER,
                autoDownload INTEGER,
                autoDownloadWifiOnly INTEGER,
                autoDownloadPowerOnly INTEGER,
                sortId INTEGER,iconId INTEGER,
                starred INTEGER,
                deleted INTEGER DEFAULT 0,
                syncStatus INTEGER DEFAULT 0)
                """.trimIndent(),
            )
            addDefaultPlaylists(db)
            version = 9
        }
        // upgrade to version 10
        if (version == 9) {
            with(db) {
                execSQL(
                    """
                    CREATE TABLE playlist_episodes (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    playlist_id INTEGER,
                    episode_uuid VARCHAR,
                    position INTEGER)
                    """.trimIndent(),
                )
                execSQL("ALTER TABLE podcast ADD COLUMN is_deleted INTEGER DEFAULT 0")
                execSQL("ALTER TABLE podcast ADD COLUMN sync_status INTEGER DEFAULT 0")
                execSQL("ALTER TABLE podcast ADD COLUMN author VARCHAR")
                execSQL("ALTER TABLE episode ADD COLUMN is_deleted INTEGER DEFAULT 0")
                execSQL("ALTER TABLE episode ADD COLUMN sync_status INTEGER DEFAULT 1")
                execSQL("ALTER TABLE episode ADD COLUMN auto_download_status INTEGER DEFAULT 0")
                execSQL("ALTER TABLE episode ADD COLUMN starred INTEGER DEFAULT 0")
            }
            version = 10
        }
        // upgrade to version 11
        if (version == 10) version = 11
        // upgrade to version 12
        if (version == 11) {
            db.execSQL("ALTER TABLE podcast ADD COLUMN playback_speed DECIMAL DEFAULT 1")
            version = 12
        }
        // upgrade to version 13
        if (version == 12) version = 13
        // upgrade to version 14
        if (version == 13) {
            db.execSQL("DROP TABLE IF EXISTS playlist_episodes")
            db.execSQL(
                """
                CREATE TABLE playlist_episodes (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                playlistId INTEGER,
                episodeUuid VARCHAR,
                position INTEGER)
                """.trimIndent(),
            )
            version = 14
        }
        // upgrade to version 15
        if (version == 14) {
            db.execSQL("ALTER TABLE podcast ADD COLUMN is_folder INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE podcast ADD COLUMN subscribed INTEGER DEFAULT 1")
            version = 15
        }
        // upgrade to version 16
        if (version == 15) {
            db.execSQL("ALTER TABLE episode ADD COLUMN thumbnail_status INTEGER DEFAULT 0")
            version = 16
        }
        // upgrade to version 17
        if (version == 16) {
            db.execSQL("UPDATE podcast SET override_global_settings = 0")
            version = 17
        }
        // upgrade to version 18
        if (version == 17) {
            db.execSQL("ALTER TABLE podcast ADD COLUMN start_from INTEGER")
            version = 18
        }
        // upgrade to version 19
        if (version == 18) {
            db.execSQL("UPDATE podcast SET sync_status = 0 WHERE start_from IS NOT NULL AND start_from > 0")
            version = 19
        }
        // upgrade to version 20
        if (version == 19) {
            db.execSQL(ADD_INDEX_TO_EPISODE_ON_PUBLISHED_DATE)
            version = 20
        }
        // upgrade to version 21
        if (version == 20) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("auto_download_status")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN auto_download_status INTEGER DEFAULT 0")
            }
            version = 21
        }
        // upgrade to version 22
        if (version == 21) {
            db.execSQL("DROP TABLE IF EXISTS player_episodes")
            db.execSQL("CREATE TABLE player_episodes (_id INTEGER PRIMARY KEY AUTOINCREMENT, episodeUuid VARCHAR, position INTEGER, playlistId INTEGER)")
            version = 22
        }
        // upgrade to version 23
        if (version == 22) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("primary_color")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN primary_color INTEGER")
            }
            if (!columnNames.contains("secondary_color")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN secondary_color INTEGER")
            }
            if (!columnNames.contains("detail_color")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN detail_color INTEGER")
            }
            if (!columnNames.contains("background_color")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN background_color INTEGER")
            }
            version = 23
        }
        // upgrade to version 24
        if (version == 23) {
            val columnNames = getColumnNames(db, "episode")
            if (!columnNames.contains("play_error_details")) {
                db.execSQL("ALTER TABLE episode ADD COLUMN play_error_details VARCHAR")
            }
            version = 24
        }
        // upgrade to version 25
        if (version == 24) {
            db.execSQL("UPDATE episode SET thumbnail_status = 0")
            version = 25
        }
        // upgrade to version 26
        if (version == 25) {
            clearPodcastTintColours(db)
            version = 26
        }
        // upgrade to version 27
        if (version == 26) {
            val columnNames = getColumnNames(db, "episode")
            if (!columnNames.contains("playing_status_modified")) {
                db.execSQL("ALTER TABLE episode ADD COLUMN playing_status_modified INTEGER")
            }
            if (!columnNames.contains("played_up_to_modified")) {
                db.execSQL("ALTER TABLE episode ADD COLUMN played_up_to_modified INTEGER")
            }
            if (!columnNames.contains("duration_modified")) {
                db.execSQL("ALTER TABLE episode ADD COLUMN duration_modified INTEGER")
            }
            if (!columnNames.contains("is_deleted_modified")) {
                db.execSQL("ALTER TABLE episode ADD COLUMN is_deleted_modified INTEGER")
            }
            if (!columnNames.contains("starred_modified")) {
                db.execSQL("ALTER TABLE episode ADD COLUMN starred_modified INTEGER")
            }
            version = 27
        }
        // upgrade to version 28
        if (version == 27) {
            val columnNames = getColumnNames(db, "playlists")
            if (!columnNames.contains("autoDownloadIncludeHotspots")) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN autoDownloadIncludeHotspots INTEGER")
            }
            version = 28
        }
        // upgrade to version 29
        if (version == 28) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("show_notifications")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN show_notifications INTEGER DEFAULT 0")
            }
            version = 29
        }
        // upgrade to version 30
        if (version == 29) {
            clearPodcastTintColours(db)
            version = 30
        }
        // upgrade to version 31
        if (version == 30) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("most_popular_color")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN most_popular_color INTEGER")
            }
            version = 31
        }
        // upgrade to version 32
        if (version == 31) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("silence_removed")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN silence_removed INTEGER DEFAULT 0")
            }
            if (!columnNames.contains("volume_boosted")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN volume_boosted INTEGER DEFAULT 0")
            }
            version = 32
        }
        // upgrade to version 33
        if (version == 32) {
            db.execSQL(ADD_INDEX_TO_EPISODE_ON_PODCAST_ID)
            version = 33
        }
        // upgrade to version 34
        if (version == 33) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("secondary_top_color")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN secondary_top_color INTEGER")
            }
            version = 34
        }
        // upgrade to version 35
        if (version == 34) version = 35
        // upgrade to version 36
        if (version == 35) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("dark_background_color")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN dark_background_color INTEGER")
            }
            if (!columnNames.contains("light_overlay_color")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN light_overlay_color INTEGER")
            }
            version = 36
        }
        // upgrade to version 37
        if (version == 36) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("sort_order")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN sort_order INTEGER")
            }
            version = 37
        }
        // upgrade to version 38
        if (version == 37) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("episodes_sort_order")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN episodes_sort_order INTEGER")
                db.execSQL("UPDATE podcast SET episodes_sort_order = 3")
            }
            version = 38
        }
        // upgrade to version 39
        if (version == 38) {
            val columnNames = getColumnNames(db, "playlists")
            if (!columnNames.contains("filterHours")) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN filterHours INTEGER DEFAULT 0")
            }
            version = 39
        }
        // upgrade to version 40
        if (version == 39) {
            var sortPosition = maxColumn(db, "playlists", "sortPosition")
            sortPosition++
            if (sortPosition <= 0) sortPosition = 1

            val insert = "INSERT INTO playlists (uuid, title, unplayed, partiallyPlayed, finished, audioVideo, allPodcasts, downloaded, downloading, notDownloaded, autoDownload, autoDownloadWifiOnly, autoDownloadIncludeHotspots, autoDownloadPowerOnly, sortId, iconId, starred, syncStatus, sortPosition, filterHours) VALUES "
            // 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18
            db.execSQL("$insert('2797DCF8-1C93-4999-B52A-D1849736FA2C', 'New Releases', 1, 1, 0, 0, 1, 1, 1, 1, 0,  0,  0,  0,  0, 10,  0,  1,  $sortPosition, 336);")
            sortPosition++
            db.execSQL("$insert('D89A925C-5CE1-41A4-A879-2751838CE5CE', 'In Progress',  0, 1, 0, 0, 1, 1, 1, 1, 0,  0,  0,  0,  0, 23,  0,  1,  $sortPosition, 0);")

            if (count(db, "playlists", "UPPER(title) = 'STARRED'") == 0) {
                sortPosition++
                db.execSQL("$insert('78EC673E-4C3A-4985-9D83-7A79C825A359', 'Starred',  1, 1, 1, 0, 1, 1, 1, 1, 0,  0,  0,  0,  0, 39,  1,  1,  $sortPosition, 0);")
            }
            version = 40
        }
        // upgrade to version 41
        if (version == 40) {
            val columnNames = getColumnNames(db, "player_episodes")
            if (!columnNames.contains("title")) {
                db.execSQL("ALTER TABLE player_episodes ADD COLUMN title VARCHAR")
            }
            if (!columnNames.contains("publishedDate")) {
                db.execSQL("ALTER TABLE player_episodes ADD COLUMN publishedDate INTEGER")
            }
            if (!columnNames.contains("downloadUrl")) {
                db.execSQL("ALTER TABLE player_episodes ADD COLUMN downloadUrl VARCHAR")
            }
            if (!columnNames.contains("podcastUuid")) {
                db.execSQL("ALTER TABLE player_episodes ADD COLUMN podcastUuid VARCHAR")
            }
            version = 41
        }
        // upgrade to version 42
        if (version == 41) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("fab_for_light_bg")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN fab_for_light_bg INTEGER")
            }
            if (!columnNames.contains("link_for_light_bg")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN link_for_light_bg INTEGER")
            }
            if (!columnNames.contains("link_for_dark_bg")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN link_for_dark_bg INTEGER")
            }
            if (!columnNames.contains("color_version")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN color_version INTEGER DEFAULT 0")
            }
            if (!columnNames.contains("color_last_downloaded")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN color_last_downloaded INTEGER")
            }
            version = 42
        }
        // upgrade to version 43
        if (version == 42) {
            val columnNames = getColumnNames(db, "podcast")
            if (!columnNames.contains("auto_add_to_up_next")) {
                db.execSQL("ALTER TABLE podcast ADD COLUMN auto_add_to_up_next INTEGER DEFAULT 0")
            }
            version = 43
        }
        // upgrade to version 44
        if (version == 43) {
            val columnNames = getColumnNames(db, "episode")
            if (!columnNames.contains("last_download_attempt_date")) {
                db.execSQL("ALTER TABLE episode ADD COLUMN last_download_attempt_date INTEGER DEFAULT 0")
                db.execSQL(ADD_INDEX_TO_EPISODE_ON_LAST_DOWNLOAD_ATTEMPT_DATE)
            }
            version = 44
        }
        // upgrade to version 45
        if (version == 44) {
            val columnNames = getColumnNames(db, "episode")
            if (!columnNames.contains("show_notes")) db.execSQL("UPDATE episode SET show_notes = NULL")
        }
    }

    private fun addDefaultPlaylists(db: SQLiteDatabase) {
        val insert = "INSERT INTO playlists (uuid, title, unplayed, partiallyPlayed, finished, audioVideo, allPodcasts, downloaded, downloading, notDownloaded, autoDownload, autoDownloadWifiOnly, autoDownloadIncludeHotspots, autoDownloadPowerOnly, sortId, iconId, starred, syncStatus, sortPosition, filterHours) VALUES "
        db.execSQL("$insert('2797DCF8-1C93-4999-B52A-D1849736FA2C', 'New Releases', 1, 1, 0, 0, 1, 1, 1, 1, 0,  0,  0,  0,  0, 10,  0,  1,  1,  336);")
        db.execSQL("$insert('D89A925C-5CE1-41A4-A879-2751838CE5CE', 'In Progress',  0, 1, 0, 0, 1, 1, 1, 1, 0,  0,  0,  0,  0, 23,  0,  1,  2,  0);")
        db.execSQL("$insert('78EC673E-4C3A-4985-9D83-7A79C825A359', 'Starred',      1, 1, 1, 0, 1, 1, 1, 1, 0,  0,  0,  0,  0, 39,  1,  1,  3,  0);")

//        // 1. unplayed
//        // 2. partiallyPlayed
//        // 3. finished
//        // 4. audioVideo
//        // 5. allPodcasts
//        // 6. downloaded
//        // 7. downloading
//        // 8. notDownloaded
//        // 9. autoDownload
//        // 10. autoDownloadWifiOnly
//        // 11. autoDownloadIncludeHotspots
//        // 12. autoDownloadPowerOnly
//        // 13. sortId
//        // 14. iconId
//        // 15. starred
//        // 16. syncStatus
//        // 17. sortPosition
//        // 18. filterHours

//        Old playlists
//        String insert = "INSERT INTO playlists (uuid, title, unplayed, partiallyPlayed, finished, audioVideo, allPodcasts, downloaded, downloading, notDownloaded, autoDownload, autoDownloadWifiOnly, autoDownloadIncludeHotspots, autoDownloadPowerOnly, sortId, iconId, starred, syncStatus, sortPosition) VALUES ";
//        db.execSQL(insert + "('ad036f20-1118-0130-36a2-60f8472cfba6', 'Unplayed',   1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,  8, 0, 1, 1);");
//        db.execSQL(insert + "('ad036980-1118-0130-36a2-60f8472cfba6', 'Audio',      1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 27, 0, 1, 2);");
//        db.execSQL(insert + "('ad036d60-1118-0130-36a2-60f8472cfba6', 'Video',      1, 1, 0, 2, 1, 1, 1, 1, 0, 0, 0, 0, 0, 31, 0, 1, 3);");
//        db.execSQL(insert + "('ad0370b0-1118-0130-36a2-60f8472cfba6', 'Downloaded', 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 15, 0, 1, 4);");
    }

    private fun getColumnNames(db: SQLiteDatabase, tableName: String): List<String> {
        val names: MutableList<String> = ArrayList()
        try {
            db.rawQuery("select * from $tableName limit 1", null).use { cursor ->
                names.addAll(listOf(*cursor.columnNames))
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return names
    }

    private fun setNullAddedDates(database: SQLiteDatabase) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        try {
            database.use { db ->
                db.transaction {
                    db.rawQuery("SELECT uuid FROM podcast", arrayOf()).use { cursor ->
                        if (cursor.moveToFirst()) {
                            do {
                                val uuid = cursor.getString(0)
                                db.execSQL("UPDATE podcast SET added_date=? WHERE uuid=?", arrayOf<Any>(cal.timeInMillis, uuid))
                                cal.add(Calendar.MINUTE, 1)
                            } while (cursor.moveToNext())
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Migrating database to version 2")
        }
    }

    private fun updateLatestEpisodeFields(database: SQLiteDatabase) {
        try {
            database.use { db ->
                db.transaction {
                    db.execSQL("ALTER TABLE podcast ADD latest_episode_date INTEGER", arrayOf())
                    db.execSQL("UPDATE podcast SET latest_episode_uuid = NULL", arrayOf())
                }
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Migrating database to version 2")
        }
    }

    private fun clearPodcastTintColours(db: SQLiteDatabase) {
        db.execSQL("UPDATE podcast SET primary_color = NULL, secondary_color = NULL")
    }

    @Suppress("SameParameterValue")
    private fun maxColumn(db: SQLiteDatabase, table: String, column: String): Int {
        val result = firstRowArray("SELECT MAX($column) FROM $table", arrayOf(), db)
        return result?.firstNotNullOfOrNull { it?.toIntOrNull() } ?: 0
    }

    @Suppress("SameParameterValue")
    private fun count(db: SQLiteDatabase, name: String, where: String?): Int {
        val whereSql = if (!where.isNullOrEmpty()) " WHERE $where" else ""
        val result = firstRowArray("SELECT count(*) FROM $name$whereSql", arrayOf(), db)
        return result?.firstOrNull()?.toInt() ?: 0
    }

    private fun firstRowArray(query: String, params: Array<String?>, db: SQLiteDatabase): Array<String?>? {
        return try {
            db.rawQuery(query, params).use { cursor ->
                if (cursor.moveToNext()) {
                    val result = arrayOfNulls<String>(cursor.columnCount)
                    for (i in 0 until cursor.columnCount) {
                        result[i] = cursor.getString(i)
                    }
                    result
                } else {
                    null
                }
            }
        } catch (exception: Exception) {
            Timber.e(exception)
            null
        }
    }

    companion object {
        private const val DATABASE_VERSION = 45

        private const val PODCAST_TABLE_CREATE =
            """
            CREATE TABLE podcast (
            uuid VARCHAR PRIMARY KEY,
            added_date INTEGER,
            thumbnail_url VARCHAR,
            title VARCHAR,
            podcast_language VARCHAR,
            podcast_category VARCHAR,
            media_type VARCHAR,
            podcast_description VARCHAR,
            latest_episode_uuid VARCHAR,
            latest_episode_date INTEGER,
            playback_speed DECIMAL DEFAULT 1,
            silence_removed INTEGER DEFAULT 0,
            volume_boosted INTEGER DEFAULT 0,
            is_deleted INTEGER DEFAULT 0,
            sync_status INTEGER DEFAULT 0,
            author VARCHAR,is_folder INTEGER DEFAULT 0,
            subscribed INTEGER DEFAULT 1,
            podcast_url VARCHAR,
            override_global_settings BOOLEAN DEFAULT 0,
            episodes_to_keep INTEGER DEFAULT 0,
            start_from INTEGER,sort_order INTEGER,
            episodes_sort_order INTEGER,
            primary_color INTEGER,
            secondary_color INTEGER,
            light_overlay_color INTEGER,
            most_popular_color INTEGER,
            fab_for_light_bg INTEGER,
            link_for_light_bg INTEGER,
            link_for_dark_bg INTEGER,
            color_version INTEGER DEFAULT 0,
            color_last_downloaded INTEGER,
            auto_download_status INTEGER DEFAULT 0,
            show_notifications INTEGER DEFAULT 0,
            auto_add_to_up_next INTEGER DEFAULT 0)
            """

        private const val EPISODE_TABLE_CREATE =
            """
            CREATE TABLE episode (
            uuid VARCHAR PRIMARY KEY,
            episode_status INTEGER,
            playing_status INTEGER,
            podcast_id VARCHAR,
            published_date INTEGER,
            duration DECIMAL,
            size_in_bytes DECIMAL,
            played_up_to DECIMAL,
            download_url VARCHAR,
            downloaded_file_path VARCHAR,
            file_type VARCHAR,
            title VARCHAR,
            episode_description VARCHAR,
            downloaded_error_details VARCHAR,
            play_error_details VARCHAR,
            is_deleted INTEGER DEFAULT 0,
            auto_download_status INTEGER DEFAULT 0,
            thumbnail_status INTEGER DEFAULT 0,
            starred INTEGER DEFAULT 0,
            added_date INTEGER,
            playing_status_modified INTEGER,
            played_up_to_modified INTEGER,
            duration_modified INTEGER,
            is_deleted_modified INTEGER,
            starred_modified INTEGER,
            last_download_attempt_date INTEGER DEFAULT 0)
            """

        private const val PLAYLIST_EPISODES_TABLE_CREATE =
            """
            CREATE TABLE playlist_episodes (
            _id INTEGER PRIMARY KEY AUTOINCREMENT,
            playlistId INTEGER,
            episodeUuid VARCHAR,
            position INTEGER)
            """

        private const val PLAYLISTS_TABLE_CREATE =
            """
            CREATE TABLE playlists (
            _id INTEGER PRIMARY KEY AUTOINCREMENT,
            uuid VARCHAR,
            title VARCHAR,
            sortPosition INTEGER,
            manual INTEGER DEFAULT 0,
            unplayed INTEGER,
            partiallyPlayed INTEGER,
            finished INTEGER,
            audioVideo INTEGER,
            allPodcasts INTEGER,
            podcastUuids VARCHAR,
            downloaded INTEGER,
            downloading INTEGER,
            notDownloaded INTEGER,
            autoDownload INTEGER,
            autoDownloadWifiOnly INTEGER,
            autoDownloadIncludeHotspots INTEGER,
            autoDownloadPowerOnly INTEGER,
            sortId INTEGER,
            iconId INTEGER,
            starred INTEGER,
            deleted INTEGER DEFAULT 0,
            syncStatus INTEGER DEFAULT 0,
            filterHours INTEGER DEFAULT 0)
            """

        private const val PLAYER_EPISODES_TABLE_CREATE =
            """
            CREATE TABLE player_episodes (
            _id INTEGER PRIMARY KEY AUTOINCREMENT,
            episodeUuid VARCHAR,
            position INTEGER,
            playlistId INTEGER,
            title VARCHAR,
            publishedDate INTEGER,
            downloadUrl VARCHAR,
            podcastUuid VARCHAR)
            """

        private const val ADD_EPISODES_TO_KEEP_TO_PODCASTS = "ALTER TABLE podcast ADD COLUMN episodes_to_keep INTEGER DEFAULT 0"

        private const val ADD_OVERRIDE_GLOBAL_SETTINGS_TO_PODCASTS = "ALTER TABLE podcast ADD COLUMN override_global_settings BOOLEAN DEFAULT 0"

        private const val ADD_ADDED_DATE_TO_EPISODES = "ALTER TABLE episode ADD COLUMN added_date INTEGER"

        private const val ADD_INDEX_TO_EPISODE_ON_UUID = "CREATE INDEX episode_uuid_idx ON episode(uuid);"

        private const val ADD_INDEX_TO_EPISODE_ON_PUBLISHED_DATE = "CREATE INDEX episode_published_date_idx ON episode(published_date);"

        private const val ADD_INDEX_TO_EPISODE_ON_PODCAST_ID = "CREATE INDEX episode_podcast_id_idx ON episode(podcast_id);"

        private const val ADD_INDEX_TO_PODCAST_ON_UUID = "CREATE INDEX podcast_uuid_idx ON podcast(uuid);"

        private const val ADD_INDEX_TO_EPISODE_ON_LAST_DOWNLOAD_ATTEMPT_DATE = "CREATE INDEX episode_last_download_attempt_date_idx ON episode(last_download_attempt_date);"
    }
}
