package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.db.helper.QueryHelper
import au.com.shiftyjelly.pocketcasts.models.db.helper.UuidCount
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
abstract class EpisodeDao {

    @RawQuery(observedEntities = [PodcastEpisode::class, Podcast::class])
    abstract fun findEpisodes(query: SupportSQLiteQuery): List<PodcastEpisode>

    @RawQuery(observedEntities = [PodcastEpisode::class, Podcast::class])
    abstract fun observeEpisodes(query: SupportSQLiteQuery): Flowable<List<PodcastEpisode>>

    @RawQuery(observedEntities = [PodcastEpisode::class, Podcast::class])
    abstract fun observeCount(query: SupportSQLiteQuery): Flowable<Int>

    @Query("SELECT * FROM podcast_episodes WHERE uuid = :uuid")
    abstract fun findByUuidSync(uuid: String): PodcastEpisode?

    @Query("SELECT * FROM podcast_episodes WHERE uuid = :uuid")
    abstract suspend fun findByUuid(uuid: String): PodcastEpisode?

    @Query("SELECT * FROM podcast_episodes WHERE uuid = :uuid")
    abstract fun findByUuidRx(uuid: String): Maybe<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE uuid = :uuid")
    abstract fun observeByUuid(uuid: String): Flow<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE download_task_id IS NOT NULL")
    abstract fun observeDownloadingEpisodes(): LiveData<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE download_task_id IS NOT NULL")
    abstract fun observeDownloadingEpisodesRx(): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE uuid IN (:uuids)")
    abstract fun findByUuids(uuids: List<String>): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE UPPER(title) = UPPER(:query) LIMIT 1")
    abstract suspend fun findFirstBySearchQuery(query: String): PodcastEpisode?

    @Query("SELECT * FROM podcast_episodes WHERE last_playback_interaction_sync_status <> 1 AND last_playback_interaction_date IS NOT NULL ORDER BY last_playback_interaction_date DESC LIMIT 1000")
    abstract fun findEpisodesForHistorySync(): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes")
    abstract fun findAll(): DataSource.Factory<Int, PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE playing_status = :episodePlayingStatus AND podcast_id = :podcastUuid")
    abstract fun findByEpisodePlayingStatus(podcastUuid: String, episodePlayingStatus: EpisodePlayingStatus): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE playing_status = :episodePlayingStatus AND archived = :archived AND podcast_id = :podcastUuid")
    abstract fun findByEpisodePlayingAndArchiveStatus(podcastUuid: String, episodePlayingStatus: EpisodePlayingStatus, archived: Boolean): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY UPPER(title) ASC")
    abstract fun findByPodcastOrderTitleAsc(podcastUuid: String): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY UPPER(title) DESC")
    abstract fun findByPodcastOrderTitleDesc(podcastUuid: String): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date ASC")
    abstract fun findByPodcastOrderPublishedDateAsc(podcastUuid: String): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC")
    abstract fun findByPodcastOrderPublishedDateDesc(podcastUuid: String): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid AND playing_status != 2 AND archived = 0 ORDER BY published_date DESC LIMIT 1")
    abstract fun findLatestUnfinishedEpisodeByPodcast(podcastUuid: String): PodcastEpisode?

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY duration ASC")
    abstract fun findByPodcastOrderDurationAsc(podcastUuid: String): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY duration DESC")
    abstract fun findByPodcastOrderDurationDesc(podcastUuid: String): List<PodcastEpisode>

    // Find new episodes to display in notifications.
    @Query(
        """SELECT podcast_episodes.*
        FROM podcast_episodes
        JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid
        WHERE podcasts.subscribed = 1 AND podcasts.show_notifications = 1
        AND (podcasts.added_date IS NULL OR (podcasts.added_date IS NOT NULL AND podcasts.added_date < :date AND podcasts.added_date != podcast_episodes.added_date))
        AND podcast_episodes.archived = 0 AND podcast_episodes.playing_status = :playingStatus AND podcast_episodes.added_date >= :date
        ORDER BY podcast_episodes.added_date DESC, podcast_episodes.published_date DESC LIMIT 100"""
    )
    abstract fun findNotificationEpisodes(date: Date, playingStatus: Int = EpisodePlayingStatus.NOT_PLAYED.ordinal): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY UPPER(title) ASC")
    abstract fun observeByPodcastOrderTitleAsc(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY UPPER(title) DESC")
    abstract fun observeByPodcastOrderTitleDesc(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date ASC")
    abstract fun observeByPodcastOrderPublishedDateAsc(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC")
    abstract fun observeByPodcastOrderPublishedDateDesc(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY duration ASC")
    abstract fun observeByPodcastOrderDurationAsc(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY duration DESC")
    abstract fun observeByPodcastOrderDurationDesc(podcastUuid: String): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid AND playing_status != :playingStatus")
    abstract fun findByPodcastAndNotPlayingStatus(podcastUuid: String, playingStatus: Int): List<PodcastEpisode>

    @Query("UPDATE podcast_episodes SET downloaded_error_details = NULL, episode_status = :episodeStatusNotDownloaded WHERE episode_status = :episodeStatusFailed")
    abstract fun clearAllDownloadErrors(episodeStatusNotDownloaded: EpisodeStatusEnum, episodeStatusFailed: EpisodeStatusEnum)

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC, added_date DESC LIMIT 1")
    abstract fun findLatest(podcastUuid: String): PodcastEpisode?

    @Query("SELECT * FROM podcast_episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC, added_date DESC LIMIT 1")
    abstract fun findLatestRx(podcastUuid: String): Maybe<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE (download_task_id IS NOT NULL OR episode_status == :downloadEpisodeStatusEnum OR (episode_status == :failedEpisodeStatusEnum AND last_download_attempt_date > :failedDownloadCutoff AND archived == 0)) ORDER BY last_download_attempt_date DESC")
    abstract fun observeDownloadingEpisodesIncludingFailed(failedDownloadCutoff: Long, failedEpisodeStatusEnum: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOAD_FAILED, downloadEpisodeStatusEnum: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOADED): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE (download_task_id IS NOT NULL AND episode_status == :notDownloaded)")
    abstract suspend fun findStaleDownloads(notDownloaded: EpisodeStatusEnum = EpisodeStatusEnum.NOT_DOWNLOADED): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE episode_status == :downloadEpisodeStatusEnum ORDER BY last_download_attempt_date DESC")
    abstract fun observeDownloadedEpisodes(downloadEpisodeStatusEnum: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOADED): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE starred = 1")
    abstract fun observeStarredEpisodes(): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE starred = 1")
    abstract suspend fun findStarredEpisodes(): List<PodcastEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE last_playback_interaction_date IS NOT NULL AND last_playback_interaction_date > 0 ORDER BY last_playback_interaction_date DESC LIMIT 1000")
    abstract fun observePlaybackHistory(): Flowable<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE last_playback_interaction_date IS NOT NULL AND last_playback_interaction_date > 0 ORDER BY last_playback_interaction_date DESC LIMIT 1000")
    abstract suspend fun findPlaybackHistoryEpisodes(): List<PodcastEpisode>

    @Update
    abstract fun update(episode: PodcastEpisode)

    @Delete
    abstract fun delete(episode: PodcastEpisode)

    @Delete
    abstract fun deleteAll(episode: List<PodcastEpisode>)

    @Query("DELETE FROM podcast_episodes")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM podcast_episodes WHERE uuid = :uuid")
    abstract fun deleteByUuid(uuid: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(episode: PodcastEpisode)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertAll(episodes: List<PodcastEpisode>)

    @Query("UPDATE podcast_episodes SET file_type = :fileType WHERE uuid = :uuid")
    abstract fun updateFileType(fileType: String, uuid: String)

    @Query("UPDATE podcast_episodes SET size_in_bytes = :sizeInBytes WHERE uuid = :uuid")
    abstract fun updateSizeInBytes(sizeInBytes: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET download_url = :url WHERE uuid = :uuid")
    abstract fun updateDownloadUrl(url: String, uuid: String)

    @Query("UPDATE podcast_episodes SET download_task_id = :taskId WHERE uuid = :uuid")
    abstract fun updateDownloadTaskId(uuid: String, taskId: String?)

    @Query("UPDATE podcast_episodes SET last_download_attempt_date = :lastDownloadAttemptDate WHERE uuid = :uuid")
    abstract fun updateLastDownloadAttemptDate(lastDownloadAttemptDate: Date, uuid: String)

    @Query("UPDATE podcast_episodes SET downloaded_error_details = :errorMessage, episode_status = :episodeStatus WHERE uuid = :uuid")
    abstract fun updateDownloadError(uuid: String, errorMessage: String?, episodeStatus: EpisodeStatusEnum)

    @Query("UPDATE podcast_episodes SET downloaded_file_path = :downloadedFilePath WHERE uuid = :uuid")
    abstract fun updateDownloadedFilePath(downloadedFilePath: String, uuid: String)

    @Query("UPDATE podcast_episodes SET auto_download_status = :autoDownloadStatus WHERE uuid = :uuid")
    abstract suspend fun updateAutoDownloadStatus(autoDownloadStatus: Int, uuid: String)

    @Query("UPDATE podcast_episodes SET thumbnail_status = :thumbnailStatus WHERE uuid = :uuid")
    abstract fun updateThumbnailStatus(thumbnailStatus: Int, uuid: String)

    @Query("UPDATE podcast_episodes SET play_error_details = :playErrorDetails WHERE uuid = :uuid")
    abstract fun updatePlayErrorDetails(playErrorDetails: String?, uuid: String)

    @Query("UPDATE podcast_episodes SET downloaded_error_details = :downloadErrorDetails WHERE uuid = :uuid")
    abstract fun updateDownloadErrorDetails(downloadErrorDetails: String?, uuid: String)

    @Query("UPDATE podcast_episodes SET episode_status = :episodeStatus WHERE uuid = :uuid")
    abstract suspend fun updateEpisodeStatus(episodeStatus: EpisodeStatusEnum, uuid: String)

    @Query("UPDATE podcast_episodes SET episode_status = :episodeStatus")
    abstract fun updateAllEpisodeStatus(episodeStatus: EpisodeStatusEnum)

    @Query("UPDATE podcast_episodes SET last_playback_interaction_date = 0 WHERE last_playback_interaction_date <= :lastCleared")
    abstract fun clearEpisodePlaybackInteractionDatesBefore(lastCleared: Date)

    @Query("UPDATE podcast_episodes SET last_playback_interaction_date = 0, last_playback_interaction_sync_status = 1")
    abstract suspend fun clearAllEpisodePlaybackInteractions()

    @Query("UPDATE podcast_episodes SET last_playback_interaction_sync_status = 1")
    abstract fun markPlaybackHistorySynced()

    @Query("SELECT COUNT(*) FROM podcast_episodes")
    abstract suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM podcast_episodes WHERE uuid = :uuid")
    abstract fun countByUuid(uuid: String): Int

    fun exists(uuid: String): Boolean {
        return countByUuid(uuid) != 0
    }

    fun existsRx(uuid: String): Single<Boolean> {
        return Single.fromCallable { exists(uuid) }
    }

    @Query("UPDATE podcast_episodes SET starred_modified = NULL, archived_modified = NULL, duration_modified = NULL, played_up_to_modified = NULL, playing_status_modified = NULL WHERE uuid IN (:episodeUuids)")
    abstract fun markAllSynced(episodeUuids: List<String>)

    @Query("SELECT podcasts.uuid AS uuid, count(podcast_episodes.uuid) AS count FROM podcast_episodes, podcasts WHERE podcast_episodes.podcast_id = podcasts.uuid AND (podcast_episodes.playing_status = :playingStatusNotPlayed OR podcast_episodes.playing_status = :playingStatusInProgress) AND podcast_episodes.archived = 0 GROUP BY podcasts.uuid")
    protected abstract fun podcastToUnfinishedEpisodeCount(playingStatusNotPlayed: Int = EpisodePlayingStatus.NOT_PLAYED.ordinal, playingStatusInProgress: Int = EpisodePlayingStatus.IN_PROGRESS.ordinal): Flowable<List<UuidCount>>

    @Query("SELECT podcasts.uuid AS uuid, count(podcast_episodes.uuid) AS count FROM podcast_episodes, podcasts WHERE podcasts.latest_episode_uuid = podcast_episodes.uuid AND podcast_episodes.playing_status = :playingStatusNotPlayed AND podcast_episodes.archived = 0 GROUP BY podcasts.uuid")
    protected abstract fun podcastToLatestEpisodeCount(playingStatusNotPlayed: Int = EpisodePlayingStatus.NOT_PLAYED.ordinal): Flowable<List<UuidCount>>

    fun podcastUuidToUnfinishedEpisodeCount(): Flowable<Map<String, Int>> {
        return podcastToUnfinishedEpisodeCount().map { it.associateBy({ it.uuid }, { it.count }) }
    }

    fun podcastUuidToLatestEpisodeCount(): Flowable<Map<String, Int>> {
        return podcastToLatestEpisodeCount().map { it.associateBy({ it.uuid }, { it.count }) }
    }

    @Query("SELECT podcast_episodes.* FROM podcast_episodes JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid WHERE podcasts.subscribed = 1 AND podcast_episodes.playing_status != 2 AND podcast_episodes.archived = 0 ORDER BY podcast_episodes.published_date DESC LIMIT 1")
    abstract fun findLatestEpisodeToPlay(): PodcastEpisode?

    @Query("UPDATE podcast_episodes SET starred = :starred, starred_modified = :modified WHERE uuid = :uuid")
    abstract suspend fun updateStarred(starred: Boolean, modified: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET starred = :starred, starred_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun updateAllStarred(episodesUUIDs: List<String>, starred: Boolean, modified: Long)

    @Query("UPDATE podcast_episodes SET archived = 0, archived_modified = :modified, last_archive_interaction_date = :modified, exclude_from_episode_limit = 1 WHERE uuid = :uuid")
    abstract fun unarchive(uuid: String, modified: Long)

    @Query("UPDATE podcast_episodes SET archived = :archived, archived_modified = :modified, last_archive_interaction_date = :modified WHERE uuid = :uuid")
    abstract fun updateArchived(archived: Boolean, modified: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET archived = :archived, last_archive_interaction_date = :modified WHERE uuid = :uuid")
    abstract fun updateArchivedNoSync(archived: Boolean, modified: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET playing_status = :playingStatus, playing_status_modified = :modified WHERE uuid = :uuid")
    abstract fun updatePlayingStatus(playingStatus: EpisodePlayingStatus, modified: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET last_playback_interaction_date = :modified, last_playback_interaction_sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updatePlaybackInteractionDate(uuid: String, modified: Long)

    @Query("UPDATE podcast_episodes SET duration = :duration, duration_modified = :modified WHERE uuid = :uuid")
    abstract fun updateDuration(duration: Double, modified: Long, uuid: String)

    @Query("UPDATE podcast_episodes SET duration = :duration WHERE uuid = :uuid")
    abstract fun updateDurationNoSync(duration: Double, uuid: String)

    @Query("UPDATE podcast_episodes SET played_up_to = :playedUpTo, played_up_to_modified = :modified WHERE uuid = :uuid AND (played_up_to IS NULL OR played_up_to < :playedUpToMin OR played_up_to > :playedUpToMax)")
    abstract fun updatePlayedUpToIfChanged(playedUpTo: Double, playedUpToMin: Double, playedUpToMax: Double, modified: Long, uuid: String)

    fun countWhere(queryAfterWhere: String, appDatabase: AppDatabase): Int {
        val result = QueryHelper.firstRowArray("SELECT count(*) FROM podcast_episodes JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid WHERE podcasts.subscribed = 1 AND $queryAfterWhere", null, appDatabase) ?: return 0
        val firstResult = result[0] ?: return 0
        return Integer.parseInt(firstResult)
    }

    @Query("SELECT * FROM podcast_episodes WHERE (playing_status_modified IS NOT NULL OR played_up_to_modified IS NOT NULL OR duration_modified IS NOT NULL OR archived_modified IS NOT NULL OR starred_modified IS NOT NULL) AND uuid IS NOT NULL LIMIT 2000")
    abstract fun findEpisodesToSync(): List<PodcastEpisode>

    @Query("SELECT podcast_episodes.* FROM podcasts, podcast_episodes WHERE podcast_episodes.podcast_id = podcasts.uuid AND podcast_episodes.podcast_id = :podcastUuid AND podcasts.subscribed = 1 AND podcast_episodes.archived = 0 AND (podcast_episodes.added_date < :inactiveTime AND (CASE WHEN podcast_episodes.last_playback_interaction_date IS NULL THEN 0 ELSE podcast_episodes.last_playback_interaction_date END) < :inactiveTime AND (CASE WHEN podcast_episodes.last_download_attempt_date IS NULL THEN 0 ELSE podcast_episodes.last_download_attempt_date END) < :inactiveDate AND (CASE WHEN podcast_episodes.last_archive_interaction_date IS NULL THEN 0 ELSE podcast_episodes.last_archive_interaction_date END) < :inactiveTime )")
    abstract fun findInactiveEpisodes(podcastUuid: String, inactiveDate: Date, inactiveTime: Long = inactiveDate.time): List<PodcastEpisode>

    @Query("UPDATE podcast_episodes SET archived = 1, archived_modified = :modified, last_archive_interaction_date = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun archiveAllInList(episodesUUIDs: List<String>, modified: Long)

    @Query("UPDATE podcast_episodes SET archived = 0, archived_modified = :modified, last_archive_interaction_date = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract fun unarchiveAllInList(episodesUUIDs: List<String>, modified: Long)

    @Query("UPDATE podcast_episodes SET playing_status = :playingStatus, playing_status_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun updateAllPlayingStatus(episodesUUIDs: List<String>, modified: Long, playingStatus: EpisodePlayingStatus)

    @Query("UPDATE podcast_episodes SET playing_status = :playingStatus, playing_status_modified = :modified, played_up_to = 0, played_up_to_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun markAllUnplayed(episodesUUIDs: List<String>, modified: Long, playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED)

    @Query("SELECT SUM(played_up_to) FROM podcast_episodes WHERE last_playback_interaction_date IS NOT NULL AND last_playback_interaction_date > :fromEpochMs AND last_playback_interaction_date < :toEpochMs")
    abstract suspend fun calculateListeningTime(fromEpochMs: Long, toEpochMs: Long): Long?

    @Query(
        """
        SELECT DISTINCT podcast_episodes.uuid as episodeId, COUNT(DISTINCT podcast_id) as numberOfPodcasts, SUM(played_up_to) as totalPlayedTime, 
        SUBSTR(TRIM(podcasts.podcast_category),1,INSTR(trim(podcasts.podcast_category)||char(10),char(10))-1) as category,
        podcasts.uuid as mostListenedPodcastId, podcasts.primary_color as mostListenedPodcastTintColor
        FROM podcast_episodes
        JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid
        WHERE podcast_episodes.last_playback_interaction_date IS NOT NULL AND podcast_episodes.last_playback_interaction_date > :fromEpochMs AND podcast_episodes.last_playback_interaction_date < :toEpochMs
        AND category IS NOT NULL and category != ''
        GROUP BY category
        ORDER BY totalPlayedTime DESC
        """
    )
    abstract suspend fun findListenedCategories(fromEpochMs: Long, toEpochMs: Long): List<ListenedCategory>

    @Query(
        """
        SELECT COUNT(DISTINCT podcast_episodes.uuid) as numberOfEpisodes, COUNT(DISTINCT podcasts.uuid) as numberOfPodcasts
        FROM podcast_episodes
        JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid
        WHERE podcast_episodes.last_playback_interaction_date IS NOT NULL AND podcast_episodes.last_playback_interaction_date > :fromEpochMs AND podcast_episodes.last_playback_interaction_date < :toEpochMs
        """
    )
    abstract suspend fun findListenedNumbers(fromEpochMs: Long, toEpochMs: Long): ListenedNumbers

    @Query(
        """
        SELECT podcast_episodes.uuid, podcast_episodes.title, podcast_episodes.duration, podcasts.uuid as podcastUuid, podcasts.title as podcastTitle, podcasts.primary_color as tintColorForLightBg, podcasts.secondary_color as tintColorForDarkBg
        FROM podcast_episodes
        JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid
        WHERE podcast_episodes.last_playback_interaction_date IS NOT NULL AND podcast_episodes.last_playback_interaction_date > :fromEpochMs AND podcast_episodes.last_playback_interaction_date < :toEpochMs
        ORDER BY podcast_episodes.played_up_to DESC
        LIMIT 1
        """
    )
    abstract suspend fun findLongestPlayedEpisode(fromEpochMs: Long, toEpochMs: Long): LongestEpisode?

    @Query(
        """
        SELECT COUNT(DISTINCT uuid) 
        FROM podcast_episodes
        WHERE played_up_to > :playedUpToInSecs 
        AND podcast_episodes.last_playback_interaction_date IS NOT NULL AND podcast_episodes.last_playback_interaction_date > :fromEpochMs AND podcast_episodes.last_playback_interaction_date < :toEpochMs
        """
    )
    abstract suspend fun countEpisodesPlayedUpto(fromEpochMs: Long, toEpochMs: Long, playedUpToInSecs: Long): Int

    @Query(
        """
        SELECT * 
        FROM podcast_episodes
        WHERE podcast_episodes.last_playback_interaction_date IS NOT NULL AND podcast_episodes.last_playback_interaction_date < :fromEpochMs AND podcast_episodes.last_playback_interaction_date != 0
        LIMIT 1
        """
    )
    abstract suspend fun findEpisodeInteractedBefore(fromEpochMs: Long): PodcastEpisode?

    @Query(
        """
        SELECT COUNT(*) 
        FROM podcast_episodes
        WHERE podcast_episodes.last_playback_interaction_date IS NOT NULL AND podcast_episodes.last_playback_interaction_date > :fromEpochMs AND podcast_episodes.last_playback_interaction_date < :toEpochMs
        """
    )
    abstract suspend fun findEpisodesCountInListeningHistory(fromEpochMs: Long, toEpochMs: Long): Int
}
