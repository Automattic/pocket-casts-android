package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(UnstableApi::class)
@HiltWorker
class CacheWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val sourceFactory: ExoPlayerDataSourceFactory,
) : Worker(context, params) {
    private var cacheWriter: CacheWriter? = null
    private val episodeUuid get() = inputData.getString(EPISODE_UUID_KEY)
    private val downloadUrl get() = inputData.getString(URL_KEY)

    override fun doWork(): Result {
        try {
            if (downloadUrl == null || episodeUuid == null) {
                Timber.tag(TAG).e("Error: Episode download url or uuid is null, downloadUrl: '$downloadUrl' episodeUuid: '$episodeUuid' worker id: '$id'")
                return Result.failure()
            }
            val uri = Uri.parse(downloadUrl)

            val dataSpec = DataSpec(uri).buildUpon().setKey(episodeUuid).build()

            val cacheDataSourceFactory = sourceFactory.cacheFactory

            cacheWriter = CacheWriter(
                cacheDataSourceFactory.createDataSource(),
                dataSpec,
                null,
                null,
            )
            cacheWriter?.cache()
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Caching complete for episode id: $episodeUuid worker id: '$id'")

            val outputData = Data.Builder().putString(EPISODE_UUID_KEY, episodeUuid).build()
            return Result.success(outputData)
        } catch (exception: Exception) {
            val errorMessage = "Failed to cache episode '$episodeUuid' for url '$downloadUrl' worker id: '$id'"
            Timber.tag(TAG).e(exception, errorMessage)
            LogBuffer.e(LogBuffer.TAG_PLAYBACK, exception, errorMessage)
            return Result.failure()
        }
    }

    override fun onStopped() {
        try {
            cacheWriter?.cancel()
            super.onStopped()
        } catch (exception: Exception) {
            Timber.tag(TAG).e("Error: ${exception.message} worker id: '$id'")
        }
    }

    companion object {
        private const val TAG = "CacheWorker"
        private const val CACHE_WORKER_TAG = "pocket_casts_cache_worker_tag"
        private const val URL_KEY = "url_key"
        private const val EPISODE_UUID_KEY = "episode_uuid_key"

        fun startCachingEntireEpisode(
            context: Context,
            url: String,
            episodeUuid: String,
            onCachingComplete: (String) -> Unit,
        ) {
            val inputData = Data.Builder()
                .putString(URL_KEY, url)
                .putString(EPISODE_UUID_KEY, episodeUuid)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val cacheWorkRequest = OneTimeWorkRequest.Builder(CacheWorker::class.java)
                .addTag(CACHE_WORKER_TAG)
                .setConstraints(constraints)
                .setInputData(inputData).build()

            observeWorkerInfo(context, cacheWorkRequest, episodeUuid, onCachingComplete)

            // Enqueue unique caching work by replacing any existing work with the same tag
            WorkManager.getInstance(context).enqueueUniqueWork(CACHE_WORKER_TAG, ExistingWorkPolicy.REPLACE, cacheWorkRequest)
        }

        private fun observeWorkerInfo(
            context: Context,
            cacheWorkRequest: OneTimeWorkRequest,
            episodeUuid: String,
            onCachingComplete: (String) -> Unit,
        ) {
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                WorkManager.getInstance(context)
                    .getWorkInfoByIdFlow(cacheWorkRequest.id).collectLatest { workInfo ->
                        if (workInfo?.state == WorkInfo.State.SUCCEEDED &&
                            workInfo.outputData.getString(EPISODE_UUID_KEY) == episodeUuid
                        ) {
                            /* For fully cached media, ExoPlayer's buffer position might not reach the end,
                            even when the entire media is cached. This is due to ExoPlayer's LoadControl
                            mechanism, which might not request the entire media to be buffered even when it's
                            fully cached.

                            This callback is triggered when the media is fully cached and is used to
                            manually update the buffer position to the end of the media, ensuring more accurate
                            buffer status tracking.

                            See https://github.com/google/ExoPlayer/issues/9172#issuecomment-877250396 for more details. */
                            onCachingComplete(episodeUuid)
                        }
                    }
            }
        }
    }
}
