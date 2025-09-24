package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.CuratedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.Date
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PodcastDao {

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY LOWER(title) ASC")
    abstract fun findSubscribedBlocking(): List<Podcast>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY LOWER(title) ASC")
    abstract fun findSubscribedFlow(): Flow<List<Podcast>>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY LOWER(title) ASC")
    abstract fun findSubscribedRxSingle(): Single<List<Podcast>>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1")
    abstract suspend fun findSubscribedNoOrder(): List<Podcast>

    @Transaction
    @Query("SELECT uuid FROM podcasts WHERE subscribed = 1")
    abstract suspend fun findSubscribedUuids(): List<String>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 0")
    abstract fun findUnsubscribedBlocking(): List<Podcast>

    @Transaction
    @Query("SELECT podcasts.uuid FROM podcasts WHERE subscribed = 0")
    abstract fun findUnsubscribedUuidRxFlowable(): Flowable<List<String>>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1")
    abstract fun findSubscribedRxFlowable(): Flowable<List<Podcast>>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND folder_uuid = :folderUuid ORDER BY CASE WHEN LOWER(SUBSTR(title,1,4)) = 'the ' THEN LOWER(SUBSTR(title,5)) ELSE LOWER(title) END ASC")
    protected abstract fun observeFolderOrderByNameAsc(folderUuid: String): Flow<List<Podcast>>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND folder_uuid = :folderUuid ORDER BY CASE WHEN LOWER(SUBSTR(title,1,4)) = 'the ' THEN LOWER(SUBSTR(title,5)) ELSE LOWER(title) END DESC")
    protected abstract fun observeFolderOrderByNameDesc(folderUuid: String): Flow<List<Podcast>>

    fun observeFolderOrderByName(folderUuid: String, orderAsc: Boolean): Flow<List<Podcast>> {
        return if (orderAsc) observeFolderOrderByNameAsc(folderUuid = folderUuid) else observeFolderOrderByNameDesc(folderUuid = folderUuid)
    }

    @Transaction
    @Query("SELECT * FROM podcasts WHERE auto_download_status = 1 AND subscribed = 1")
    abstract fun findPodcastsAutoDownloadBlocking(): List<Podcast>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND folder_uuid = :folderUuid ORDER BY added_date ASC")
    protected abstract fun observeFolderOrderByAddedDateAsc(folderUuid: String): Flow<List<Podcast>>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND folder_uuid = :folderUuid ORDER BY added_date DESC")
    protected abstract fun observeFolderOrderByAddedDateDesc(folderUuid: String): Flow<List<Podcast>>

    fun observeFolderOrderByAddedDate(folderUuid: String, orderAsc: Boolean): Flow<List<Podcast>> {
        return if (orderAsc) observeFolderOrderByAddedDateAsc(folderUuid = folderUuid) else observeFolderOrderByAddedDateDesc(folderUuid = folderUuid)
    }

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND auto_add_to_up_next > 0 ORDER BY LOWER(title) ASC")
    abstract fun findAutoAddToUpNextPodcastsRxFlowable(): Flowable<List<Podcast>>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE auto_add_to_up_next > 0")
    abstract suspend fun findAutoAddToUpNextPodcasts(): List<Podcast>

    @Transaction
    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.archived = 0 AND podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date ASC, podcasts.latest_episode_date ASC")
    protected abstract fun observeSubscribedOrderByLatestEpisodeAsc(): Flow<List<Podcast>>

    @Transaction
    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.archived = 0 AND podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date DESC, podcasts.latest_episode_date DESC")
    protected abstract fun observeSubscribedOrderByLatestEpisodeDesc(): Flow<List<Podcast>>

    fun observeSubscribedOrderByLatestEpisode(orderAsc: Boolean): Flow<List<Podcast>> {
        return if (orderAsc) observeSubscribedOrderByLatestEpisodeAsc() else observeSubscribedOrderByLatestEpisodeDesc()
    }

    @Query(
        """
        SELECT 
            podcasts.* 
        FROM 
            podcasts 
            LEFT JOIN (
                SELECT 
                    podcast_id,
                    MAX(last_playback_interaction_date) as last_playback_interaction_date
                FROM 
                    podcast_episodes
                GROUP BY 
                    podcast_id
            ) recently_played_episodes ON podcasts.uuid = recently_played_episodes.podcast_id 
        WHERE 
            podcasts.subscribed = 1 
            AND (:folderUuid IS NULL OR podcasts.folder_uuid = :folderUuid)
        ORDER BY 
            CASE WHEN recently_played_episodes.last_playback_interaction_date IS NULL THEN 1 ELSE 0 END,
            recently_played_episodes.last_playback_interaction_date DESC
    """,
    )
    abstract fun observePodcastsOrderByRecentlyPlayedEpisode(folderUuid: String? = null): Flow<List<Podcast>>

    @Transaction
    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.archived = 0 AND podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 AND folder_uuid = :folderUuid ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date ASC, podcasts.latest_episode_date ASC")
    protected abstract fun observeFolderOrderByLatestEpisodeAsc(folderUuid: String): Flow<List<Podcast>>

    @Transaction
    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.archived = 0 AND podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 AND folder_uuid = :folderUuid ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date DESC, podcasts.latest_episode_date DESC")
    protected abstract fun observeFolderOrderByLatestEpisodeDesc(folderUuid: String): Flow<List<Podcast>>

    fun observeFolderOrderByLatestEpisode(folderUuid: String, orderAsc: Boolean): Flow<List<Podcast>> {
        return if (orderAsc) observeFolderOrderByLatestEpisodeAsc(folderUuid = folderUuid) else observeFolderOrderByLatestEpisodeDesc(folderUuid = folderUuid)
    }

    @Transaction
    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date ASC, podcasts.latest_episode_date ASC")
    abstract suspend fun findSubscribedOrderByLatestEpisodeAsc(): List<Podcast>

    @Transaction
    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date DESC, podcasts.latest_episode_date DESC")
    abstract suspend fun findSubscribedOrderByLatestEpisodeDesc(): List<Podcast>

    suspend fun findSubscribedOrderByLatestEpisode(orderAsc: Boolean): List<Podcast> {
        return if (orderAsc) findSubscribedOrderByLatestEpisodeAsc() else findSubscribedOrderByLatestEpisodeDesc()
    }

    @Transaction
    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 AND folder_uuid = :folderUuid ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date DESC, podcasts.latest_episode_date DESC")
    abstract suspend fun findFolderPodcastsOrderByLatestEpisodeBlocking(folderUuid: String): List<Podcast>

    @Transaction
    @Query(
        """
        SELECT 
            podcasts.* 
        FROM 
            podcasts 
            LEFT JOIN (
                SELECT 
                    podcast_id,
                    MAX(last_playback_interaction_date) as last_playback_interaction_date
                FROM 
                    podcast_episodes
                GROUP BY 
                    podcast_id
            ) recently_played_episodes ON podcasts.uuid = recently_played_episodes.podcast_id 
        WHERE 
            podcasts.subscribed = 1 
            AND (:folderUuid IS NULL OR podcasts.folder_uuid = :folderUuid)
        ORDER BY 
            CASE WHEN recently_played_episodes.last_playback_interaction_date IS NULL THEN 1 ELSE 0 END,
            recently_played_episodes.last_playback_interaction_date DESC
    """,
    )
    abstract suspend fun findPodcastsOrderByRecentlyPlayedEpisode(folderUuid: String? = null): List<Podcast>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND folder_uuid = :folderUuid ORDER BY sort_order ASC")
    abstract fun observeFolderOrderByUserSort(folderUuid: String): Flow<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE uuid = :uuid")
    abstract fun findByUuidBlocking(uuid: String): Podcast?

    @Query("SELECT * FROM podcasts WHERE uuid = :uuid")
    abstract suspend fun findPodcastByUuid(uuid: String): Podcast?

    @Query("SELECT * FROM podcasts WHERE uuid = :uuid")
    abstract fun findByUuidRxFlowable(uuid: String): Flowable<Podcast>

    @Query("SELECT * FROM podcasts WHERE uuid = :uuid")
    abstract fun findByUuidFlow(uuid: String): Flow<Podcast>

    @Query("SELECT * FROM podcasts WHERE uuid = :uuid")
    abstract fun findByUuidRxMaybe(uuid: String): Maybe<Podcast>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE uuid IN (:uuids)")
    abstract fun findByUuidsBlocking(uuids: Array<String>): List<Podcast>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE folder_uuid = :folderUuid")
    abstract suspend fun findPodcastsInFolder(folderUuid: String): List<Podcast>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE folder_uuid = :folderUuid")
    abstract fun findPodcastsInFolderRxSingle(folderUuid: String): Single<List<Podcast>>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE folder_uuid IS NULL")
    abstract suspend fun findPodcastsNotInFolder(): List<Podcast>

    @Query("SELECT * FROM podcasts WHERE UPPER(title) LIKE UPPER(:title)")
    abstract fun searchByTitleBlocking(title: String): Podcast?

    @Query("UPDATE podcasts SET sync_status = :syncStatus WHERE uuid = :uuid")
    abstract fun updateSyncStatusBlocking(syncStatus: Int, uuid: String)

    @Query("UPDATE podcasts SET show_archived = :showArchived, show_archived_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateShowArchived(uuid: String, showArchived: Boolean, modified: Date = Date())

    @Query("UPDATE podcasts SET show_archived = :showArchived, show_archived_modified = :modified, sync_status = 0")
    abstract suspend fun updateAllShowArchived(showArchived: Boolean, modified: Date = Date())

    @Query("UPDATE podcasts SET folder_uuid = :folderUuid, sync_status = 0 WHERE uuid IN (:podcastUuids)")
    abstract suspend fun updateFolderUuid(folderUuid: String?, podcastUuids: List<String>)

    fun updateSyncStatusRxCompletable(syncStatus: Int, uuid: String): Completable {
        return Completable.fromAction { updateSyncStatusBlocking(syncStatus, uuid) }
    }

    @Query("UPDATE podcasts SET sync_status = :syncStatus")
    abstract suspend fun updateAllSyncStatus(syncStatus: Int)

    @Query("UPDATE podcasts SET sync_status = :syncStatus WHERE subscribed = 1")
    abstract suspend fun updateAllSubscribedSyncStatus(syncStatus: Int)

    @Query("UPDATE podcasts SET sync_status = :syncStatus WHERE uuid IN (:uuids)")
    protected abstract suspend fun updateAllSyncStatusUnsafe(syncStatus: Int, uuids: Collection<String>)

    @Transaction
    open suspend fun updateAllSyncStatus(syncStatus: Int, uuids: Collection<String>) {
        uuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT - 1).forEach { chunk ->
            updateAllSyncStatusUnsafe(syncStatus, chunk)
        }
    }

    @Query("UPDATE podcasts SET sort_order = :sortPosition, sync_status = :syncStatus WHERE uuid = :uuid")
    abstract suspend fun updateSortPosition(sortPosition: Int, uuid: String, syncStatus: Int = Podcast.SYNC_STATUS_NOT_SYNCED)

    @Transaction
    open suspend fun updateSortPositions(podcasts: List<Podcast>) {
        for (podcast in podcasts) {
            updateSortPosition(sortPosition = podcast.sortPosition, uuid = podcast.uuid)
        }
    }

    @Update
    abstract fun updateBlocking(podcast: Podcast)

    @Update
    abstract suspend fun updateSuspend(podcast: Podcast)

    fun updateRxCompletable(podcast: Podcast): Completable {
        return Completable.fromCallable { updateBlocking(podcast) }
    }

    @Query("DELETE FROM podcasts WHERE uuid = :uuid")
    abstract fun deleteByUuidBlocking(uuid: String)

    @Delete
    abstract fun deleteBlocking(podcast: Podcast)

    @Delete
    abstract suspend fun delete(podcast: Podcast)

    @Query("DELETE FROM podcasts")
    abstract suspend fun deleteAll()

    @Insert(onConflict = REPLACE)
    abstract fun insertBlocking(podcast: Podcast): Long

    @Insert(onConflict = REPLACE)
    abstract suspend fun insertSuspend(podcast: Podcast): Long

    @Insert(onConflict = IGNORE)
    abstract suspend fun insertAllOrIgnore(podcasts: Collection<Podcast>)

    fun insertRxSingle(podcast: Podcast): Single<Podcast> {
        return Single.fromCallable {
            insertBlocking(podcast)
            podcast
        }
    }

    @Query("SELECT COUNT(*) FROM podcasts")
    abstract fun countBlocking(): Int

    @Query("SELECT COUNT(*) FROM podcasts WHERE uuid = :uuid")
    abstract fun countByUuidBlocking(uuid: String): Int

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1")
    abstract suspend fun countSubscribed(): Int

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1 AND uuid = :uuid")
    abstract fun countSubscribedByUuidBlocking(uuid: String): Int

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1")
    abstract fun countSubscribedRxSingle(): Single<Int>

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1")
    abstract fun countSubscribedFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1 AND show_notifications = 1")
    abstract fun countNotificationsOnBlocking(): Int

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1 AND auto_download_status = :downloadStatus")
    abstract fun countDownloadStatusBlocking(downloadStatus: Int): Int

    @Query("SELECT COUNT(*) > 0 FROM podcasts WHERE subscribed = 1 AND auto_download_status = :downloadStatus")
    abstract suspend fun hasEpisodesWithAutoDownloadStatus(downloadStatus: Int): Boolean

    fun existsBlocking(uuid: String): Boolean {
        return countByUuidBlocking(uuid) != 0
    }

    @Query("SELECT COUNT(*) FROM podcast_episodes WHERE podcast_id IS :podcastUuid")
    abstract fun episodeCountFlow(podcastUuid: String): Flow<Int>

    fun isSubscribedToPodcastBlocking(uuid: String): Boolean {
        return countSubscribedByUuidBlocking(uuid) != 0
    }

    fun isSubscribedToPodcastRxSingle(uuid: String): Single<Boolean> {
        return Single.fromCallable { isSubscribedToPodcastBlocking(uuid) }
    }

    @Query("UPDATE podcasts SET auto_add_to_up_next = :autoAddToUpNext, auto_add_to_up_next_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateAutoAddToUpNext(autoAddToUpNext: Podcast.AutoAddUpNext, uuid: String, modified: Date = Date())

    @Transaction
    open suspend fun updateAutoAddToUpNexts(autoAddToUpNext: Podcast.AutoAddUpNext, podcastUuids: List<String>) {
        for (uuid in podcastUuids) {
            updateAutoAddToUpNext(autoAddToUpNext = autoAddToUpNext, uuid = uuid)
        }
    }

    @Query("UPDATE podcasts SET auto_add_to_up_next = :newValue WHERE uuid = :uuid AND auto_add_to_up_next = :onlyIfValue")
    abstract suspend fun updateAutoAddToUpNextIf(uuid: String, newValue: Int, onlyIfValue: Int)

    @Transaction
    open suspend fun updateAutoAddToUpNextsIf(podcastUuids: List<String>, newValue: Int, onlyIfValue: Int) {
        for (uuid in podcastUuids) {
            updateAutoAddToUpNextIf(uuid = uuid, newValue = newValue, onlyIfValue = onlyIfValue)
        }
    }

    @Query("UPDATE podcasts SET exclude_from_auto_archive = :excludeFromAutoArchive WHERE uuid = :uuid")
    abstract fun updateExcludeFromAutoArchiveBlocking(excludeFromAutoArchive: Boolean, uuid: String)

    @Query("UPDATE podcasts SET override_global_effects = :override, override_global_effects_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updateOverrideGlobalEffectsBlocking(override: Boolean, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET trim_silence_level = :trimMode, trim_silence_level_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updateTrimSilenceModeBlocking(trimMode: TrimMode, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET volume_boosted = :volumeBoosted, volume_boosted_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updateVolumeBoostedBlocking(volumeBoosted: Boolean, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET playback_speed = :speed, playback_speed_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updatePlaybackSpeedBlocking(speed: Double, uuid: String, modified: Date = Date())

    @Transaction
    open fun updateEffectsBlocking(speed: Double, volumeBoosted: Boolean, trimMode: TrimMode, uuid: String, modified: Date = Date()) {
        updatePlaybackSpeedBlocking(speed, uuid, modified)
        updateVolumeBoostedBlocking(volumeBoosted, uuid, modified)
        updateTrimSilenceModeBlocking(trimMode, uuid, modified)
    }

    @Query("UPDATE podcasts SET episodes_sort_order = :episodesSortType, episodes_sort_order_modified = :modified, sync_status = 0  WHERE uuid = :uuid")
    abstract fun updateEpisodesSortTypeBlocking(episodesSortType: EpisodesSortType, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET show_notifications = :show, show_notifications_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateShowNotifications(uuid: String, show: Boolean, modified: Date = Date())

    @Query("UPDATE podcasts SET subscribed = :subscribed WHERE uuid = :uuid")
    abstract fun updateSubscribedBlocking(subscribed: Boolean, uuid: String)

    @Query("UPDATE podcasts SET refresh_available = :refreshAvailable WHERE uuid = :uuid")
    abstract suspend fun updateRefreshAvailable(refreshAvailable: Boolean, uuid: String)

    fun updateSubscribedRxCompletable(subscribed: Boolean, uuid: String): Completable {
        return Completable.fromAction { updateSubscribedBlocking(subscribed, uuid) }
    }

    @Query("UPDATE podcasts SET start_from = :autoStartFrom, start_from_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateStartFrom(autoStartFrom: Int, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET skip_last = :skipLast, skip_last_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateSkipLast(skipLast: Int, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET color_last_downloaded = :lastDownloaded WHERE uuid = :uuid")
    abstract fun updateColorLastDownloadedBlocking(lastDownloaded: Long, uuid: String)

    @Query("UPDATE podcasts SET override_global_settings = :override WHERE uuid = :uuid")
    abstract fun updateOverrideGobalSettingsBlocking(override: Boolean, uuid: String)

    @Query("UPDATE podcasts SET episodes_to_keep = :episodeToKeep WHERE uuid = :uuid")
    abstract fun updateEpisodesToKeepBlocking(episodeToKeep: Int, uuid: String)

    @Query("UPDATE podcasts SET most_popular_color = :background, primary_color = :tintForLightBg, secondary_color = :tintForDarkBg, fab_for_light_bg = :fabForLightBg, light_overlay_color = :fabForDarkBg, link_for_light_bg = :linkForLightBg, link_for_dark_bg = :linkForDarkBg, color_last_downloaded = :colorLastDownloaded WHERE uuid = :uuid")
    abstract fun updateColorsBlocking(background: Int, tintForLightBg: Int, tintForDarkBg: Int, fabForLightBg: Int, fabForDarkBg: Int, linkForLightBg: Int, linkForDarkBg: Int, colorLastDownloaded: Long, uuid: String)

    @Query("UPDATE podcasts SET latest_episode_uuid = :episodeUuid, latest_episode_date = :publishedDate WHERE uuid = :podcastUuid")
    abstract fun updateLatestEpisodeBlocking(episodeUuid: String, publishedDate: Date, podcastUuid: String)

    fun updateLatestEpisodeRxCompletable(episodeUuid: String, publishedDate: Date, podcastUuid: String): Completable {
        return Completable.fromAction { updateLatestEpisodeBlocking(episodeUuid, publishedDate, podcastUuid) }
    }

    @Query("UPDATE podcasts SET auto_download_status = :autoDownloadStatus")
    abstract suspend fun updateAllAutoDownloadStatus(autoDownloadStatus: Int)

    @Query("UPDATE podcasts SET show_notifications = :showNotifications, show_notifications_modified = :modified, sync_status = 0")
    abstract suspend fun updateAllShowNotifications(showNotifications: Boolean, modified: Date = Date())

    @Query("UPDATE podcasts SET auto_download_status = :autoDownloadStatus WHERE uuid = :uuid")
    abstract fun updateAutoDownloadStatusBlocking(autoDownloadStatus: Int, uuid: String)

    @Query("UPDATE podcasts SET auto_download_status = :autoDownloadStatus WHERE uuid IN (:uuids)")
    protected abstract suspend fun updateAutoDownloadStatusUnsafe(uuids: Collection<String>, autoDownloadStatus: Int)

    @Transaction
    open suspend fun updateAutoDownloadStatus(uuids: Collection<String>, autoDownloadStatus: Int) {
        uuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT - 1).forEach { chunk ->
            updateAutoDownloadStatusUnsafe(chunk, autoDownloadStatus)
        }
    }

    @Query("SELECT * FROM podcasts WHERE sync_status = " + Podcast.SYNC_STATUS_NOT_SYNCED + " AND uuid IS NOT NULL")
    abstract fun findNotSyncedBlocking(): List<Podcast>

    @Query("SELECT * FROM podcasts WHERE sync_status = " + Podcast.SYNC_STATUS_NOT_SYNCED + " AND uuid IS NOT NULL")
    abstract suspend fun findNotSynced(): List<Podcast>

    @Query("SELECT COUNT(*) FROM podcast_episodes WHERE podcast_id = :podcastUuid AND episode_status = :episodeStatus")
    abstract fun countEpisodesInPodcastWithStatusBlocking(podcastUuid: String, episodeStatus: EpisodeStatusEnum): Int

    @Query("UPDATE podcasts SET grouping = :grouping, grouping_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updateGroupingBlocking(grouping: PodcastGrouping, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET grouping = :grouping, grouping_modified = :modified, sync_status = 0 WHERE subscribed = 1")
    abstract fun updatePodcastGroupingForAllBlocking(grouping: PodcastGrouping, modified: Date = Date())

    @Query("SELECT * FROM podcasts ORDER BY random() LIMIT :limit")
    abstract suspend fun findRandomPodcasts(limit: Int): List<Podcast>

    @Query("UPDATE podcasts SET override_global_archive = :enable, auto_archive_played_after = :afterPlaying, auto_archive_inactive_after = :inactive, override_global_archive_modified = :modified, auto_archive_played_after_modified = :modified, auto_archive_inactive_after_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateArchiveSettings(uuid: String, enable: Boolean, afterPlaying: AutoArchiveAfterPlaying, inactive: AutoArchiveInactive, modified: Date = Date())

    @Query("UPDATE podcasts SET auto_archive_played_after = :value, auto_archive_played_after_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateArchiveAfterPlaying(uuid: String, value: AutoArchiveAfterPlaying, modified: Date = Date())

    @Query("UPDATE podcasts SET auto_archive_inactive_after = :value, auto_archive_inactive_after_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateArchiveAfterInactive(uuid: String, value: AutoArchiveInactive, modified: Date = Date())

    @Query("UPDATE podcasts SET auto_archive_episode_limit = :value, auto_archive_episode_limit_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateArchiveEpisodeLimit(uuid: String, value: AutoArchiveLimit, modified: Date = Date())

    @Query("DELETE FROM curated_podcasts")
    protected abstract suspend fun deleteAllCuratedPodcasts()

    @Insert(onConflict = REPLACE)
    protected abstract suspend fun insertAllCuratedPodcasts(podcasts: List<CuratedPodcast>)

    @Transaction
    open suspend fun replaceAllCuratedPodcasts(podcasts: List<CuratedPodcast>) {
        deleteAllCuratedPodcasts()
        insertAllCuratedPodcasts(podcasts)
    }

    @Query("UPDATE podcasts SET is_header_expanded = :isExpanded WHERE uuid IS :uuid")
    abstract suspend fun updateIsHeaderExpanded(uuid: String, isExpanded: Boolean)
}
