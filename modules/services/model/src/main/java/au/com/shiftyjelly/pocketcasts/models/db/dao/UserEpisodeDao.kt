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
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import java.util.Date
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
    open suspend fun findEpisodesByUuids(episodeUuids: List<String>): List<UserEpisode> {
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
    abstract fun updateEpisodeStatusBlocking(uuid: String, episodeStatus: EpisodeStatusEnum)

    @Query("UPDATE user_episodes SET auto_download_status = :autoDownloadStatus WHERE uuid = :uuid")
    abstract fun updateAutoDownloadStatusBlocking(autoDownloadStatus: Int, uuid: String)

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
}
