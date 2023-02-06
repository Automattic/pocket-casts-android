package au.com.shiftyjelly.pocketcasts.models.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    companion object {
        private const val TEST_DB = "migration-test"
    }

    @Rule @JvmField
    val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private var dataOpenHelper: OldDataOpenHelper? = null

    @Before
    fun setUp() {
        // Test migrations from old version of the database
        dataOpenHelper = OldDataOpenHelper(InstrumentationRegistry.getInstrumentation().targetContext, TEST_DB).apply {
            writableDatabase.use { database ->
                dropAllTables(database)
                dropAllIndexes(database)
                onCreate(database)
            }
        }
    }

    @After
    fun tearDown() {
        // Clear the database after every test
        dataOpenHelper?.writableDatabase?.use { database ->
            dropAllTables(database)
            dropAllIndexes(database)
        }
    }

    @Test
    fun migrateToRoom() {
        insertTestData(database = dataOpenHelper)

        // Upgrade to the Room schema
        val db = migrationTestHelper.runMigrationsAndValidate(TEST_DB, 48, true, AppDatabase.MIGRATION_45_46, AppDatabase.MIGRATION_46_47, AppDatabase.MIGRATION_47_48)

        // Test the data has been migrated
        assertEquals("Podcasts not copied", 3, countRows(db, "podcasts"))
        assertEquals("Episodes not copied", 2, countRows(db, "episodes"))
        assertEquals("Up next Episodes not copied", 1, countRows(db, "up_next_episodes"))
        assertEquals("Filters not copied", 4, countRows(db, "filters"))
        assertEquals("Filter Episodes not copied", 1, countRows(db, "filter_episodes"))

        val migratedDatabase = getMigratedRoomDatabase()

        val podcastDao = migratedDatabase.podcastDao()
        val podcast = podcastDao.findByUuid("c33338e0-ea44-0134-ec45-4114446340cb")
        assertNotNull("Podcast should be found", podcast)
        assertEquals("MaxFun", podcast?.title)
        assertNotNull(podcast?.addedDate)
        assertEquals("http://static.pocketcasts.com/thumb.jpg", podcast?.thumbnailUrl)
        assertEquals("Fancy description", podcast?.podcastDescription)
        assertEquals("Tech", podcast?.podcastCategory)
        assertEquals("England", podcast?.podcastLanguage)
        assertEquals("audio/mp3", podcast?.mediaType)
        assertEquals("http://www.podcasturl.com", podcast?.podcastUrl)
        assertEquals("4cc23c90-247b-0135-52f8-452518e2d253", podcast?.latestEpisodeUuid)
        assertEquals("Tom", podcast?.author)
        assertEquals(2, podcast?.sortPosition)
        assertEquals(EpisodesSortType.EPISODES_SORT_BY_DATE_DESC, podcast?.episodesSortType)
        assertNotNull(podcast?.latestEpisodeDate)
        assertEquals(true, podcast?.overrideGlobalSettings)
        assertEquals(123, podcast?.startFromSecs)
        assertEquals(1.2, podcast?.playbackSpeed)
        assertEquals(true, podcast?.isSilenceRemoved)
        assertEquals(true, podcast?.isVolumeBoosted)
        assertEquals(true, podcast?.isSubscribed)
        assertEquals(true, podcast?.isShowNotifications)
        assertEquals(2, podcast?.autoDownloadStatus)
        assertEquals(1, podcast?.autoAddToUpNext)
        assertEquals(0xF00000, podcast?.backgroundColor)
        assertEquals(0xFF0000, podcast?.tintColorForLightBg)
        assertEquals(0xFFF000, podcast?.tintColorForDarkBg)
        assertEquals(0xFFFF00, podcast?.fabColorForDarkBg)
        assertEquals(0xFFFFF0, podcast?.fabColorForLightBg)
        assertEquals(0xFFFFFF, podcast?.linkColorForLightBg)
        assertEquals(0xFF00FF, podcast?.linkColorForDarkBg)
        assertEquals(51, podcast?.colorVersion)
        assertEquals(100L, podcast?.colorLastDownloaded)
        assertEquals(1, podcast?.syncStatus)
    }

    private fun getMigratedRoomDatabase(): AppDatabase {
        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java, TEST_DB
        )
            .addMigrations(
                AppDatabase.MIGRATION_45_46,
                AppDatabase.MIGRATION_46_47,
                AppDatabase.MIGRATION_47_48,
                AppDatabase.MIGRATION_48_49,
                AppDatabase.MIGRATION_49_50,
                AppDatabase.MIGRATION_50_51,
                AppDatabase.MIGRATION_51_52,
                AppDatabase.MIGRATION_52_53,
                AppDatabase.MIGRATION_53_54,
                AppDatabase.MIGRATION_54_55,
                AppDatabase.MIGRATION_55_56,
                AppDatabase.MIGRATION_56_57,
                AppDatabase.MIGRATION_57_58,
                AppDatabase.MIGRATION_58_59,
                AppDatabase.MIGRATION_59_60,
                AppDatabase.MIGRATION_60_61,
                AppDatabase.MIGRATION_61_62,
                AppDatabase.MIGRATION_62_63,
                AppDatabase.MIGRATION_63_64,
                AppDatabase.MIGRATION_64_65,
                AppDatabase.MIGRATION_65_66,
                AppDatabase.MIGRATION_66_67,
                AppDatabase.MIGRATION_67_68,
                AppDatabase.MIGRATION_68_69,
                AppDatabase.MIGRATION_69_70,
                AppDatabase.MIGRATION_70_71,
                AppDatabase.MIGRATION_71_72,
                AppDatabase.MIGRATION_72_73,
                AppDatabase.MIGRATION_73_74
            )
            .build()
        // close the database and release any stream resources when the test finishes
        migrationTestHelper.closeWhenFinished(database)
        return database
    }

    private fun countRows(db: SupportSQLiteDatabase?, tableName: String): Int {
        return db?.query("select count(*) from $tableName", null).use { cursor ->
            cursor?.let {
                it.moveToFirst()
                return@use it.getInt(0)
            }
        } ?: 0
    }

    private fun insertTestData(database: OldDataOpenHelper?) {
        database?.writableDatabase?.use {
            it.execSQL("INSERT INTO podcast (title) VALUES ('No UUID!');")
            it.execSQL("INSERT INTO podcast (uuid) VALUES ('e7a6f7d0-02f2-0133-1c51-059c869cc4eb');")
            it.execSQL(
                "INSERT INTO podcast (uuid, added_date, title, thumbnail_url, podcast_url, podcast_description, podcast_category, podcast_language, media_type, latest_episode_uuid, author, sort_order, episodes_sort_order, latest_episode_date, episodes_to_keep, override_global_settings, start_from, playback_speed, silence_removed, volume_boosted, is_folder, subscribed, show_notifications, auto_download_status, auto_add_to_up_next, most_popular_color, primary_color, secondary_color, light_overlay_color, fab_for_light_bg, link_for_dark_bg, link_for_light_bg, color_version, color_last_downloaded, sync_status) VALUES ('c33338e0-ea44-0134-ec45-4114446340cb', '2018-06-06', 'MaxFun', 'http://static.pocketcasts.com/thumb.jpg', 'http://www.podcasturl.com', 'Fancy description', 'Tech', 'England', 'audio/mp3', '4cc23c90-247b-0135-52f8-452518e2d253', 'Tom', 2, 3, '2018-05-03', 1, 1, 123, 1.2, 1, 1, 1, 1, 1, 2, 1, ${0xF00000}, ${0xFF0000}, ${0xFFF000}, ${0xFFFF00}, ${0xFFFFF0}, ${0xFFFFFF}, ${0xFF00FF}, 51, 100, 1);"
            )

            it.execSQL("INSERT INTO episode (uuid) VALUES (NULL);")
            it.execSQL("INSERT INTO episode (uuid) VALUES ('4cc23c90-247b-0135-52f8-452518e2d253');")

            it.execSQL("INSERT INTO player_episodes (episodeUuid) VALUES (NULL);")
            it.execSQL("INSERT INTO playlists (uuid) VALUES (NULL);")
            it.execSQL("INSERT INTO playlist_episodes (episodeUuid) VALUES (NULL);")
        }
    }

    private fun dropAllTables(db: SQLiteDatabase) {
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
            val tables = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }

            for (table in tables) {
                if (table.startsWith("sqlite_")) {
                    continue
                }
                db.execSQL("DROP TABLE IF EXISTS $table")
            }
        }
    }

    private fun dropAllIndexes(db: SQLiteDatabase) {
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='index'", null).use { cursor ->
            val tables = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }

            for (table in tables) {
                if (table.startsWith("sqlite_")) {
                    continue
                }
                db.execSQL("DROP INDEX IF EXISTS $table")
            }
        }
    }
}
