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
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
abstract class EpisodeDao {

    @RawQuery(observedEntities = [Episode::class, Podcast::class])
    abstract fun findEpisodes(query: SupportSQLiteQuery): List<Episode>

    @RawQuery(observedEntities = [Episode::class, Podcast::class])
    abstract fun observeEpisodes(query: SupportSQLiteQuery): Flowable<List<Episode>>

    @RawQuery(observedEntities = [Episode::class, Podcast::class])
    abstract fun observeCount(query: SupportSQLiteQuery): Flowable<Int>

    @Query("SELECT * FROM episodes WHERE uuid = :uuid")
    abstract fun findByUuid(uuid: String): Episode?

    @Query("SELECT * FROM episodes WHERE uuid = :uuid")
    abstract fun findByUuidRx(uuid: String): Maybe<Episode>

    @Query("SELECT * FROM episodes WHERE uuid = :uuid")
    abstract fun observeByUuid(uuid: String): Flowable<Episode>

    @Query("SELECT * FROM episodes WHERE download_task_id IS NOT NULL")
    abstract fun observeDownloadingEpisodes(): LiveData<List<Episode>>

    @Query("SELECT * FROM episodes WHERE download_task_id IS NOT NULL")
    abstract fun observeDownloadingEpisodesRx(): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE uuid IN (:uuids)")
    abstract fun findByUuids(uuids: List<String>): List<Episode>

    @Query("SELECT * FROM episodes WHERE UPPER(title) = UPPER(:query) LIMIT 1")
    abstract fun findFirstBySearchQuery(query: String): Episode?

    @Query("SELECT * FROM episodes WHERE last_playback_interaction_sync_status <> 1 AND last_playback_interaction_date IS NOT NULL ORDER BY last_playback_interaction_date DESC LIMIT 1000")
    abstract fun findEpisodesForHistorySync(): List<Episode>

    @Query("SELECT * FROM episodes")
    abstract fun findAll(): DataSource.Factory<Int, Episode>

    @Query("SELECT * FROM episodes WHERE playing_status = :episodePlayingStatus AND podcast_id = :podcastUuid")
    abstract fun findByEpisodePlayingStatus(podcastUuid: String, episodePlayingStatus: EpisodePlayingStatus): List<Episode>

    @Query("SELECT * FROM episodes WHERE playing_status = :episodePlayingStatus AND archived = :archived AND podcast_id = :podcastUuid")
    abstract fun findByEpisodePlayingAndArchiveStatus(podcastUuid: String, episodePlayingStatus: EpisodePlayingStatus, archived: Boolean): List<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY UPPER(title) ASC")
    abstract fun findByPodcastOrderTitleAsc(podcastUuid: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY UPPER(title) DESC")
    abstract fun findByPodcastOrderTitleDesc(podcastUuid: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY published_date ASC")
    abstract fun findByPodcastOrderPublishedDateAsc(podcastUuid: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC")
    abstract fun findByPodcastOrderPublishedDateDesc(podcastUuid: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid AND playing_status != 2 AND archived = 0 ORDER BY published_date DESC LIMIT 1")
    abstract fun findLatestUnfinishedEpisodeByPodcast(podcastUuid: String): Episode?

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY duration ASC")
    abstract fun findByPodcastOrderDurationAsc(podcastUuid: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY duration DESC")
    abstract fun findByPodcastOrderDurationDesc(podcastUuid: String): List<Episode>

    // Find new episodes to display in notifications.
    @Query(
        """SELECT episodes.*
        FROM episodes
        JOIN podcasts ON episodes.podcast_id = podcasts.uuid
        WHERE podcasts.subscribed = 1 AND podcasts.show_notifications = 1
        AND (podcasts.added_date IS NULL OR (podcasts.added_date IS NOT NULL AND podcasts.added_date < :date AND podcasts.added_date != episodes.added_date))
        AND episodes.archived = 0 AND episodes.playing_status = :playingStatus AND episodes.added_date >= :date
        ORDER BY episodes.added_date DESC, episodes.published_date DESC LIMIT 100"""
    )
    abstract fun findNotificationEpisodes(date: Date, playingStatus: Int = EpisodePlayingStatus.NOT_PLAYED.ordinal): List<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY UPPER(title) ASC")
    abstract fun observeByPodcastOrderTitleAsc(podcastUuid: String): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY UPPER(title) DESC")
    abstract fun observeByPodcastOrderTitleDesc(podcastUuid: String): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY published_date ASC")
    abstract fun observeByPodcastOrderPublishedDateAsc(podcastUuid: String): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC")
    abstract fun observeByPodcastOrderPublishedDateDesc(podcastUuid: String): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY duration ASC")
    abstract fun observeByPodcastOrderDurationAsc(podcastUuid: String): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY duration DESC")
    abstract fun observeByPodcastOrderDurationDesc(podcastUuid: String): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid AND playing_status != :playingStatus")
    abstract fun findByPodcastAndNotPlayingStatus(podcastUuid: String, playingStatus: Int): List<Episode>

    @Query("UPDATE episodes SET downloaded_error_details = NULL, episode_status = :episodeStatusNotDownloaded WHERE episode_status = :episodeStatusFailed")
    abstract fun clearAllDownloadErrors(episodeStatusNotDownloaded: EpisodeStatusEnum, episodeStatusFailed: EpisodeStatusEnum)

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC, added_date DESC LIMIT 1")
    abstract fun findLatest(podcastUuid: String): Episode?

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid ORDER BY published_date DESC, added_date DESC LIMIT 1")
    abstract fun findLatestRx(podcastUuid: String): Maybe<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastUuid AND playing_status != 2 ORDER BY published_date DESC LIMIT 10")
    abstract fun findPodcastEpisodesForMediaBrowserSearch(podcastUuid: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE (download_task_id IS NOT NULL OR episode_status == :downloadEpisodeStatusEnum OR (episode_status == :failedEpisodeStatusEnum AND last_download_attempt_date > :failedDownloadCutoff AND archived == 0)) ORDER BY last_download_attempt_date DESC")
    abstract fun observeDownloadingEpisodesIncludingFailed(failedDownloadCutoff: Long, failedEpisodeStatusEnum: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOAD_FAILED, downloadEpisodeStatusEnum: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOADED): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE (download_task_id IS NOT NULL AND episode_status == :notDownloaded)")
    abstract suspend fun findStaleDownloads(notDownloaded: EpisodeStatusEnum = EpisodeStatusEnum.NOT_DOWNLOADED): List<Episode>

    @Query("SELECT * FROM episodes WHERE episode_status == :downloadEpisodeStatusEnum ORDER BY last_download_attempt_date DESC")
    abstract fun observeDownloadedEpisodes(downloadEpisodeStatusEnum: EpisodeStatusEnum = EpisodeStatusEnum.DOWNLOADED): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE starred = 1")
    abstract fun observeStarredEpisodes(): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE starred = 1")
    abstract suspend fun findStarredEpisodes(): List<Episode>

    @Query("SELECT * FROM episodes WHERE last_playback_interaction_date IS NOT NULL AND last_playback_interaction_date > 0 ORDER BY last_playback_interaction_date DESC LIMIT 1000")
    abstract fun observePlaybackHistory(): Flowable<List<Episode>>

    @Query("SELECT * FROM episodes WHERE last_playback_interaction_date IS NOT NULL AND last_playback_interaction_date > 0 ORDER BY last_playback_interaction_date DESC LIMIT 1000")
    abstract suspend fun findPlaybackHistoryEpisodes(): List<Episode>

    @Update
    abstract fun update(episode: Episode)

    @Delete
    abstract fun delete(episode: Episode)

    @Delete
    abstract fun deleteAll(episode: List<Episode>)

    @Query("DELETE FROM episodes WHERE uuid = :uuid")
    abstract fun deleteByUuid(uuid: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(episode: Episode)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertAll(episodes: List<Episode>)

    @Query("UPDATE episodes SET file_type = :fileType WHERE uuid = :uuid")
    abstract fun updateFileType(fileType: String, uuid: String)

    @Query("UPDATE episodes SET size_in_bytes = :sizeInBytes WHERE uuid = :uuid")
    abstract fun updateSizeInBytes(sizeInBytes: Long, uuid: String)

    @Query("UPDATE episodes SET download_url = :url WHERE uuid = :uuid")
    abstract fun updateDownloadUrl(url: String, uuid: String)

    @Query("UPDATE episodes SET download_task_id = :taskId WHERE uuid = :uuid")
    abstract fun updateDownloadTaskId(uuid: String, taskId: String?)

    @Query("UPDATE episodes SET last_download_attempt_date = :lastDownloadAttemptDate WHERE uuid = :uuid")
    abstract fun updateLastDownloadAttemptDate(lastDownloadAttemptDate: Date, uuid: String)

    @Query("UPDATE episodes SET downloaded_error_details = :errorMessage, episode_status = :episodeStatus WHERE uuid = :uuid")
    abstract fun updateDownloadError(uuid: String, errorMessage: String?, episodeStatus: EpisodeStatusEnum)

    @Query("UPDATE episodes SET downloaded_file_path = :downloadedFilePath WHERE uuid = :uuid")
    abstract fun updateDownloadedFilePath(downloadedFilePath: String, uuid: String)

    @Query("UPDATE episodes SET auto_download_status = :autoDownloadStatus WHERE uuid = :uuid")
    abstract fun updateAutoDownloadStatus(autoDownloadStatus: Int, uuid: String)

    @Query("UPDATE episodes SET thumbnail_status = :thumbnailStatus WHERE uuid = :uuid")
    abstract fun updateThumbnailStatus(thumbnailStatus: Int, uuid: String)

    @Query("UPDATE episodes SET play_error_details = :playErrorDetails WHERE uuid = :uuid")
    abstract fun updatePlayErrorDetails(playErrorDetails: String?, uuid: String)

    @Query("UPDATE episodes SET downloaded_error_details = :downloadErrorDetails WHERE uuid = :uuid")
    abstract fun updateDownloadErrorDetails(downloadErrorDetails: String?, uuid: String)

    @Query("UPDATE episodes SET episode_status = :episodeStatus WHERE uuid = :uuid")
    abstract fun updateEpisodeStatus(episodeStatus: EpisodeStatusEnum, uuid: String)

    @Query("UPDATE episodes SET episode_status = :episodeStatus")
    abstract fun updateAllEpisodeStatus(episodeStatus: EpisodeStatusEnum)

    @Query("UPDATE episodes SET last_playback_interaction_date = 0 WHERE last_playback_interaction_date <= :lastCleared")
    abstract fun clearEpisodePlaybackInteractionDatesBefore(lastCleared: Date)

    @Query("UPDATE episodes SET last_playback_interaction_date = 0, last_playback_interaction_sync_status = 1")
    abstract suspend fun clearAllEpisodePlaybackInteractions()

    @Query("UPDATE episodes SET last_playback_interaction_sync_status = 1")
    abstract fun markPlaybackHistorySynced()

    @Query("SELECT COUNT(*) FROM episodes")
    abstract fun count(): Int

    @Query("SELECT COUNT(*) FROM episodes WHERE uuid = :uuid")
    abstract fun countByUuid(uuid: String): Int

    fun exists(uuid: String): Boolean {
        return countByUuid(uuid) != 0
    }

    fun existsRx(uuid: String): Single<Boolean> {
        return Single.fromCallable { exists(uuid) }
    }

    @Query("UPDATE episodes SET starred_modified = NULL, archived_modified = NULL, duration_modified = NULL, played_up_to_modified = NULL, playing_status_modified = NULL WHERE uuid IN (:episodeUuids)")
    abstract fun markAllSynced(episodeUuids: List<String>)

    @Query("SELECT podcasts.uuid AS uuid, count(episodes.uuid) AS count FROM episodes, podcasts WHERE episodes.podcast_id = podcasts.uuid AND (episodes.playing_status = :playingStatusNotPlayed OR episodes.playing_status = :playingStatusInProgress) AND episodes.archived = 0 GROUP BY podcasts.uuid")
    protected abstract fun podcastToUnfinishedEpisodeCount(playingStatusNotPlayed: Int = EpisodePlayingStatus.NOT_PLAYED.ordinal, playingStatusInProgress: Int = EpisodePlayingStatus.IN_PROGRESS.ordinal): Flowable<List<UuidCount>>

    @Query("SELECT podcasts.uuid AS uuid, count(episodes.uuid) AS count FROM episodes, podcasts WHERE podcasts.latest_episode_uuid = episodes.uuid AND episodes.playing_status = :playingStatusNotPlayed AND episodes.archived = 0 GROUP BY podcasts.uuid")
    protected abstract fun podcastToLatestEpisodeCount(playingStatusNotPlayed: Int = EpisodePlayingStatus.NOT_PLAYED.ordinal): Flowable<List<UuidCount>>

    fun podcastUuidToUnfinishedEpisodeCount(): Flowable<Map<String, Int>> {
        return podcastToUnfinishedEpisodeCount().map { it.associateBy({ it.uuid }, { it.count }) }
    }

    fun podcastUuidToLatestEpisodeCount(): Flowable<Map<String, Int>> {
        return podcastToLatestEpisodeCount().map { it.associateBy({ it.uuid }, { it.count }) }
    }

    @Query("SELECT episodes.* FROM episodes JOIN podcasts ON episodes.podcast_id = podcasts.uuid WHERE podcasts.subscribed = 1 AND episodes.playing_status != 2 AND episodes.archived = 0 ORDER BY episodes.published_date DESC LIMIT 1")
    abstract fun findLatestEpisodeToPlay(): Episode?

    @Query("UPDATE episodes SET starred = :starred, starred_modified = :modified WHERE uuid = :uuid")
    abstract fun updateStarred(starred: Boolean, modified: Long, uuid: String)

    @Query("UPDATE episodes SET starred = :starred, starred_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun updateAllStarred(episodesUUIDs: List<String>, starred: Boolean, modified: Long)

    @Query("UPDATE episodes SET archived = 0, archived_modified = :modified, last_archive_interaction_date = :modified, exclude_from_episode_limit = 1 WHERE uuid = :uuid")
    abstract fun unarchive(uuid: String, modified: Long)

    @Query("UPDATE episodes SET archived = :archived, archived_modified = :modified, last_archive_interaction_date = :modified WHERE uuid = :uuid")
    abstract fun updateArchived(archived: Boolean, modified: Long, uuid: String)

    @Query("UPDATE episodes SET archived = :archived, last_archive_interaction_date = :modified WHERE uuid = :uuid")
    abstract fun updateArchivedNoSync(archived: Boolean, modified: Long, uuid: String)

    @Query("UPDATE episodes SET playing_status = :playingStatus, playing_status_modified = :modified WHERE uuid = :uuid")
    abstract fun updatePlayingStatus(playingStatus: EpisodePlayingStatus, modified: Long, uuid: String)

    @Query("UPDATE episodes SET last_playback_interaction_date = :modified, last_playback_interaction_sync_status = 0 WHERE uuid = :uuid")
    abstract suspend fun updatePlaybackInteractionDate(uuid: String, modified: Long)

    @Query("UPDATE episodes SET duration = :duration, duration_modified = :modified WHERE uuid = :uuid")
    abstract fun updateDuration(duration: Double, modified: Long, uuid: String)

    @Query("UPDATE episodes SET duration = :duration WHERE uuid = :uuid")
    abstract fun updateDurationNoSync(duration: Double, uuid: String)

    @Query("UPDATE episodes SET played_up_to = :playedUpTo, played_up_to_modified = :modified WHERE uuid = :uuid AND (played_up_to IS NULL OR played_up_to < :playedUpToMin OR played_up_to > :playedUpToMax)")
    abstract fun updatePlayedUpToIfChanged(playedUpTo: Double, playedUpToMin: Double, playedUpToMax: Double, modified: Long, uuid: String)

    fun countWhere(queryAfterWhere: String, appDatabase: AppDatabase): Int {
        val result = QueryHelper.firstRowArray("SELECT count(*) FROM episodes JOIN podcasts ON episodes.podcast_id = podcasts.uuid WHERE podcasts.subscribed = 1 AND $queryAfterWhere", null, appDatabase) ?: return 0
        val firstResult = result[0] ?: return 0
        return Integer.parseInt(firstResult)
    }

    @Query("SELECT * FROM episodes WHERE (playing_status_modified IS NOT NULL OR played_up_to_modified IS NOT NULL OR duration_modified IS NOT NULL OR archived_modified IS NOT NULL OR starred_modified IS NOT NULL) AND uuid IS NOT NULL LIMIT 2000")
    abstract fun findEpisodesToSync(): List<Episode>

    @Query("SELECT episodes.* FROM podcasts, episodes WHERE episodes.podcast_id = podcasts.uuid AND episodes.podcast_id = :podcastUuid AND podcasts.subscribed = 1 AND episodes.archived = 0 AND (episodes.added_date < :inactiveTime AND (CASE WHEN episodes.last_playback_interaction_date IS NULL THEN 0 ELSE episodes.last_playback_interaction_date END) < :inactiveTime AND (CASE WHEN episodes.last_download_attempt_date IS NULL THEN 0 ELSE episodes.last_download_attempt_date END) < :inactiveDate AND (CASE WHEN episodes.last_archive_interaction_date IS NULL THEN 0 ELSE episodes.last_archive_interaction_date END) < :inactiveTime )")
    abstract fun findInactiveEpisodes(podcastUuid: String, inactiveDate: Date, inactiveTime: Long = inactiveDate.time): List<Episode>

    @Query("UPDATE episodes SET archived = 1, archived_modified = :modified, last_archive_interaction_date = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract fun archiveAllInList(episodesUUIDs: List<String>, modified: Long)

    @Query("UPDATE episodes SET archived = 0, archived_modified = :modified, last_archive_interaction_date = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract fun unarchiveAllInList(episodesUUIDs: List<String>, modified: Long)

    @Query("UPDATE episodes SET playing_status = :playingStatus, playing_status_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun updateAllPlayingStatus(episodesUUIDs: List<String>, modified: Long, playingStatus: EpisodePlayingStatus)

    @Query("UPDATE episodes SET playing_status = :playingStatus, playing_status_modified = :modified, played_up_to = 0, played_up_to_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun markAllUnplayed(episodesUUIDs: List<String>, modified: Long, playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED)

    @Query("SELECT SUM(played_up_to) FROM episodes WHERE last_playback_interaction_date IS NOT NULL AND last_playback_interaction_date > :fromEpochMs AND last_playback_interaction_date < :toEpochMs")
    abstract fun calculateListeningTime(fromEpochMs: Long, toEpochMs: Long): Flow<Long?>

    @Query(
        """
        SELECT COUNT(DISTINCT podcast_id) as numberOfPodcasts, SUM(played_up_to) as totalPlayedTime, REPLACE(IFNULL(NULLIF(SUBSTR(podcasts.podcast_category, 0, INSTR(podcasts.podcast_category, char(10))), ''), podcasts.podcast_category), char(10), '') as category, podcasts.uuid as mostListenedPodcastId, podcasts.primary_color as mostListenedPodcastTintColor
        FROM episodes
        JOIN podcasts ON episodes.podcast_id = podcasts.uuid
        WHERE episodes.last_playback_interaction_date IS NOT NULL AND episodes.last_playback_interaction_date > :fromEpochMs AND episodes.last_playback_interaction_date < :toEpochMs
        GROUP BY category
        ORDER BY totalPlayedTime DESC
        """
    )
    abstract fun findListenedCategories(fromEpochMs: Long, toEpochMs: Long): Flow<List<ListenedCategory>>

    @Query(
        """
        SELECT COUNT(episodes.uuid) as numberOfEpisodes, COUNT(DISTINCT podcasts.uuid) as numberOfPodcasts
        FROM episodes
        JOIN podcasts ON episodes.podcast_id = podcasts.uuid
        WHERE episodes.last_playback_interaction_date IS NOT NULL AND episodes.last_playback_interaction_date > :fromEpochMs AND episodes.last_playback_interaction_date < :toEpochMs
        """
    )
    abstract fun findListenedNumbers(fromEpochMs: Long, toEpochMs: Long): Flow<ListenedNumbers>

    @Query(
        """
        SELECT episodes.title, episodes.duration, podcasts.uuid as podcastUuid, podcasts.title as podcastTitle, podcasts.primary_color as tintColorForLightBg, podcasts.secondary_color as tintColorForDarkBg
        FROM episodes
        JOIN podcasts ON episodes.podcast_id = podcasts.uuid
        WHERE episodes.last_playback_interaction_date IS NOT NULL AND episodes.last_playback_interaction_date > :fromEpochMs AND episodes.last_playback_interaction_date < :toEpochMs
        ORDER BY episodes.played_up_to DESC
        LIMIT 1
        """
    )
    abstract fun findLongestPlayedEpisode(fromEpochMs: Long, toEpochMs: Long): Flow<LongestEpisode?>

    @Query(
        """
        SELECT COUNT(*) 
        FROM episodes
        WHERE played_up_to > :playedUpToInSecs 
        AND episodes.last_playback_interaction_date IS NOT NULL AND episodes.last_playback_interaction_date > :fromEpochMs AND episodes.last_playback_interaction_date < :toEpochMs
        """
    )
    abstract fun countEpisodesPlayedUpto(fromEpochMs: Long, toEpochMs: Long, playedUpToInSecs: Long): Flow<Int>

    @Query(
        """
        SELECT * 
        FROM episodes
        WHERE episodes.last_playback_interaction_date IS NOT NULL AND episodes.last_playback_interaction_date < :fromEpochMs AND episodes.last_playback_interaction_date != 0
        LIMIT 1
        """
    )
    abstract suspend fun findEpisodeInteractedBefore(fromEpochMs: Long): Episode?

    @Query(
        """
        SELECT COUNT(*) 
        FROM episodes
        WHERE episodes.last_playback_interaction_date IS NOT NULL AND episodes.last_playback_interaction_date > :fromEpochMs AND episodes.last_playback_interaction_date < :toEpochMs
        """
    )
    abstract suspend fun findEpisodesCountInListeningHistory(fromEpochMs: Long, toEpochMs: Long): Int
}
