package au.com.shiftyjelly.pocketcasts.models.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import com.squareup.moshi.Moshi
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
        private const val MIGRATION_DB = "migration-test-132-133"
        private const val MIGRATION_DB_133_134 = "migration-test-133-134"
    }

    @Rule @JvmField
    val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    private var dataOpenHelper: DataOpenHelper? = null

    @Before
    fun setUp() {
        // Test migrations from old version of the database
        dataOpenHelper = DataOpenHelper(InstrumentationRegistry.getInstrumentation().targetContext, TEST_DB).apply {
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
        val podcast = podcastDao.findByUuidBlocking("c33338e0-ea44-0134-ec45-4114446340cb")
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
        assertEquals(TrimMode.OFF, podcast?.trimMode)
        assertEquals(true, podcast?.isVolumeBoosted)
        assertEquals(true, podcast?.isSubscribed)
        assertEquals(true, podcast?.isShowNotifications)
        assertEquals(2, podcast?.autoDownloadStatus)
        assertEquals(Podcast.AutoAddUpNext.PLAY_LAST, podcast?.autoAddToUpNext)
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

    @Test
    fun migrate132To133BackfillsOriginAndDropsLegacyColumns() {
        migrationTestHelper.createDatabase(MIGRATION_DB, 132).use { db ->
            db.execSQL(
                "INSERT INTO episode_chapters (chapter_index, episode_uuid, start_time, is_embedded, is_generated) VALUES " +
                    "(0, 'episode-1', 0, 1, 0), " + // embedded -> NativeMedia (3)
                    "(1, 'episode-1', 1000, 0, 1), " + // generated -> Generated (4)
                    "(2, 'episode-1', 2000, 0, 0)", // neither -> Unknown (0)
            )
            // 5000 rows, half embedded, to prove the migration completes at scale
            db.execSQL(
                "INSERT INTO episode_chapters (chapter_index, episode_uuid, start_time, is_embedded, is_generated) " +
                    "WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 5000) " +
                    "SELECT n, 'bulk-episode', n * 1000, n % 2, 0 FROM seq",
            )
        }

        val db = migrationTestHelper.runMigrationsAndValidate(MIGRATION_DB, 133, true, AppDatabase.MIGRATION_132_133)

        assertEquals("All chapters should be preserved", 5003, countRows(db, "episode_chapters"))
        assertEquals("NativeMedia origin", 2501, countWhere(db, "episode_chapters", "origin = 3"))
        assertEquals("Generated origin", 1, countWhere(db, "episode_chapters", "origin = 4"))
        assertEquals("Unknown origin", 2501, countWhere(db, "episode_chapters", "origin = 0"))

        val columns = mutableListOf<String>()
        db.query("PRAGMA table_info(episode_chapters)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        assertEquals("origin column should exist", true, columns.contains("origin"))
        assertEquals("is_embedded column should be dropped", false, columns.contains("is_embedded"))
        assertEquals("is_generated column should be dropped", false, columns.contains("is_generated"))
    }

    @Test
    fun migrate133To134CreatesAlternateEnclosuresTable() {
        migrationTestHelper.createDatabase(MIGRATION_DB_133_134, 133).close()

        val db = migrationTestHelper.runMigrationsAndValidate(MIGRATION_DB_133_134, 134, true, AppDatabase.MIGRATION_133_134)

        val columns = mutableListOf<String>()
        db.query("PRAGMA table_info(episode_alternate_enclosures)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        assertEquals(
            "All enclosure columns should exist",
            true,
            columns.containsAll(
                listOf("_id", "episode_uuid", "position", "type", "bitrate", "length", "height", "width", "lang", "title", "codecs", "integrity_type", "integrity_value", "is_default", "sources"),
            ),
        )

        val indexes = mutableListOf<String>()
        db.query("PRAGMA index_list(episode_alternate_enclosures)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                indexes.add(cursor.getString(nameIndex))
            }
        }
        assertEquals("episode_uuid index should exist", true, indexes.contains("episode_alternate_enclosure_episode_uuid_index"))

        db.execSQL("INSERT INTO episode_alternate_enclosures (episode_uuid, position, type, is_default, sources) VALUES ('episode-1', 0, 'application/x-mpegURL', 1, '[]')")
        assertEquals(1, countRows(db, "episode_alternate_enclosures"))
    }

    private fun countWhere(db: SupportSQLiteDatabase?, tableName: String, where: String): Int {
        return db?.query("SELECT count(*) FROM $tableName WHERE $where").use { cursor ->
            cursor?.let {
                it.moveToFirst()
                return@use it.getInt(0)
            }
        } ?: 0
    }

    private fun getMigratedRoomDatabase(): AppDatabase {
        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB,
        ).addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
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
                AppDatabase.MIGRATION_73_74,
                AppDatabase.MIGRATION_74_75,
                AppDatabase.MIGRATION_75_76,
                AppDatabase.MIGRATION_76_77,
                AppDatabase.MIGRATION_77_78,
                AppDatabase.MIGRATION_78_79,
                AppDatabase.MIGRATION_79_80,
                AppDatabase.MIGRATION_80_81,
                AppDatabase.MIGRATION_82_83,
                AppDatabase.MIGRATION_83_84,
                AppDatabase.MIGRATION_84_85,
                AppDatabase.MIGRATION_85_86,
                AppDatabase.MIGRATION_86_87,
                AppDatabase.MIGRATION_87_88,
                AppDatabase.MIGRATION_89_90,
                AppDatabase.MIGRATION_90_91,
                AppDatabase.MIGRATION_91_92,
                AppDatabase.MIGRATION_92_93,
                AppDatabase.MIGRATION_93_94,
                AppDatabase.MIGRATION_94_95,
                AppDatabase.MIGRATION_95_96,
                AppDatabase.MIGRATION_96_97,
                AppDatabase.MIGRATION_97_98,
                AppDatabase.MIGRATION_98_99,
                AppDatabase.MIGRATION_99_100,
                AppDatabase.MIGRATION_100_101,
                AppDatabase.MIGRATION_101_102,
                // 102 to 103 added via auto migration
                AppDatabase.MIGRATION_103_104,
                AppDatabase.MIGRATION_104_105,
                AppDatabase.MIGRATION_105_106,
                AppDatabase.MIGRATION_106_107,
                AppDatabase.MIGRATION_107_108,
                AppDatabase.MIGRATION_108_109,
                AppDatabase.MIGRATION_109_110,
                AppDatabase.MIGRATION_110_111,
                AppDatabase.MIGRATION_111_112,
                AppDatabase.MIGRATION_112_113,
                AppDatabase.MIGRATION_113_114,
                AppDatabase.MIGRATION_114_115,
                AppDatabase.MIGRATION_115_116,
                AppDatabase.MIGRATION_116_117,
                AppDatabase.MIGRATION_117_118,
                AppDatabase.MIGRATION_118_119,
                AppDatabase.MIGRATION_119_120,
                AppDatabase.MIGRATION_120_121,
                AppDatabase.MIGRATION_121_122,
                AppDatabase.MIGRATION_122_123,
                AppDatabase.MIGRATION_123_124,
                AppDatabase.MIGRATION_124_125,
                AppDatabase.MIGRATION_125_126,
                AppDatabase.MIGRATION_126_127,
                AppDatabase.MIGRATION_127_128,
                AppDatabase.MIGRATION_129_130,
                AppDatabase.MIGRATION_130_131,
                AppDatabase.MIGRATION_131_132,
                AppDatabase.MIGRATION_132_133,
                AppDatabase.MIGRATION_133_134,
                AppDatabase.MIGRATION_134_135,
            )
            .build()
        // close the database and release any stream resources when the test finishes
        migrationTestHelper.closeWhenFinished(database)
        return database
    }

    private fun countRows(db: SupportSQLiteDatabase?, tableName: String): Int {
        return db?.query("select count(*) from $tableName").use { cursor ->
            cursor?.let {
                it.moveToFirst()
                return@use it.getInt(0)
            }
        } ?: 0
    }

    private fun insertTestData(database: DataOpenHelper?) {
        database?.writableDatabase?.use {
            it.execSQL("INSERT INTO podcast (title) VALUES ('No UUID!');")
            it.execSQL("INSERT INTO podcast (uuid) VALUES ('e7a6f7d0-02f2-0133-1c51-059c869cc4eb');")
            it.execSQL(
                "INSERT INTO podcast (uuid, added_date, title, thumbnail_url, podcast_url, podcast_description, podcast_category, podcast_language, media_type, latest_episode_uuid, author, sort_order, episodes_sort_order, latest_episode_date, episodes_to_keep, override_global_settings, start_from, playback_speed, silence_removed, volume_boosted, is_folder, subscribed, show_notifications, auto_download_status, auto_add_to_up_next, most_popular_color, primary_color, secondary_color, light_overlay_color, fab_for_light_bg, link_for_dark_bg, link_for_light_bg, color_version, color_last_downloaded, sync_status) VALUES ('c33338e0-ea44-0134-ec45-4114446340cb', '2018-06-06', 'MaxFun', 'http://static.pocketcasts.com/thumb.jpg', 'http://www.podcasturl.com', 'Fancy description', 'Tech', 'England', 'audio/mp3', '4cc23c90-247b-0135-52f8-452518e2d253', 'Tom', 2, 3, '2018-05-03', 1, 1, 123, 1.2, 1, 1, 1, 1, 1, 2, 1, ${0xF00000}, ${0xFF0000}, ${0xFFF000}, ${0xFFFF00}, ${0xFFFFF0}, ${0xFFFFFF}, ${0xFF00FF}, 51, 100, 1);",
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
