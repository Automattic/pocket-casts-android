package au.com.shiftyjelly.pocketcasts.models.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import com.squareup.moshi.Moshi
import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PcDroid687DatabaseLossReproTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Rule
    @JvmField
    val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    private class RecordingReporter : DatabaseCorruptionReporter {
        var calls = 0
        var databaseName: String? = null
        var backupPath: String? = null
        var databaseSizeBytes: Long = -1

        override fun onDatabaseCorrupted(databaseName: String, backupPath: String?, databaseSizeBytes: Long) {
            calls++
            this.databaseName = databaseName
            this.backupPath = backupPath
            this.databaseSizeBytes = databaseSizeBytes
        }
    }

    private val insertPodcast =
        "INSERT INTO podcasts (uuid, title, podcast_description, podcast_html_description, podcast_category, podcast_language, author, sort_order, episodes_sort_order, episodes_to_keep, override_global_settings, override_global_effects, start_from, playback_speed, volume_boosted, is_folder, subscribed, show_notifications, auto_download_status, auto_add_to_up_next, most_popular_color, primary_color, secondary_color, light_overlay_color, fab_for_light_bg, link_for_dark_bg, link_for_light_bg, color_version, color_last_downloaded, sync_status, exclude_from_auto_archive, override_global_archive, auto_archive_played_after, auto_archive_inactive_after, auto_archive_episode_limit, grouping, skip_last, show_archived, trim_silence_level, refresh_available, licensing, isPaid, is_private, is_header_expanded, slug, web_feed, clean_title) VALUES ('podcast-1', 'Test Show', '', '', '', '', '', 0, 0, 0, 0, 0, 0, 0.0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 0, '')"
    private val insertEpisode =
        "INSERT INTO podcast_episodes (uuid, episode_description, published_date, title, size_in_bytes, episode_status, duration, played_up_to, playing_status, podcast_id, added_date, auto_download_status, starred, thumbnail_status, archived, last_playback_interaction_sync_status, exclude_from_episode_limit, deselected_chapters, slug, has_generated_transcript) VALUES ('episode-1', '', 0, 'Test Ep', 0, 0, 0.0, 0.0, 0, 'podcast-1', 0, 0, 0, 0, 0, 0, 0, '', '', 0)"
    private val insertFolder =
        "INSERT INTO folders (uuid, name, color, added_date, sort_position, podcasts_sort_type, deleted, sync_modified, clean_name) VALUES ('folder-1', 'Test Folder', 0, 0, 0, 0, 0, 0, '')"
    private val insertPlaylist =
        "INSERT INTO playlists (uuid, title, iconId, sortId, manual, deleted, syncStatus, autoDownload, autoDownloadLimit, unplayed, partiallyPlayed, finished, downloaded, notDownloaded, audioVideo, filterHours, starred, allPodcasts, filterDuration, longerThan, shorterThan, showArchivedEpisodes, clean_title) VALUES ('playlist-1', 'Test Filter', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '')"
    private val insertUpNext =
        "INSERT INTO up_next_episodes (episodeUuid, position, title) VALUES ('episode-1', 0, 'Test Ep')"

    private fun populate(db: SupportSQLiteDatabase) {
        db.execSQL(insertPodcast)
        db.execSQL(insertEpisode)
        db.execSQL(insertFolder)
        db.execSQL(insertPlaylist)
        db.execSQL(insertUpNext)
    }

    private fun countRows(db: SupportSQLiteDatabase, table: String): Int = db.query("SELECT count(*) FROM $table").use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    private fun productionRoom(dbName: String, reporter: DatabaseCorruptionReporter): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .openHelperFactory(CorruptionHandlingOpenHelperFactory(FrameworkSQLiteOpenHelperFactory()) { reporter })
        .also { builder -> AppDatabase.addMigrations(builder, context) }
        .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
        .build()

    @Test
    fun migration133ToCurrentPreservesPopulatedLibrary() {
        val dbName = "pcdroid687-migration"
        migrationTestHelper.createDatabase(dbName, 133).use { populate(it) }

        val db = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            137,
            true,
            AppDatabase.MIGRATION_133_134,
            AppDatabase.MIGRATION_134_135,
            AppDatabase.MIGRATION_135_136,
            AppDatabase.MIGRATION_136_137,
        )

        assertEquals("podcasts should survive the migration", 1, countRows(db, "podcasts"))
        assertEquals("podcast_episodes should survive the migration", 1, countRows(db, "podcast_episodes"))
        assertEquals("folders should survive the migration", 1, countRows(db, "folders"))
        assertEquals("playlists should survive the migration", 1, countRows(db, "playlists"))
        assertEquals("up_next_episodes should survive the migration", 1, countRows(db, "up_next_episodes"))
    }

    @Test
    fun corruptDatabaseIsBackedUpAndReportedBeforeRecreation() {
        val dbName = "pcdroid687-corrupt"

        migrationTestHelper.createDatabase(dbName, 133).use { populate(it) }

        val dbFile = context.getDatabasePath(dbName)
        context.getDatabasePath("$dbName-wal").delete()
        context.getDatabasePath("$dbName-shm").delete()
        RandomAccessFile(dbFile, "rw").use { raf ->
            val length = raf.length().coerceAtMost(8192L).toInt()
            raf.seek(0)
            raf.write(ByteArray(length) { 0xFF.toByte() })
        }

        val reporter = RecordingReporter()
        var podcasts = -1
        repeat(2) {
            if (podcasts >= 0) return@repeat
            val room = productionRoom(dbName, reporter)
            try {
                podcasts = countRows(room.openHelper.writableDatabase, "podcasts")
            } catch (_: Throwable) {
            } finally {
                room.close()
            }
        }

        assertEquals("corruption should be reported exactly once", 1, reporter.calls)
        assertNotNull("the corrupt database should be preserved", reporter.backupPath)
        assertTrue("the backup file should exist on disk", File(reporter.backupPath!!).exists())
        assertTrue("the corrupt database size should be captured", reporter.databaseSizeBytes > 0)
        assertEquals("Room still recovers by recreating an empty database", 0, podcasts)
    }
}
