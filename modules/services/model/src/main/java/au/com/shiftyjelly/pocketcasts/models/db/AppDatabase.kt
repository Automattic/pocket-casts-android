package au.com.shiftyjelly.pocketcasts.models.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import au.com.shiftyjelly.pocketcasts.model.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.converter.BundlePaidTypeConverter
import au.com.shiftyjelly.pocketcasts.models.converter.DateTypeConverter
import au.com.shiftyjelly.pocketcasts.models.converter.EpisodePlayingStatusConverter
import au.com.shiftyjelly.pocketcasts.models.converter.EpisodeStatusEnumConverter
import au.com.shiftyjelly.pocketcasts.models.converter.EpisodesSortTypeConverter
import au.com.shiftyjelly.pocketcasts.models.converter.PodcastAutoUpNextConverter
import au.com.shiftyjelly.pocketcasts.models.converter.PodcastLicensingEnumConverter
import au.com.shiftyjelly.pocketcasts.models.converter.PodcastsSortTypeConverter
import au.com.shiftyjelly.pocketcasts.models.converter.SyncStatusConverter
import au.com.shiftyjelly.pocketcasts.models.converter.TrimModeTypeConverter
import au.com.shiftyjelly.pocketcasts.models.converter.UserEpisodeServerStatusConverter
import au.com.shiftyjelly.pocketcasts.models.db.dao.BookmarkDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.BumpStatsDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.FolderDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaylistDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastRatingsDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.SearchHistoryDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextChangeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserEpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.models.entity.SearchHistoryItem
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextChange
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import java.util.Arrays
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Database(
    entities = [
        AnonymousBumpStat::class,
        Bookmark::class,
        PodcastEpisode::class,
        Folder::class,
        Playlist::class,
        PlaylistEpisode::class,
        Podcast::class,
        SearchHistoryItem::class,
        UpNextChange::class,
        UpNextEpisode::class,
        UserEpisode::class,
        PodcastRatings::class
    ],
    version = 77,
    exportSchema = true
)
@TypeConverters(
    AnonymousBumpStat.CustomEventPropsTypeConverter::class,
    BundlePaidTypeConverter::class,
    DateTypeConverter::class,
    EpisodePlayingStatusConverter::class,
    EpisodeStatusEnumConverter::class,
    EpisodesSortTypeConverter::class,
    PodcastAutoUpNextConverter::class,
    PodcastLicensingEnumConverter::class,
    PodcastsSortTypeConverter::class,
    SyncStatusConverter::class,
    TrimModeTypeConverter::class,
    UserEpisodeServerStatusConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun upNextDao(): UpNextDao
    abstract fun upNextChangeDao(): UpNextChangeDao
    abstract fun userEpisodeDao(): UserEpisodeDao
    abstract fun folderDao(): FolderDao
    abstract fun bumpStatsDao(): BumpStatsDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun podcastRatingsDao(): PodcastRatingsDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        // This seems dodgy but I got it from Google, https://github.com/googlesamples/android-sunflower/blob/master/app/src/main/java/com/google/samples/apps/sunflower/data/AppDatabase.kt
        @Volatile private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val databaseBuilder = Room.databaseBuilder(context, AppDatabase::class.java, "pocketcasts")
            AppDatabase.addMigrations(databaseBuilder, context)
            if (BuildConfig.DEBUG) {
                databaseBuilder.fallbackToDestructiveMigration()
            }
            return databaseBuilder.build()
        }

        val MIGRATION_45_46 = addMigration(45, 46) { database ->
            database.execSQL("CREATE TABLE IF NOT EXISTS `podcasts` (`uuid` TEXT NOT NULL, `added_date` INTEGER, `thumbnail_url` TEXT, `title` TEXT NOT NULL, `podcast_url` TEXT, `podcast_description` TEXT NOT NULL, `podcast_category` TEXT NOT NULL, `podcast_language` TEXT NOT NULL, `media_type` TEXT, `latest_episode_uuid` TEXT, `author` TEXT NOT NULL, `sort_order` INTEGER NOT NULL, `episodes_sort_order` INTEGER NOT NULL, `latest_episode_date` INTEGER, `episodes_to_keep` INTEGER NOT NULL, `override_global_settings` INTEGER NOT NULL, `start_from` INTEGER NOT NULL, `playback_speed` REAL NOT NULL, `silence_removed` INTEGER NOT NULL, `volume_boosted` INTEGER NOT NULL, `is_folder` INTEGER NOT NULL, `subscribed` INTEGER NOT NULL, `show_notifications` INTEGER NOT NULL, `auto_download_status` INTEGER NOT NULL, `auto_add_to_up_next` INTEGER NOT NULL, `most_popular_color` INTEGER NOT NULL, `primary_color` INTEGER NOT NULL, `secondary_color` INTEGER NOT NULL, `light_overlay_color` INTEGER NOT NULL, `fab_for_light_bg` INTEGER NOT NULL, `link_for_dark_bg` INTEGER NOT NULL, `link_for_light_bg` INTEGER NOT NULL, `color_version` INTEGER NOT NULL, `color_last_downloaded` INTEGER NOT NULL, `sync_status` INTEGER NOT NULL, PRIMARY KEY(`uuid`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `episodes` (`uuid` TEXT NOT NULL, `episode_description` TEXT NOT NULL, `published_date` INTEGER NOT NULL, `title` TEXT NOT NULL, `size_in_bytes` INTEGER NOT NULL, `episode_status` INTEGER NOT NULL, `file_type` TEXT, `duration` REAL NOT NULL, `download_url` TEXT, `downloaded_file_path` TEXT, `downloaded_error_details` TEXT, `play_error_details` TEXT, `played_up_to` REAL NOT NULL, `playing_status` INTEGER NOT NULL, `podcast_id` TEXT NOT NULL, `added_date` INTEGER NOT NULL, `auto_download_status` INTEGER NOT NULL, `starred` INTEGER NOT NULL, `thumbnail_status` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `last_download_attempt_date` INTEGER, `playing_status_modified` INTEGER, `played_up_to_modified` INTEGER, `duration_modified` INTEGER, `archived_modified` INTEGER, `starred_modified` INTEGER, PRIMARY KEY(`uuid`))")
            database.execSQL("CREATE INDEX IF NOT EXISTS `episode_last_download_attempt_date` ON `episodes` (`last_download_attempt_date`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `episode_podcast_id` ON `episodes` (`podcast_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `episode_published_date` ON `episodes` (`published_date`)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `filters` (`_id` INTEGER, `uuid` TEXT NOT NULL, `title` TEXT NOT NULL, `sortPosition` INTEGER, `manual` INTEGER NOT NULL, `unplayed` INTEGER NOT NULL, `partiallyPlayed` INTEGER NOT NULL, `finished` INTEGER NOT NULL, `audioVideo` INTEGER NOT NULL, `allPodcasts` INTEGER NOT NULL, `podcastUuids` TEXT, `downloaded` INTEGER NOT NULL, `downloading` INTEGER NOT NULL, `notDownloaded` INTEGER NOT NULL, `autoDownload` INTEGER NOT NULL, `autoDownloadWifiOnly` INTEGER NOT NULL, `autoDownloadPowerOnly` INTEGER NOT NULL, `sortId` INTEGER NOT NULL, `iconId` INTEGER NOT NULL, `filterHours` INTEGER NOT NULL, `starred` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `syncStatus` INTEGER NOT NULL, PRIMARY KEY(`_id`))")
            database.execSQL("CREATE INDEX IF NOT EXISTS `filters_uuid` ON `filters` (`uuid`)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `filter_episodes` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `playlistId` INTEGER NOT NULL, `episodeUuid` TEXT NOT NULL, `position` INTEGER NOT NULL)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `filter_episodes_playlist_id` ON `filter_episodes` (`playlistId`)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `up_next_episodes` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `episodeUuid` TEXT NOT NULL, `position` INTEGER NOT NULL, `playlistId` INTEGER, `title` TEXT NOT NULL, `publishedDate` INTEGER, `downloadUrl` TEXT, `podcastUuid` TEXT)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `up_next_changes` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `type` INTEGER NOT NULL, `uuid` TEXT, `uuids` TEXT, `modified` INTEGER NOT NULL)")

            // you can't delete a podcast now
            database.execSQL("UPDATE podcast SET subscribed = CASE WHEN is_deleted = 1 THEN 0 ELSE 1 END;")

            // in case the last migration failed
            database.execSQL("DELETE FROM podcasts")
            database.execSQL("INSERT INTO podcasts (uuid, added_date, thumbnail_url, title, podcast_language, podcast_category, media_type, podcast_description, latest_episode_uuid, latest_episode_date, playback_speed, silence_removed, volume_boosted, sync_status, author, is_folder, subscribed, podcast_url, override_global_settings, episodes_to_keep, start_from, sort_order, episodes_sort_order, primary_color, secondary_color, light_overlay_color, most_popular_color, fab_for_light_bg, link_for_light_bg, link_for_dark_bg, color_version, color_last_downloaded, auto_download_status, show_notifications, auto_add_to_up_next) SELECT IFNULL(uuid, '') uuid, added_date, thumbnail_url, IFNULL(title, '') title, IFNULL(podcast_language, '') podcast_language, IFNULL(podcast_category, '') podcast_category, media_type, IFNULL(podcast_description, '') podcast_description, latest_episode_uuid, latest_episode_date, IFNULL(playback_speed, 1) playback_speed, IFNULL(silence_removed, 0) silence_removed, IFNULL(volume_boosted, 0) volume_boosted, IFNULL(sync_status, 0) sync_status, IFNULL(author, '') author, IFNULL(is_folder, 0) is_folder, IFNULL(subscribed, 0) subscribed, podcast_url, IFNULL(override_global_settings, 0) override_global_settings, IFNULL(episodes_to_keep, 0) episodes_to_keep, IFNULL(start_from, 0) start_from, IFNULL(sort_order, 0) sort_order, IFNULL(episodes_sort_order, 0) episodes_sort_order, IFNULL(primary_color, 0) primary_color, IFNULL(secondary_color, 0) secondary_color, IFNULL(light_overlay_color, 0) light_overlay_color, IFNULL(most_popular_color, 0) most_popular_color, IFNULL(fab_for_light_bg, 0) fab_for_light_bg, IFNULL(link_for_light_bg, 0) link_for_light_bg, IFNULL(link_for_dark_bg, 0) link_for_dark_bg, IFNULL(color_version, 0) color_version, IFNULL(color_last_downloaded, 0) color_last_downloaded, IFNULL(auto_download_status, 0) auto_download_status, IFNULL(show_notifications, 0) show_notifications, IFNULL(auto_add_to_up_next, 0) auto_add_to_up_next FROM podcast")

            database.execSQL("DELETE FROM episodes")
            database.execSQL("INSERT INTO episodes (uuid, episode_description, published_date, title, size_in_bytes, episode_status, file_type, duration, download_url, downloaded_file_path, downloaded_error_details, play_error_details, played_up_to, playing_status, podcast_id, added_date, auto_download_status, starred, thumbnail_status, archived, last_download_attempt_date, playing_status_modified, played_up_to_modified, duration_modified, archived_modified, starred_modified) SELECT IFNULL(uuid, '') uuid, IFNULL(episode_description, '') episode_description, IFNULL(published_date, 0) published_date, IFNULL(title, '') title, IFNULL(size_in_bytes, 0) size_in_bytes, IFNULL(episode_status, 0) episode_status, file_type, IFNULL(duration, 0) duration, download_url, downloaded_file_path, downloaded_error_details, play_error_details, IFNULL(played_up_to, 0) played_up_to, IFNULL(playing_status, 0) playing_status, IFNULL(podcast_id, '')  podcast_id, IFNULL(added_date, 0) added_date, IFNULL(auto_download_status, 0) auto_download_status, IFNULL(starred, 0) starred, IFNULL(thumbnail_status, 0) thumbnail_status, IFNULL(is_deleted, 0) is_deleted, last_download_attempt_date, playing_status_modified, played_up_to_modified, duration_modified, is_deleted_modified, starred_modified FROM episode")

            database.execSQL("DELETE FROM up_next_episodes")
            database.execSQL("INSERT INTO up_next_episodes (episodeUuid, position, playlistId, title, publishedDate, downloadUrl, podcastUuid) SELECT IFNULL(episodeUuid, '') episodeUuid, IFNULL(position, 0) position, playlistId, IFNULL(title, '') title, publishedDate, downloadUrl, podcastUuid FROM player_episodes")

            database.execSQL("DELETE FROM filters")
            database.execSQL("INSERT INTO filters (uuid, title, sortPosition, manual, unplayed, partiallyPlayed, finished, audioVideo, allPodcasts, podcastUuids, downloaded, downloading, notDownloaded, autoDownload, autoDownloadWifiOnly, autoDownloadPowerOnly, sortId, iconId, filterHours, starred, deleted, syncStatus) SELECT IFNULL(uuid, '') uuid, IFNULL(title, '') title, sortPosition, IFNULL(manual, 0) manual, IFNULL(unplayed, 1) unplayed, IFNULL(partiallyPlayed, 1) partiallyPlayed, IFNULL(finished, 0) finished, IFNULL(audioVideo, 0) audioVideo, IFNULL(allPodcasts, 1) allPodcasts, podcastUuids, IFNULL(downloaded, 1) downloaded, IFNULL(downloading, 1) downloading, IFNULL(notDownloaded, 1) notDownloaded, IFNULL(autoDownload, 0) autoDownload, IFNULL(autoDownloadWifiOnly, 0) autoDownloadWifiOnly, IFNULL(autoDownloadPowerOnly, 0) autoDownloadPowerOnly, IFNULL(sortId, 0) sortId, IFNULL(iconId, 0) iconId, IFNULL(filterHours, 0) filterHours, IFNULL(starred, 0) starred, IFNULL(deleted, 0) deleted, IFNULL(syncStatus, 0) syncStatus FROM playlists")

            database.execSQL("DELETE FROM filter_episodes")
            database.execSQL("INSERT INTO filter_episodes (playlistId, episodeUuid, position) SELECT IFNULL(playlistId, 0) playlistId, IFNULL(episodeUuid, '') episodeUuid, IFNULL(position, 0) position FROM playlist_episodes")

            database.execSQL("DROP TABLE IF EXISTS podcast")
            database.execSQL("DROP TABLE IF EXISTS episode")
            database.execSQL("DROP TABLE IF EXISTS player_episodes")
            database.execSQL("DROP TABLE IF EXISTS playlists")
            database.execSQL("DROP TABLE IF EXISTS playlist_episodes")
        }

        val MIGRATION_46_47 = addMigration(46, 47) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("exclude_from_auto_archive")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN exclude_from_auto_archive INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_47_48 = addMigration(47, 48) { database ->
            val episodeColumnNames = getColumnNames(database, "episodes")
            if (!episodeColumnNames.contains("season")) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN season INTEGER DEFAULT NULL")
            }
            if (!episodeColumnNames.contains("number")) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN number INTEGER DEFAULT NULL")
            }
            if (!episodeColumnNames.contains("type")) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN type TEXT DEFAULT NULL")
            }
            if (!episodeColumnNames.contains("cleanTitle")) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN cleanTitle TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_48_49 = addMigration(48, 49) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("override_global_effects")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN override_global_effects INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_49_50 = addMigration(49, 50) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("estimated_next_episode")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN estimated_next_episode INTEGER")
            }
            if (!podcastColumnNames.contains("episode_frequency")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN episode_frequency TEXT")
            }
        }

        val MIGRATION_50_51 = addMigration(50, 51) { database ->
            val episodeColumnNames = getColumnNames(database, "episodes")
            if (!episodeColumnNames.contains("last_playback_interaction_date")) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN last_playback_interaction_date INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_51_52 = addMigration(51, 52) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("override_global_archive")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN override_global_archive INTEGER NOT NULL DEFAULT 0")
            }
            if (!podcastColumnNames.contains("auto_archive_played_after")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN auto_archive_played_after INTEGER NOT NULL DEFAULT 0")
            }
            if (!podcastColumnNames.contains("auto_archive_inactive_after")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN auto_archive_inactive_after INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_52_53 = addMigration(52, 53) { database ->
            val episodeColumnNames = getColumnNames(database, "episodes")
            if (!episodeColumnNames.contains("last_playback_interaction_sync_status")) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN last_playback_interaction_sync_status INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_53_54 = addMigration(53, 54) { database ->
            val playlistColumnNames = getColumnNames(database, "filters")
            if (!playlistColumnNames.contains("autoDownloadLimit")) {
                database.execSQL("ALTER TABLE filters ADD COLUMN autoDownloadLimit INTEGER NOT NULL DEFAULT 10")
            }
        }

        val MIGRATION_54_55 = addMigration(54, 55) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("auto_archive_episode_limit")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN auto_archive_episode_limit INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_55_56 = addMigration(55, 56) { database ->
            val episodeColumnNames = getColumnNames(database, "episodes")
            if (!episodeColumnNames.contains("exclude_from_episode_limit")) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN exclude_from_episode_limit INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_56_57 = addMigration(56, 57) { database ->
            val episodeColumnNames = getColumnNames(database, "episodes")
            if (!episodeColumnNames.contains("download_task_id")) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN download_task_id TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_57_58 = addMigration(57, 58) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("grouping")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN grouping INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_58_59 = addMigration(58, 59) { database ->
            database.execSQL("CREATE TABLE IF NOT EXISTS user_episodes (uuid TEXT PRIMARY KEY NOT NULL, published_date INTEGER NOT NULL, episode_description TEXT NOT NULL, title TEXT NOT NULL, size_in_bytes INTEGER NOT NULL, episode_status INTEGER NOT NULL, file_type TEXT, duration REAL NOT NULL, download_url TEXT, played_up_to REAL NOT NULL, playing_status INTEGER NOT NULL, added_date INTEGER NOT NULL, auto_download_status INTEGER NOT NULL, last_download_attempt_date INTEGER, archived INTEGER NOT NULL, download_task_id TEXT, downloaded_file_path TEXT, playing_status_modified INTEGER, played_up_to_modified INTEGER, artwork_url TEXT, play_error_details TEXT, server_status INTEGER NOT NULL, upload_error_details TEXT, downloaded_error_details TEXT, tint_color_index INTEGER NOT NULL, has_custom_image INTEGER NOT NULL, upload_task_id TEXT)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `user_episode_last_download_attempt_date` ON `user_episodes` (`last_download_attempt_date`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `user_episode_published_date` ON `user_episodes` (`published_date`)")
        }

        val MIGRATION_59_60 = addMigration(59, 60) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("skip_last")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN skip_last INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_60_61 = addMigration(60, 61) { database ->
            val episodeColumnNames = getColumnNames(database, "episodes")
            if (!episodeColumnNames.contains("last_archive_interaction_date")) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN last_archive_interaction_date INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_61_62 = addMigration(61, 62) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("isPaid")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN isPaid INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_62_63 = addMigration(62, 63) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("show_archived")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN show_archived INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_63_64 = addMigration(63, 64) { database ->
            val filterColumnNames = getColumnNames(database, "filters")
            if (!filterColumnNames.contains("filterDuration")) {
                database.execSQL("ALTER TABLE filters ADD COLUMN filterDuration INTEGER NOT NULL DEFAULT 0")
            }
            if (!filterColumnNames.contains("longerThan")) {
                database.execSQL("ALTER TABLE filters ADD COLUMN longerThan INTEGER NOT NULL DEFAULT 20")
            }
            if (!filterColumnNames.contains("shorterThan")) {
                database.execSQL("ALTER TABLE filters ADD COLUMN shorterThan INTEGER NOT NULL DEFAULT 40")
            }
        }

        val MIGRATION_64_65 = addMigration(64, 65) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("licensing")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN licensing INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_65_66 = addMigration(65, 66) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("bundleuuid")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN bundleuuid TEXT")
            }
            if (!podcastColumnNames.contains("bundlebundleUrl")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN bundlebundleUrl TEXT")
            }
            if (!podcastColumnNames.contains("bundlepaymentUrl")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN bundlepaymentUrl TEXT")
            }
            if (!podcastColumnNames.contains("bundledescription")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN bundledescription TEXT")
            }
        }

        val MIGRATION_66_67 = addMigration(66, 67) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("bundlepodcastUuid")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN bundlepodcastUuid TEXT DEFAULT NULL")
            }
            if (!podcastColumnNames.contains("bundlepaidType")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN bundlepaidType TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_67_68 = addMigration(67, 68) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("trim_silence_level")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN trim_silence_level INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_68_69 = addMigration(68, 69) { database ->
            val filterColumnNames = getColumnNames(database, "filters")
            if (!filterColumnNames.contains("draft")) {
                database.execSQL("ALTER TABLE filters ADD COLUMN draft INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_69_70 = addMigration(69, 70) { database ->
            database.execSQL("DELETE FROM filters WHERE manual = 1")
            database.execSQL("DELETE FROM filter_episodes")
        }

        val MIGRATION_70_71 = addMigration(70, 71) { database ->
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("refresh_available")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN refresh_available INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_71_72 = addMigration(71, 72) { database ->
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS folders (
                    uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    color INTEGER NOT NULL,
                    added_date INTEGER NOT NULL,
                    sort_position INTEGER NOT NULL,
                    podcasts_sort_type INTEGER NOT NULL,
                    deleted INTEGER NOT NULL,
                    sync_modified INTEGER NOT NULL,
                    PRIMARY KEY(uuid)
                );
                """.trimIndent()
            )
            val podcastColumnNames = getColumnNames(database, "podcasts")
            if (!podcastColumnNames.contains("folder_uuid")) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN folder_uuid TEXT")
            }
        }

        val MIGRATION_72_73 = addMigration(72, 73) { database ->
            database.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS bump_stats (
                      name TEXT NOT NULL,
                      event_time INTEGER NOT NULL,
                      custom_event_props TEXT NOT NULL,
                      PRIMARY KEY(name, event_time, custom_event_props)
                    );
                """.trimIndent()
            )
        }

        val MIGRATION_73_74 = addMigration(73, 74) { database ->
            database.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS search_history (
                        _id INTEGER PRIMARY KEY AUTOINCREMENT,
                        modified INTEGER NOT NULL,
                        term TEXT,
                        podcast_uuid TEXT,
                        podcast_title TEXT,
                        podcast_author TEXT,
                        folder_uuid TEXT,
                        folder_title TEXT,
                        folder_color INTEGER,
                        folder_podcastIds TEXT,
                        episode_uuid TEXT,
                        episode_title TEXT,
                        episode_duration REAL,
                        episode_podcastUuid TEXT, 
                        episode_podcastTitle TEXT, 
                        episode_artworkUrl TEXT
                    );
                """.trimIndent()
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_term` ON search_history (`term`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_podcast_uuid` ON search_history (`podcast_uuid`);")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_folder_uuid` ON search_history (`folder_uuid`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_episode_uuid` ON search_history (`episode_uuid`)")
        }

        val MIGRATION_74_75 = addMigration(74, 75) { database ->
            database.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS podcast_ratings (
                        podcast_uuid TEXT NOT NULL, 
                        average REAL NOT NULL, 
                        total INTEGER, 
                        PRIMARY KEY(`podcast_uuid`)
                    );
                """.trimIndent()
            )
        }

        val MIGRATION_75_76 = addMigration(75, 76) { database ->
            database.execSQL("ALTER TABLE episodes RENAME TO podcast_episodes")
        }

        val MIGRATION_76_77 = addMigration(76, 77) { database ->
            database.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `bookmarks` (
                        `uuid` TEXT NOT NULL,
                        `podcast_uuid` TEXT NOT NULL,
                        `episode_uuid` TEXT NOT NULL,
                        `time` INTEGER NOT NULL, 
                        `created_at` INTEGER NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `title_modified` INTEGER, 
                        `deleted` INTEGER NOT NULL, 
                        `deleted_modified` INTEGER,
                        `sync_status` INTEGER NOT NULL, 
                        PRIMARY KEY(`uuid`)
                    );
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `bookmarks_podcast_uuid` ON `bookmarks` (`podcast_uuid`)")
        }

        fun addMigrations(databaseBuilder: Builder<AppDatabase>, context: Context) {
            databaseBuilder.addMigrations(
                addMigration(1, 2) { },
                addMigration(2, 3) { },
                addMigration(3, 4) { },
                addMigration(4, 5) { database ->
                    database.execSQL("ALTER TABLE podcast ADD COLUMN episodes_to_keep INTEGER DEFAULT 0")
                    database.execSQL("ALTER TABLE podcast ADD COLUMN override_global_settings BOOLEAN DEFAULT 0")
                },
                addMigration(5, 6) { database ->
                    database.execSQL("ALTER TABLE episode ADD COLUMN added_date INTEGER")
                },
                addMigration(6, 7) { },
                addMigration(7, 8) { },
                addMigration(8, 9) { database ->
                    database.execSQL("CREATE TABLE playlists (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid VARCHAR, title VARCHAR, sortPosition INTEGER, manual INTEGER DEFAULT 0, unplayed INTEGER, partiallyPlayed INTEGER, finished INTEGER, audioVideo INTEGER, allPodcasts INTEGER, podcastUuids VARCHAR, downloaded INTEGER, downloading INTEGER, notDownloaded INTEGER, autoDownload INTEGER, autoDownloadWifiOnly INTEGER, autoDownloadPowerOnly INTEGER, sortId INTEGER, iconId INTEGER, starred INTEGER, deleted INTEGER DEFAULT 0, syncStatus INTEGER DEFAULT 0)")
                    // New Releases
                    database.insert(
                        "playlists",
                        SQLiteDatabase.CONFLICT_IGNORE,
                        contentValuesOf(
                            "uuid" to "2797DCF8-1C93-4999-B52A-D1849736FA2C",
                            "title" to context.getString(LR.string.filters_title_new_releases),
                            "unplayed" to 1,
                            "partiallyPlayed" to 1,
                            "finished" to 0,
                            "audioVideo" to 0,
                            "allPodcasts" to 1,
                            "downloaded" to 1,
                            "downloading" to 1,
                            "notDownloaded" to 1,
                            "autoDownload" to 0,
                            "autoDownloadWifiOnly" to 0,
                            "autoDownloadIncludeHotspots" to 0,
                            "autoDownloadPowerOnly" to 0,
                            "sortId" to 0,
                            "iconId" to 10,
                            "starred" to 0,
                            "syncStatus" to 1,
                            "sortPosition" to 1,
                            "filterHours" to 336
                        )
                    )
                    // In Progress
                    database.insert(
                        "playlists",
                        SQLiteDatabase.CONFLICT_IGNORE,
                        contentValuesOf(
                            "uuid" to "D89A925C-5CE1-41A4-A879-2751838CE5CE",
                            "title" to context.getString(LR.string.filters_title_in_progress),
                            "unplayed" to 0,
                            "partiallyPlayed" to 1,
                            "finished" to 0,
                            "audioVideo" to 0,
                            "allPodcasts" to 1,
                            "downloaded" to 1,
                            "downloading" to 1,
                            "notDownloaded" to 1,
                            "autoDownload" to 0,
                            "autoDownloadWifiOnly" to 0,
                            "autoDownloadIncludeHotspots" to 0,
                            "autoDownloadPowerOnly" to 0,
                            "sortId" to 0,
                            "iconId" to 23,
                            "starred" to 0,
                            "syncStatus" to 1,
                            "sortPosition" to 2,
                            "filterHours" to 0
                        )
                    )
                    // Starred
                    database.insert(
                        "playlists",
                        SQLiteDatabase.CONFLICT_IGNORE,
                        contentValuesOf(
                            "uuid" to "78EC673E-4C3A-4985-9D83-7A79C825A359",
                            "title" to context.getString(LR.string.filters_title_starred),
                            "unplayed" to 1,
                            "partiallyPlayed" to 1,
                            "finished" to 1,
                            "audioVideo" to 0,
                            "allPodcasts" to 1,
                            "downloaded" to 1,
                            "downloading" to 1,
                            "notDownloaded" to 1,
                            "autoDownload" to 0,
                            "autoDownloadWifiOnly" to 0,
                            "autoDownloadIncludeHotspots" to 0,
                            "autoDownloadPowerOnly" to 0,
                            "sortId" to 0,
                            "iconId" to 39,
                            "starred" to 1,
                            "syncStatus" to 1,
                            "sortPosition" to 3,
                            "filterHours" to 0
                        )
                    )
                },
                addMigration(9, 10) { database ->
                    database.execSQL(
                        "CREATE TABLE playlist_episodes (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "playlist_id INTEGER," +
                            "episode_uuid VARCHAR," +
                            "position INTEGER" +
                            ")"
                    )
                    database.execSQL("ALTER TABLE podcast ADD COLUMN is_deleted INTEGER DEFAULT 0")
                    database.execSQL("ALTER TABLE podcast ADD COLUMN sync_status INTEGER DEFAULT 0")
                    database.execSQL("ALTER TABLE podcast ADD COLUMN author VARCHAR")
                    database.execSQL("ALTER TABLE episode ADD COLUMN is_deleted INTEGER DEFAULT 0")
                    database.execSQL("ALTER TABLE episode ADD COLUMN sync_status INTEGER DEFAULT 1")
                    database.execSQL("ALTER TABLE episode ADD COLUMN auto_download_status INTEGER DEFAULT 0")
                    database.execSQL("ALTER TABLE episode ADD COLUMN starred INTEGER DEFAULT 0")
                },
                addMigration(10, 11) { },
                addMigration(11, 12) { database ->
                    database.execSQL("ALTER TABLE podcast ADD COLUMN playback_speed DECIMAL DEFAULT 1")
                },
                addMigration(12, 13) { },
                addMigration(13, 14) { database ->
                    database.execSQL("DROP TABLE IF EXISTS playlist_episodes")
                    database.execSQL("CREATE TABLE playlist_episodes (_id INTEGER PRIMARY KEY AUTOINCREMENT, playlistId INTEGER, episodeUuid VARCHAR, position INTEGER)")
                },
                addMigration(14, 15) { database ->
                    database.execSQL("ALTER TABLE podcast ADD COLUMN is_folder INTEGER DEFAULT 0")
                    database.execSQL("ALTER TABLE podcast ADD COLUMN subscribed INTEGER DEFAULT 1")
                },
                addMigration(15, 16) { database ->
                    database.execSQL("ALTER TABLE episode ADD COLUMN thumbnail_status INTEGER DEFAULT 0")
                },
                addMigration(16, 17) { database ->
                    database.execSQL("UPDATE podcast SET override_global_settings = 0")
                },
                addMigration(17, 18) { database ->
                    database.execSQL("ALTER TABLE podcast ADD COLUMN start_from INTEGER")
                },
                addMigration(18, 19) { database ->
                    database.execSQL("UPDATE podcast SET sync_status = 0 WHERE start_from IS NOT NULL AND start_from > 0")
                },
                addMigration(19, 20) { database ->
                    database.execSQL("CREATE INDEX episode_published_date_idx ON episode(published_date);")
                },
                addMigration(20, 21) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("auto_download_status")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN auto_download_status INTEGER DEFAULT 0")
                    }
                },
                addMigration(21, 22) { database ->
                    database.execSQL("DROP TABLE IF EXISTS player_episodes")
                    database.execSQL("CREATE TABLE player_episodes (_id INTEGER PRIMARY KEY AUTOINCREMENT, episodeUuid VARCHAR, position INTEGER, playlistId INTEGER)")
                },
                addMigration(22, 23) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("primary_color")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN primary_color INTEGER")
                    }
                    if (!columnNames.contains("secondary_color")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN secondary_color INTEGER")
                    }
                    if (!columnNames.contains("detail_color")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN detail_color INTEGER")
                    }
                    if (!columnNames.contains("background_color")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN background_color INTEGER")
                    }
                },
                addMigration(23, 24) { database ->
                    val columnNames = getColumnNames(database, "episode")
                    if (!columnNames.contains("play_error_details")) {
                        database.execSQL("ALTER TABLE episode ADD COLUMN play_error_details VARCHAR")
                    }
                },
                addMigration(24, 25) { database ->
                    database.execSQL("UPDATE episode SET thumbnail_status = 0")
                },
                addMigration(25, 26) { database ->
                    database.execSQL("UPDATE podcast SET primary_color = NULL, secondary_color = NULL")
                },
                addMigration(26, 27) { database ->
                    val columnNames = getColumnNames(database, "episode")
                    if (!columnNames.contains("playing_status_modified")) {
                        database.execSQL("ALTER TABLE episode ADD COLUMN playing_status_modified INTEGER")
                    }
                    if (!columnNames.contains("played_up_to_modified")) {
                        database.execSQL("ALTER TABLE episode ADD COLUMN played_up_to_modified INTEGER")
                    }
                    if (!columnNames.contains("duration_modified")) {
                        database.execSQL("ALTER TABLE episode ADD COLUMN duration_modified INTEGER")
                    }
                    if (!columnNames.contains("is_deleted_modified")) {
                        database.execSQL("ALTER TABLE episode ADD COLUMN is_deleted_modified INTEGER")
                    }
                    if (!columnNames.contains("starred_modified")) {
                        database.execSQL("ALTER TABLE episode ADD COLUMN starred_modified INTEGER")
                    }
                },
                addMigration(27, 28) { database ->
                    val columnNames = getColumnNames(database, "playlists")
                    if (!columnNames.contains("autoDownloadIncludeHotspots")) {
                        database.execSQL("ALTER TABLE playlists ADD COLUMN autoDownloadIncludeHotspots INTEGER")
                    }
                },
                addMigration(28, 29) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("show_notifications")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN show_notifications INTEGER DEFAULT 0")
                    }
                },
                addMigration(29, 30) { database ->
                    database.execSQL("UPDATE podcast SET primary_color = NULL, secondary_color = NULL")
                },
                addMigration(30, 31) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("most_popular_color")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN most_popular_color INTEGER")
                    }
                },
                addMigration(31, 32) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("silence_removed")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN silence_removed INTEGER DEFAULT 0")
                    }
                    if (!columnNames.contains("volume_boosted")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN volume_boosted INTEGER DEFAULT 0")
                    }
                },
                addMigration(32, 33) { database ->
                    database.execSQL("CREATE INDEX episode_podcast_id_idx ON episode(podcast_id);")
                },
                addMigration(33, 34) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("secondary_top_color")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN secondary_top_color INTEGER")
                    }
                },
                addMigration(34, 35) { _ -> },
                addMigration(35, 36) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("dark_background_color")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN dark_background_color INTEGER")
                    }
                    if (!columnNames.contains("light_overlay_color")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN light_overlay_color INTEGER")
                    }
                },
                addMigration(36, 37) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("sort_order")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN sort_order INTEGER")
                    }
                },
                addMigration(37, 38) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("episodes_sort_order")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN episodes_sort_order INTEGER")
                        database.execSQL("UPDATE podcast SET episodes_sort_order = 3")
                    }
                },
                addMigration(38, 39) { database ->
                    val columnNames = getColumnNames(database, "playlists")
                    if (!columnNames.contains("filterHours")) {
                        database.execSQL("ALTER TABLE playlists ADD COLUMN filterHours INTEGER DEFAULT 0")
                    }
                },
                addMigration(39, 40) { database ->
                    var sortPosition = maxColumn(database, "playlists", "sortPosition")
                    sortPosition++
                    if (sortPosition <= 0) {
                        sortPosition = 1
                    }

                    val insert = "INSERT INTO playlists (uuid, title, unplayed, partiallyPlayed, finished, audioVideo, allPodcasts, downloaded, downloading, notDownloaded, autoDownload, autoDownloadWifiOnly, autoDownloadIncludeHotspots, autoDownloadPowerOnly, sortId, iconId, starred, syncStatus, sortPosition, filterHours) VALUES "
                    database.execSQL("$insert('2797DCF8-1C93-4999-B52A-D1849736FA2C', 'New Releases', 1, 1, 0, 0, 1, 1, 1, 1, 0,  0,  0,  0,  0, 10,  0,  1,  $sortPosition,  336);")
                    sortPosition++
                    database.execSQL("$insert('D89A925C-5CE1-41A4-A879-2751838CE5CE', 'In Progress',  0, 1, 0, 0, 1, 1, 1, 1, 0,  0,  0,  0,  0, 23,  0,  1,  $sortPosition,  0);")

                    if (count(database, "playlists", "UPPER(title) = 'STARRED'") == 0) {
                        sortPosition++
                        database.execSQL("$insert('78EC673E-4C3A-4985-9D83-7A79C825A359', 'Starred',      1, 1, 1, 0, 1, 1, 1, 1, 0,  0,  0,  0,  0, 39,  1,  1,  $sortPosition,  0);")
                    }
                },
                addMigration(40, 41) { database ->
                    val columnNames = getColumnNames(database, "player_episodes")
                    if (!columnNames.contains("title")) {
                        database.execSQL("ALTER TABLE player_episodes ADD COLUMN title VARCHAR")
                    }
                    if (!columnNames.contains("publishedDate")) {
                        database.execSQL("ALTER TABLE player_episodes ADD COLUMN publishedDate INTEGER")
                    }
                    if (!columnNames.contains("downloadUrl")) {
                        database.execSQL("ALTER TABLE player_episodes ADD COLUMN downloadUrl VARCHAR")
                    }
                    if (!columnNames.contains("podcastUuid")) {
                        database.execSQL("ALTER TABLE player_episodes ADD COLUMN podcastUuid VARCHAR")
                    }
                },
                addMigration(41, 42) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("fab_for_light_bg")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN fab_for_light_bg INTEGER")
                    }
                    if (!columnNames.contains("link_for_light_bg")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN link_for_light_bg INTEGER")
                    }
                    if (!columnNames.contains("link_for_dark_bg")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN link_for_dark_bg INTEGER")
                    }
                    if (!columnNames.contains("color_version")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN color_version INTEGER DEFAULT 0")
                    }
                    if (!columnNames.contains("color_last_downloaded")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN color_last_downloaded INTEGER")
                    }
                },
                addMigration(42, 43) { database ->
                    val columnNames = getColumnNames(database, "podcast")
                    if (!columnNames.contains("auto_add_to_up_next")) {
                        database.execSQL("ALTER TABLE podcast ADD COLUMN auto_add_to_up_next INTEGER DEFAULT 0")
                    }
                },
                addMigration(43, 44) { database ->
                    val columnNames = getColumnNames(database, "episode")
                    if (!columnNames.contains("last_download_attempt_date")) {
                        database.execSQL("ALTER TABLE episode ADD COLUMN last_download_attempt_date INTEGER DEFAULT 0")
                        database.execSQL("CREATE INDEX episode_last_download_attempt_date_idx ON episode(last_download_attempt_date);")
                    }
                },
                addMigration(44, 45) { database ->
                    val columnNames = getColumnNames(database, "episode")
                    if (!columnNames.contains("show_notes")) {
                        database.execSQL("UPDATE episode SET show_notes = NULL")
                    }
                },
                MIGRATION_45_46,
                MIGRATION_46_47,
                MIGRATION_47_48,
                MIGRATION_48_49,
                MIGRATION_49_50,
                MIGRATION_50_51,
                MIGRATION_51_52,
                MIGRATION_52_53,
                MIGRATION_53_54,
                MIGRATION_54_55,
                MIGRATION_55_56,
                MIGRATION_56_57,
                MIGRATION_57_58,
                MIGRATION_58_59,
                MIGRATION_59_60,
                MIGRATION_60_61,
                MIGRATION_61_62,
                MIGRATION_62_63,
                MIGRATION_63_64,
                MIGRATION_64_65,
                MIGRATION_65_66,
                MIGRATION_66_67,
                MIGRATION_67_68,
                MIGRATION_68_69,
                MIGRATION_69_70,
                MIGRATION_70_71,
                MIGRATION_71_72,
                MIGRATION_72_73,
                MIGRATION_73_74,
                MIGRATION_74_75,
                MIGRATION_75_76,
                MIGRATION_76_77,
            )
        }

        private fun addMigration(startVersion: Int, endVersion: Int, migration: (database: SupportSQLiteDatabase) -> Unit): Migration {
            return object : Migration(startVersion, endVersion) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    migration(database)
                }
            }
        }

        private fun getColumnNames(database: SupportSQLiteDatabase, tableName: String): List<String> {
            val names = mutableListOf<String>()
            database.query("select * from $tableName limit 1").use { cursor ->
                names.addAll(Arrays.asList(*cursor.columnNames))
            }
            return names
        }

        fun count(database: SupportSQLiteDatabase, name: String, where: String): Int {
            var whereSql = ""
            if (where.isNotBlank()) {
                whereSql = " WHERE $where"
            }
            val result = firstRowArray("SELECT count(*) FROM $name$whereSql", arrayOf(), database)
            return Integer.parseInt(result[0])
        }

        private fun maxColumn(database: SupportSQLiteDatabase, table: String, column: String): Int {
            val result = firstRowArray("SELECT MAX($column) FROM $table", arrayOf(), database)
            return if (result.isEmpty()) 0 else Integer.parseInt(result[0])
        }

        private fun firstRowArray(query: String, params: Array<String>?, database: SupportSQLiteDatabase): List<String> {
            val result = mutableListOf<String>()
            database.query(query, params ?: arrayOf()).use { cursor ->
                if (cursor.moveToNext()) {
                    for (i in 0 until cursor.columnCount) {
                        result.add(cursor.getString(i))
                    }
                }
            }
            return result
        }
    }
}
