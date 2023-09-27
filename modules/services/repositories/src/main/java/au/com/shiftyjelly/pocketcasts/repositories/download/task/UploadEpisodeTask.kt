package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Data
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.sync.parseErrorResponse
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Single
import retrofit2.HttpException

@HiltWorker
class UploadEpisodeTask @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    var userEpisodeManager: UserEpisodeManager,
    var playbackManager: PlaybackManager
) : RxWorker(context, params) {

    companion object {
        const val INPUT_EPISODE_UUID = "episode_uuid"
        const val OUTPUT_ERROR_MESSAGE = "error_message"
    }

    private val episodeUUID: String? = inputData.getString(INPUT_EPISODE_UUID)

    override fun createWork(): Single<Result> {
        var outputData = Data.Builder().putString(DownloadEpisodeTask.OUTPUT_EPISODE_UUID, episodeUUID)

        if (episodeUUID == null) {
            outputData = outputData.putString(OUTPUT_ERROR_MESSAGE, "Could not find episode $episodeUUID for upload")
            return Single.just(Result.failure(outputData.build()))
        }

        return userEpisodeManager.findEpisodeByUuidRx(episodeUUID)
            .flatMapCompletable { userEpisode ->
                userEpisodeManager.performUploadToServer(userEpisode, playbackManager)
            }
            .andThen(Single.just(Result.success(outputData.build())))
            .onErrorReturn {
                LogBuffer.logException(LogBuffer.TAG_BACKGROUND_TASKS, it, "Could not upload file")
                var errorMessage: String?
                val retry: Boolean

                if (it is HttpException) {
                    val errorResponse = it.parseErrorResponse()
                    errorMessage = errorResponse?.messageLocalized(context.resources)

                    if (errorMessage == null) {
                        errorMessage = when (it.code()) {
                            400 -> "Unable to upload, unsupported file"
                            else -> "Unable to upload, please check your internet connection and try again"
                        }
                    }

                    retry = it.code() != 400
                } else {
                    errorMessage = "Unable to upload, please check your internet connection and try again"
                    retry = true
                }

                outputData = outputData.putString(OUTPUT_ERROR_MESSAGE, errorMessage)
                if (retry && runAttemptCount < 3) {
                    return@onErrorReturn Result.retry()
                } else {
                    return@onErrorReturn Result.failure(outputData.build())
                }
            }
    }
}
