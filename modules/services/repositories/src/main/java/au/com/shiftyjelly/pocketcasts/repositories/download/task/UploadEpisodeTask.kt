package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.sync.parseErrorResponse
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.squareup.moshi.Moshi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.rx2.await
import retrofit2.HttpException

@HiltWorker
class UploadEpisodeTask @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userEpisodeManager: UserEpisodeManager,
    private val playbackManager: PlaybackManager,
    private val moshi: Moshi,
) : CoroutineWorker(context, params) {

    companion object {
        const val INPUT_EPISODE_UUID = "episode_uuid"
        const val OUTPUT_EPISODE_UUID = "episode_uuid"
        const val OUTPUT_ERROR_MESSAGE = "error_message"
    }

    private val episodeUuid: String? = inputData.getString(INPUT_EPISODE_UUID)

    override suspend fun doWork(): Result {
        val outputData = Data.Builder().putString(OUTPUT_EPISODE_UUID, episodeUuid)

        if (episodeUuid == null) {
            outputData.putString(OUTPUT_ERROR_MESSAGE, "Could not find episode $episodeUuid for upload")
            return Result.failure(outputData.build())
        }

        return try {
            // A missing episode skips the upload and still succeeds
            val userEpisode = userEpisodeManager.findEpisodeByUuid(episodeUuid)
            if (userEpisode != null) {
                userEpisodeManager.performUploadToServerRxCompletable(userEpisode, playbackManager).await()
            }
            Result.success(outputData.build())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Could not upload file")
            val errorMessage: String
            val retry: Boolean

            if (e is HttpException) {
                errorMessage = e.parseErrorResponse(moshi)?.messageLocalized(applicationContext.resources)
                    ?: when (e.code()) {
                        400 -> "Unable to upload, unsupported file"
                        else -> "Unable to upload, please check your internet connection and try again"
                    }
                retry = e.code() != 400
            } else {
                errorMessage = "Unable to upload, please check your internet connection and try again"
                retry = true
            }

            outputData.putString(OUTPUT_ERROR_MESSAGE, errorMessage)
            if (retry && runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(outputData.build())
            }
        }
    }
}
