package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
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

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY LOWER(title) ASC")
    abstract fun findSubscribed(): List<Podcast>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY LOWER(title) ASC")
    abstract fun findSubscribedFlow(): Flow<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY LOWER(title) ASC")
    abstract fun findSubscribedRx(): Single<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1")
    abstract suspend fun findSubscribedNoOrder(): List<Podcast>

    @Query("SELECT uuid FROM podcasts WHERE subscribed = 1")
    abstract suspend fun findSubscribedUuids(): List<String>

    @Query("SELECT * FROM podcasts WHERE subscribed = 0")
    abstract fun findUnsubscribed(): List<Podcast>

    @Query("SELECT podcasts.uuid FROM podcasts WHERE subscribed = 0")
    abstract fun observeUnsubscribedUuid(): Flowable<List<String>>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1")
    abstract fun observeSubscribed(): Flowable<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND folder_uuid = :folderUuid ORDER BY CASE WHEN LOWER(SUBSTR(title,1,4)) = 'the ' THEN LOWER(SUBSTR(title,5)) ELSE LOWER(title) END ASC")
    abstract fun observeFolderOrderByNameAsc(folderUuid: String): Flowable<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND folder_uuid = :folderUuid ORDER BY CASE WHEN LOWER(SUBSTR(title,1,4)) = 'the ' THEN LOWER(SUBSTR(title,5)) ELSE LOWER(title) END DESC")
    abstract fun observeFolderOrderByNameDesc(folderUuid: String): Flowable<List<Podcast>>

    fun observeFolderOrderByName(folderUuid: String, orderAsc: Boolean): Flowable<List<Podcast>> {
        return if (orderAsc) observeFolderOrderByNameAsc(folderUuid = folderUuid) else observeFolderOrderByNameDesc(folderUuid = folderUuid)
    }

    @Query("SELECT * FROM podcasts WHERE auto_download_status = 1 AND subscribed = 1")
    abstract fun findPodcastsAutodownload(): List<Podcast>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY added_date ASC")
    abstract fun observeSubscribedOrderByAddedDateAsc(): Flowable<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY added_date DESC")
    abstract fun observeSubscribedOrderByAddedDateDesc(): Flowable<List<Podcast>>

    fun observeSubscribedOrderByAddedDate(orderAsc: Boolean): Flowable<List<Podcast>> {
        return if (orderAsc) observeSubscribedOrderByAddedDateAsc() else observeSubscribedOrderByAddedDateDesc()
    }

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND folder_uuid = :folderUuid ORDER BY added_date ASC")
    abstract fun observeFolderOrderByAddedDateAsc(folderUuid: String): Flowable<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND folder_uuid = :folderUuid ORDER BY added_date DESC")
    abstract fun observeFolderOrderByAddedDateDesc(folderUuid: String): Flowable<List<Podcast>>

    fun observeFolderOrderByAddedDate(folderUuid: String, orderAsc: Boolean): Flowable<List<Podcast>> {
        return if (orderAsc) observeFolderOrderByAddedDateAsc(folderUuid = folderUuid) else observeFolderOrderByAddedDateDesc(folderUuid = folderUuid)
    }

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND auto_add_to_up_next > 0 ORDER BY LOWER(title) ASC")
    abstract fun observeAutoAddToUpNextPodcasts(): Flowable<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE auto_add_to_up_next > 0")
    abstract suspend fun findAutoAddToUpNextPodcasts(): List<Podcast>

    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.archived = 0 AND podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date ASC, podcasts.latest_episode_date ASC")
    abstract fun observeSubscribedOrderByLatestEpisodeAsc(): Flowable<List<Podcast>>

    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.archived = 0 AND podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date DESC, podcasts.latest_episode_date DESC")
    abstract fun observeSubscribedOrderByLatestEpisodeDesc(): Flowable<List<Podcast>>

    fun observeSubscribedOrderByLatestEpisode(orderAsc: Boolean): Flowable<List<Podcast>> {
        return if (orderAsc) observeSubscribedOrderByLatestEpisodeAsc() else observeSubscribedOrderByLatestEpisodeDesc()
    }

    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.archived = 0 AND podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 AND folder_uuid = :folderUuid ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date ASC, podcasts.latest_episode_date ASC")
    abstract fun observeFolderOrderByLatestEpisodeAsc(folderUuid: String): Flowable<List<Podcast>>

    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.archived = 0 AND podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 AND folder_uuid = :folderUuid ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date DESC, podcasts.latest_episode_date DESC")
    abstract fun observeFolderOrderByLatestEpisodeDesc(folderUuid: String): Flowable<List<Podcast>>

    fun observeFolderOrderByLatestEpisode(folderUuid: String, orderAsc: Boolean): Flowable<List<Podcast>> {
        return if (orderAsc) observeFolderOrderByLatestEpisodeAsc(folderUuid = folderUuid) else observeFolderOrderByLatestEpisodeDesc(folderUuid = folderUuid)
    }

    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date ASC, podcasts.latest_episode_date ASC")
    abstract suspend fun findSubscribedOrderByLatestEpisodeAsc(): List<Podcast>

    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date DESC, podcasts.latest_episode_date DESC")
    abstract suspend fun findSubscribedOrderByLatestEpisodeDesc(): List<Podcast>

    suspend fun findSubscribedOrderByLatestEpisode(orderAsc: Boolean): List<Podcast> {
        return if (orderAsc) findSubscribedOrderByLatestEpisodeAsc() else findSubscribedOrderByLatestEpisodeDesc()
    }

    @Query("SELECT podcasts.* FROM podcasts LEFT JOIN podcast_episodes ON podcasts.uuid = podcast_episodes.podcast_id AND podcast_episodes.uuid = (SELECT podcast_episodes.uuid FROM podcast_episodes WHERE podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.playing_status != 2 ORDER BY podcast_episodes.published_date DESC LIMIT 1) WHERE podcasts.subscribed = 1 AND folder_uuid = :folderUuid ORDER BY CASE WHEN podcast_episodes.published_date IS NULL THEN 1 ELSE 0 END, podcast_episodes.published_date DESC, podcasts.latest_episode_date DESC")
    abstract suspend fun findFolderPodcastsOrderByLatestEpisode(folderUuid: String): List<Podcast>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY sort_order ASC")
    abstract fun observeSubscribedOrderByUserSort(): Flowable<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 AND folder_uuid = :folderUuid ORDER BY sort_order ASC")
    abstract fun observeFolderOrderByUserSort(folderUuid: String): Flowable<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE uuid = :uuid")
    abstract fun findByUuid(uuid: String): Podcast?

    @Query("SELECT * FROM podcasts WHERE uuid = :uuid")
    abstract suspend fun findPodcastByUuidSuspend(uuid: String): Podcast?

    @Query("SELECT * FROM podcasts WHERE uuid = :uuid")
    abstract fun observeByUuid(uuid: String): Flowable<Podcast>

    @Query("SELECT * FROM podcasts WHERE uuid = :uuid")
    abstract fun observeByUuidFlow(uuid: String): Flow<Podcast>

    @Query("SELECT * FROM podcasts WHERE uuid = :uuid")
    abstract fun findByUuidRx(uuid: String): Maybe<Podcast>

    @Query("SELECT * FROM podcasts WHERE uuid IN (:uuids)")
    abstract fun findByUuids(uuids: Array<String>): List<Podcast>

    @Query("SELECT * FROM podcasts WHERE folder_uuid = :folderUuid")
    abstract suspend fun findPodcastsInFolder(folderUuid: String): List<Podcast>

    @Query("SELECT * FROM podcasts WHERE folder_uuid = :folderUuid")
    abstract fun findPodcastsInFolderSingle(folderUuid: String): Single<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE folder_uuid IS NULL")
    abstract suspend fun findPodcastsNotInFolder(): List<Podcast>

    @Query("SELECT * FROM podcasts WHERE UPPER(title) LIKE UPPER(:title)")
    abstract fun searchByTitle(title: String): Podcast?

    @Query("UPDATE podcasts SET sync_status = :syncStatus WHERE uuid = :uuid")
    abstract fun updateSyncStatus(syncStatus: Int, uuid: String)

    @Query("UPDATE podcasts SET show_archived = :showArchived, show_archived_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateShowArchived(uuid: String, showArchived: Boolean, modified: Date = Date())

    @Query("UPDATE podcasts SET show_archived = :showArchived, show_archived_modified = :modified, sync_status = 0")
    abstract suspend fun updateAllShowArchived(showArchived: Boolean, modified: Date = Date())

    @Query("UPDATE podcasts SET folder_uuid = :folderUuid, sync_status = 0 WHERE uuid IN (:podcastUuids)")
    abstract suspend fun updateFolderUuid(folderUuid: String?, podcastUuids: List<String>)

    fun updateSyncStatusRx(syncStatus: Int, uuid: String): Completable {
        return Completable.fromAction { updateSyncStatus(syncStatus, uuid) }
    }

    @Query("UPDATE podcasts SET sync_status = :syncStatus")
    abstract fun updateAllSyncStatus(syncStatus: Int)

    @Query("UPDATE podcasts SET sync_status = :syncStatus WHERE subscribed = 1")
    abstract suspend fun updateAllSubscribedSyncStatus(syncStatus: Int)

    @Query("UPDATE podcasts SET sort_order = :sortPosition, sync_status = :syncStatus WHERE uuid = :uuid")
    abstract suspend fun updateSortPosition(sortPosition: Int, uuid: String, syncStatus: Int = Podcast.SYNC_STATUS_NOT_SYNCED)

    @Transaction
    open suspend fun updateSortPositions(podcasts: List<Podcast>) {
        for (podcast in podcasts) {
            updateSortPosition(sortPosition = podcast.sortPosition, uuid = podcast.uuid)
        }
    }

    @Update
    abstract fun update(podcast: Podcast)

    fun updateRx(podcast: Podcast): Completable {
        return Completable.fromCallable { update(podcast) }
    }

    @Query("DELETE FROM podcasts WHERE uuid = :uuid")
    abstract fun deleteByUuid(uuid: String)

    @Delete
    abstract fun delete(podcast: Podcast)

    @Query("DELETE FROM podcasts")
    abstract suspend fun deleteAll()

    @Insert(onConflict = REPLACE)
    abstract fun insert(podcast: Podcast): Long

    fun insertRx(podcast: Podcast): Single<Podcast> {
        return Single.fromCallable {
            insert(podcast)
            podcast
        }
    }

    @Query("SELECT COUNT(*) FROM podcasts")
    abstract fun count(): Int

    @Query("SELECT COUNT(*) FROM podcasts WHERE uuid = :uuid")
    abstract fun countByUuid(uuid: String): Int

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1")
    abstract suspend fun countSubscribed(): Int

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1 AND uuid = :uuid")
    abstract fun countSubscribedByUuid(uuid: String): Int

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1")
    abstract fun countSubscribedRx(): Single<Int>

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1")
    abstract fun observeCountSubscribed(): Flowable<Int>

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1 AND show_notifications = 1")
    abstract fun countNotificationsOn(): Int

    @Query("SELECT COUNT(*) FROM podcasts WHERE subscribed = 1 AND auto_download_status = :downloadStatus")
    abstract fun countDownloadStatus(downloadStatus: Int): Int

    fun exists(uuid: String): Boolean {
        return countByUuid(uuid) != 0
    }

    fun existsRx(uuid: String): Single<Boolean> {
        return Single.fromCallable { exists(uuid) }
    }

    fun isSubscribedToPodcast(uuid: String): Boolean {
        return countSubscribedByUuid(uuid) != 0
    }

    fun isSubscribedToPodcastRx(uuid: String): Single<Boolean> {
        return Single.fromCallable { isSubscribedToPodcast(uuid) }
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
    abstract fun updateExcludeFromAutoArchive(excludeFromAutoArchive: Boolean, uuid: String)

    @Query("UPDATE podcasts SET override_global_effects = :override, override_global_effects_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updateOverrideGlobalEffects(override: Boolean, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET trim_silence_level = :trimMode, trim_silence_level_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updateTrimSilenceMode(trimMode: TrimMode, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET volume_boosted = :volumeBoosted, volume_boosted_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updateVolumeBoosted(volumeBoosted: Boolean, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET playback_speed = :speed, playback_speed_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updatePlaybackSpeed(speed: Double, uuid: String, modified: Date = Date())

    @Transaction
    open fun updateEffects(speed: Double, volumeBoosted: Boolean, trimMode: TrimMode, uuid: String, modified: Date = Date()) {
        updatePlaybackSpeed(speed, uuid, modified)
        updateVolumeBoosted(volumeBoosted, uuid, modified)
        updateTrimSilenceMode(trimMode, uuid, modified)
    }

    @Query("UPDATE podcasts SET episodes_sort_order = :episodesSortType WHERE uuid = :uuid")
    abstract fun updateEpisodesSortType(episodesSortType: EpisodesSortType, uuid: String)

    @Query("UPDATE podcasts SET show_notifications = :show, show_notifications_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updateShowNotifications(show: Boolean, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET subscribed = :subscribed WHERE uuid = :uuid")
    abstract fun updateSubscribed(subscribed: Boolean, uuid: String)

    @Query("UPDATE podcasts SET refresh_available = :refreshAvailable WHERE uuid = :uuid")
    abstract suspend fun updateRefreshAvailable(refreshAvailable: Boolean, uuid: String)

    fun updateSubscribedRx(subscribed: Boolean, uuid: String): Completable {
        return Completable.fromAction { updateSubscribed(subscribed, uuid) }
    }

    @Query("UPDATE podcasts SET start_from = :autoStartFrom, start_from_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateStartFrom(autoStartFrom: Int, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET skip_last = :skipLast, skip_last_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateSkipLast(skipLast: Int, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET color_last_downloaded = :lastDownloaded WHERE uuid = :uuid")
    abstract fun updateColorLastDownloaded(lastDownloaded: Long, uuid: String)

    @Query("UPDATE podcasts SET override_global_settings = :override WHERE uuid = :uuid")
    abstract fun updateOverrideGobalSettings(override: Boolean, uuid: String)

    @Query("UPDATE podcasts SET episodes_to_keep = :episodeToKeep WHERE uuid = :uuid")
    abstract fun updateEpisodesToKeep(episodeToKeep: Int, uuid: String)

    @Query("UPDATE podcasts SET most_popular_color = :background, primary_color = :tintForLightBg, secondary_color = :tintForDarkBg, fab_for_light_bg = :fabForLightBg, light_overlay_color = :fabForDarkBg, link_for_light_bg = :linkForLightBg, link_for_dark_bg = :linkForDarkBg, color_last_downloaded = :colorLastDownloaded WHERE uuid = :uuid")
    abstract fun updateColors(background: Int, tintForLightBg: Int, tintForDarkBg: Int, fabForLightBg: Int, fabForDarkBg: Int, linkForLightBg: Int, linkForDarkBg: Int, colorLastDownloaded: Long, uuid: String)

    @Query("UPDATE podcasts SET latest_episode_uuid = :episodeUuid, latest_episode_date = :publishedDate WHERE uuid = :podcastUuid")
    abstract fun updateLatestEpisode(episodeUuid: String, publishedDate: Date, podcastUuid: String)

    fun updateLatestEpisodeRx(episodeUuid: String, publishedDate: Date, podcastUuid: String): Completable {
        return Completable.fromAction { updateLatestEpisode(episodeUuid, publishedDate, podcastUuid) }
    }

    @Query("UPDATE podcasts SET auto_download_status = :autoDownloadStatus")
    abstract fun updateAllAutoDownloadStatus(autoDownloadStatus: Int)

    @Query("UPDATE podcasts SET show_notifications = :showNotifications, show_notifications_modified = :modified, sync_status = 0")
    abstract suspend fun updateAllShowNotifications(showNotifications: Boolean, modified: Date = Date())

    @Query("UPDATE podcasts SET auto_download_status = :autoDownloadStatus WHERE uuid = :uuid")
    abstract fun updateAutoDownloadStatus(autoDownloadStatus: Int, uuid: String)

    @Query("SELECT * FROM podcasts WHERE sync_status = " + Podcast.SYNC_STATUS_NOT_SYNCED + " AND uuid IS NOT NULL")
    abstract fun findNotSynced(): List<Podcast>

    @Query("SELECT COUNT(*) FROM podcast_episodes WHERE podcast_id = :podcastUuid AND episode_status = :episodeStatus")
    abstract fun countEpisodesInPodcastWithStatus(podcastUuid: String, episodeStatus: EpisodeStatusEnum): Int

    @Query("UPDATE podcasts SET grouping = :grouping, grouping_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract fun updateGrouping(grouping: PodcastGrouping, uuid: String, modified: Date = Date())

    @Query("UPDATE podcasts SET grouping = :grouping, grouping_modified = :modified, sync_status = 0 WHERE subscribed = 1")
    abstract fun updatePodcastGroupingForAll(grouping: PodcastGrouping, modified: Date = Date())

    @Query(
        """
         SELECT DISTINCT podcast_episodes.uuid as episodeId, podcasts.uuid, podcasts.title, podcasts.author, podcasts.primary_color as tintColorForLightBg, podcasts.secondary_color as tintColorForDarkBg, SUM(podcast_episodes.played_up_to) as totalPlayedTime, COUNT(podcast_episodes.uuid) as numberOfPlayedEpisodes
            FROM podcast_episodes
            JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid
            WHERE podcast_episodes.last_playback_interaction_date IS NOT NULL AND podcast_episodes.last_playback_interaction_date > :fromEpochMs AND podcast_episodes.last_playback_interaction_date < :toEpochMs
            GROUP BY podcast_id
            ORDER BY totalPlayedTime DESC, numberOfPlayedEpisodes DESC
            LIMIT :limit
        """,
    )
    abstract suspend fun findTopPodcasts(fromEpochMs: Long, toEpochMs: Long, limit: Int): List<TopPodcast>

    @Query("SELECT * FROM podcasts ORDER BY random() LIMIT :limit")
    abstract suspend fun findRandomPodcasts(limit: Int): List<Podcast>

    @Query("UPDATE podcasts SET override_global_archive = :enable, auto_archive_played_after = :afterPlaying, auto_archive_inactive_after = :inactive, override_global_archive_modified = :modified, auto_archive_played_after_modified = :modified, auto_archive_inactive_after_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateArchiveSettings(uuid: String, enable: Boolean, afterPlaying: AutoArchiveAfterPlaying, inactive: AutoArchiveInactive, modified: Date = Date())

    @Query("UPDATE podcasts SET auto_archive_played_after = :value, auto_archive_played_after_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateArchiveAfterPlaying(uuid: String, value: AutoArchiveAfterPlaying, modified: Date = Date())

    @Query("UPDATE podcasts SET auto_archive_inactive_after = :value, auto_archive_inactive_after_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateArchiveAfterInactive(uuid: String, value: AutoArchiveInactive, modified: Date = Date())

    @Query("UPDATE podcasts SET auto_archive_episode_limit = :value, auto_archive_episode_limit_modified = :modified, sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updateArchiveEpisodeLimit(uuid: String, value: Int?, modified: Date = Date())
}
