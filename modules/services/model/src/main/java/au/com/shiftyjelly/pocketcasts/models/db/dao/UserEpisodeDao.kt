package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.DownloadStatusUpdate
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserEpisodeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(userEpisode: UserEpisode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertRxCompletable(userEpisode: UserEpisode): Completable

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(userEpisodes: List<UserEpisode>)

    @Update
    abstract suspend fun update(userEpisode: UserEpisode)

    @Delete
    abstract suspend fun delete(userEpisode: UserEpisode)

    @Delete
    abstract suspend fun deleteAll(userEpisode: List<UserEpisode>)

    @Query("SELECT uuid FROM user_episodes")
    abstract suspend fun findAllUuids(): List<String>

    @Query("SELECT * FROM user_episodes ORDER BY added_date DESC")
    abstract fun findUserEpisodesDescRxFlowable(): Flowable<List<UserEpisode>>

    @Query("SELECT * FROM user_episodes ORDER BY added_date DESC")
    abstract suspend fun findUserEpisodesDesc(): List<UserEpisode>

    @Query("SELECT * FROM user_episodes ORDER BY added_date ASC")
    abstract fun findUserEpisodesAscRxFlowable(): Flowable<List<UserEpisode>>

    @Query("SELECT * FROM user_episodes ORDER BY title ASC")
    abstract fun findUserEpisodesTitleAscRxFlowable(): Flowable<List<UserEpisode>>

    @Query("SELECT * FROM user_episodes ORDER BY title DESC")
    abstract fun findUserEpisodesTitleDescRxFlowable(): Flowable<List<UserEpisode>>

    @Query("SELECT * FROM user_episodes ORDER BY duration ASC")
    abstract fun findUserEpisodesDurationAscRxFlowable(): Flowable<List<UserEpisode>>

    @Query("SELECT * FROM user_episodes ORDER BY duration DESC")
    abstract fun findUserEpisodesDurationDescRxFlowable(): Flowable<List<UserEpisode>>

    @Query("SELECT * FROM user_episodes WHERE download_task_id IS NOT NULL")
    abstract fun findDownloadingUserEpisodesRxFlowable(): Flowable<List<UserEpisode>>

    @Query("SELECT * FROM user_episodes WHERE uuid = :uuid")
    abstract fun findEpisodeRxFlowable(uuid: String): Flowable<UserEpisode>

    @Query("SELECT * FROM user_episodes WHERE uuid = :uuid")
    abstract fun findEpisodeFlow(uuid: String): Flow<UserEpisode?>

    @Query("SELECT * FROM user_episodes WHERE uuid = :uuid")
    abstract fun findEpisodeByUuidRxMaybe(uuid: String): Maybe<UserEpisode>

    @Query("SELECT * FROM user_episodes WHERE uuid = :uuid")
    abstract suspend fun findEpisodeByUuid(uuid: String): UserEpisode?

    @Query("SELECT * FROM user_episodes WHERE uuid IN (:episodeUuids)")
    protected abstract suspend fun findEpisodesByUuidsUnsafe(episodeUuids: Collection<String>): List<UserEpisode>

    @Transaction
    open suspend fun findEpisodesByUuids(episodeUuids: Collection<String>): List<UserEpisode> {
        return episodeUuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT).flatMap { chunk ->
            findEpisodesByUuidsUnsafe(chunk)
        }
    }

    @Query("UPDATE user_episodes SET played_up_to = :playedUpTo, played_up_to_modified = :modified WHERE uuid = :uuid AND (played_up_to IS NULL OR played_up_to < :playedUpToMin OR played_up_to > :playedUpToMax)")
    abstract fun updatePlayedUpToIfChangedBlocking(playedUpTo: Double, playedUpToMin: Double, playedUpToMax: Double, modified: Long, uuid: String)

    @Query("UPDATE user_episodes SET duration = :duration WHERE uuid = :uuid")
    abstract fun updateDurationBlocking(duration: Double, uuid: String)

    @Query("UPDATE user_episodes SET playing_status = :playingStatus, playing_status_modified = :modified WHERE uuid = :uuid")
    abstract fun updatePlayingStatusBlocking(playingStatus: EpisodePlayingStatus, modified: Long, uuid: String)

    @Query("UPDATE user_episodes SET episode_status = :episodeStatus WHERE uuid = :uuid")
    abstract fun updateEpisodeStatusBlocking(uuid: String, episodeStatus: EpisodeDownloadStatus)

    @Query("UPDATE user_episodes SET auto_download_status = 1 WHERE uuid IN (:uuids)")
    protected abstract suspend fun disableAutoDownloadUnsafe(uuids: Collection<String>)

    @Transaction
    open suspend fun disableAutoDownload(uuids: Collection<String>) {
        uuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT).forEach { chunk ->
            disableAutoDownloadUnsafe(chunk)
        }
    }

    @Query("UPDATE user_episodes SET server_status = :serverStatus WHERE uuid = :uuid")
    abstract fun updateServerStatusRxCompletable(uuid: String, serverStatus: UserEpisodeServerStatus): Completable

    @Query("UPDATE user_episodes SET server_status = :serverStatus WHERE uuid = :uuid")
    abstract suspend fun updateServerStatus(uuid: String, serverStatus: UserEpisodeServerStatus)

    @Query("UPDATE user_episodes SET downloaded_file_path = :downloadPath WHERE uuid = :uuid")
    abstract suspend fun updateDownloadedFilePath(uuid: String, downloadPath: String)

    @Query("UPDATE user_episodes SET file_type = :fileType WHERE uuid = :uuid")
    abstract suspend fun updateFileType(uuid: String, fileType: String)

    @Query("UPDATE user_episodes SET size_in_bytes = :sizeInBytes WHERE uuid = :uuid")
    abstract suspend fun updateSizeInBytes(uuid: String, sizeInBytes: Long)

    @Query("UPDATE user_episodes SET last_download_attempt_date = :date WHERE uuid = :uuid")
    abstract suspend fun updateDownloadedAttemptDate(uuid: String, date: Date)

    @Query("UPDATE user_episodes SET downloaded_error_details = :error WHERE uuid = :uuid")
    abstract suspend fun updateDownloadError(uuid: String, error: String?)

    @Query("UPDATE user_episodes SET play_error_details = :error WHERE uuid = :uuid")
    abstract fun updatePlayErrorBlocking(uuid: String, error: String?)

    @Query("UPDATE user_episodes SET download_task_id = :taskId WHERE uuid = :uuid")
    abstract suspend fun updateDownloadTaskId(uuid: String, taskId: String?)

    @Query("UPDATE user_episodes SET upload_error_details = :uploadError WHERE uuid = :uuid")
    abstract fun updateUploadErrorRxCompetable(uuid: String, uploadError: String?): Completable

    @Query("UPDATE user_episodes SET upload_error_details = :uploadError WHERE uuid = :uuid")
    abstract suspend fun updateUploadError(uuid: String, uploadError: String?)

    @Query("SELECT * FROM user_episodes WHERE (playing_status_modified IS NOT NULL OR played_up_to_modified IS NOT NULL) AND uuid IS NOT NULL LIMIT 2000")
    abstract fun findUserEpisodesToSyncBlocking(): List<UserEpisode>

    @Query("UPDATE user_episodes SET played_up_to_modified = NULL, playing_status_modified = NULL")
    abstract fun markAllSyncedBlocking()

    @Query("UPDATE user_episodes SET upload_task_id = :uploadTaskId WHERE uuid = :uuid")
    abstract suspend fun updateUploadTaskId(uuid: String, uploadTaskId: String?)

    @Query("SELECT * FROM user_episodes WHERE upload_task_id = :uploadTaskId")
    abstract suspend fun findByUploadTaskId(uploadTaskId: String): UserEpisode?

    @Query("UPDATE user_episodes SET playing_status = :playingStatus, playing_status_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun updateAllPlayingStatus(episodesUUIDs: List<String>, modified: Long, playingStatus: EpisodePlayingStatus)

    @Query("UPDATE user_episodes SET playing_status = :playingStatus, playing_status_modified = :modified, played_up_to = 0, played_up_to_modified = :modified WHERE uuid IN (:episodesUUIDs)")
    abstract suspend fun markAllUnplayed(episodesUUIDs: List<String>, modified: Long, playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED)

    @Transaction
    @Query("SELECT * FROM user_episodes WHERE episode_status IN (:statuses)")
    abstract suspend fun getEpisodesWithDownloadStatus(statuses: Collection<EpisodeDownloadStatus>): List<UserEpisode>

    /**
     * Atomically updates the download-related fields for a single episode, with an optimistic-ownership guard
     * based on [taskId] to avoid race conditions between overlapping WorkManager runs (or enqueue/cancel flows).
     *
     * WorkManager jobs for the same episode can overlap in time:
     * - A new download is enqueued while an older worker is still running.
     * - A cancel/remove-from-queue happens while a worker is finishing.
     * - WorkManager retries / reschedules a work request and an "old" attempt reports completion late.
     *
     * Without a guard, a stale worker could “win” and overwrite the episode row with out-of-date state:
     * - Old worker marks the episode as Downloaded/Failed after a newer worker has already started.
     * - Old worker clears `download_task_id`, breaking the newer worker’s ability to report progress.
     * - A late failure overwrites a successful newer download path, or vice versa.
     */
    @Query(
        """
        UPDATE user_episodes 
        SET
          episode_status = :status,
          downloaded_file_path = :downloadPath,
          downloaded_error_details = :downloadError,
          download_task_id = CASE
            WHEN :status IN (0, 3, 4) THEN NULL
            ELSE download_task_id
          END,
          -- Update the timestamps only if we're changing to downloading state
          last_download_attempt_date = CASE
            WHEN :status = 2 THEN :issuedAt
            ELSE last_download_attempt_date
          END
        WHERE
          uuid = :episodeUuid
          AND download_task_id = :taskId
          AND episode_status != :status
        """,
    )
    protected abstract suspend fun updateDownloadStatus(
        episodeUuid: String,
        status: EpisodeDownloadStatus,
        taskId: String,
        issuedAt: Date,
        downloadPath: String?,
        downloadError: String?,
    ): Int

    suspend fun updateDownloadStatus(episodeUuid: String, statusUpdate: DownloadStatusUpdate): Boolean {
        val rowUpdateCount = updateDownloadStatus(
            episodeUuid = episodeUuid,
            status = statusUpdate.episodeStatus,
            taskId = statusUpdate.taskId.toString(),
            issuedAt = Date.from(statusUpdate.issuedAt),
            downloadPath = statusUpdate.outputFile?.path,
            downloadError = statusUpdate.errorMessage,
        )
        return rowUpdateCount == 1
    }

    /**
     * Atomically attempts to acquire download ownership for a single episode.
     *
     * `download_task_id` acts as an ownership token for the currently active download attempt.
     * This query sets a new [downloadTaskId] only if no task currently owns the episode
     * (`download_task_id IS NULL`).
     *
     * This prevents races where:
     * - The user taps download multiple times.
     * - Auto-download and manual download overlap.
     * - A retry is scheduled while a previous attempt is still finishing.
     *
     * Without the guard, multiple workers could believe they own the episode and
     * overwrite each other’s state.
     */
    @Query(
        """
        UPDATE user_episodes
        SET
          archived = 0,
          episode_status = 1,
          download_task_id = :downloadTaskId,
          auto_download_status = 0,
          last_download_attempt_date = :issuedAt
        WHERE
          uuid = :episodeUuid
          AND (:forceNewDownload OR download_task_id IS NULL)
        """,
    )
    protected abstract suspend fun setReadyForDownloadRaw(
        episodeUuid: String,
        downloadTaskId: String,
        issuedAt: Date,
        forceNewDownload: Boolean,
    ): Int

    suspend fun setReadyForDownload(
        episodeUuid: String,
        downloadTaskId: UUID,
        issuedAt: Instant,
        forceNewDownload: Boolean,
    ): Boolean {
        val rowUpdateCount = setReadyForDownloadRaw(
            episodeUuid = episodeUuid,
            downloadTaskId = downloadTaskId.toString(),
            issuedAt = Date.from(issuedAt),
            forceNewDownload = forceNewDownload,
        )
        return rowUpdateCount == 1
    }

    /**
     * Atomically cancels an in-progress download and releases ownership.
     *
     * This query resets the episode to `DownloadNotRequested` (status = 0)
     * and clears `download_task_id`. It also resets episodes currently in
     * `Downloaded` state (status = 4) back to `DownloadNotRequested`.
     *
     * `download_task_id` is treated as an ownership token for the active
     * download attempt. The `AND (download_task_id IS NOT NULL OR episode_status = 4)`
     * guard ensures cancellation only applies if a task currently owns the episode
     * or if the episode is already marked as Downloaded.
     *
     * This prevents unnecessary writes and avoids interfering with episodes
     * that are already idle. Clearing `download_task_id` releases ownership
     * so a future enqueue can safely acquire it.
     */
    @Query(
        """
        UPDATE user_episodes
        SET
          episode_status = 0,
          downloaded_file_path = NULL,
          downloaded_error_details = NULL,
          download_task_id = null
        WHERE
          uuid = :episodeUuid
          AND (download_task_id IS NOT NULL OR episode_status = 4)
        """,
    )
    protected abstract suspend fun setDownloadCancelledRaw(episodeUuid: String): Int

    suspend fun setDownloadCancelled(episodeUuid: String): Boolean {
        val rowUpdateCount = setDownloadCancelledRaw(episodeUuid)
        return rowUpdateCount == 1
    }

    @Query(
        """
        UPDATE user_episodes 
        SET
          episode_status = 0,
          downloaded_file_path = NULL,
          downloaded_error_details = NULL,
          download_task_id = NULL
        WHERE
          uuid = :episodeUuid
          AND download_task_id = :taskId
        """,
    )
    abstract suspend fun clearDownloadForEpisode(episodeUuid: String, taskId: String)

    @Query(
        """
        UPDATE user_episodes
        SET
          episode_status = 0,
          downloaded_file_path = NULL,
          downloaded_error_details = NULL
        WHERE
          download_task_id IS NULL
          AND episode_status IN (:statuses)
        """,
    )
    abstract suspend fun clearDownloadsWithoutTaskId(statuses: Collection<EpisodeDownloadStatus>)
}
