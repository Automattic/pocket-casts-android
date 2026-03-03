package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.hilt.work.HiltWorker
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@OptIn(UnstableApi::class)
@HiltWorker
class PrefetchWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val sourceFactory: ExoPlayerDataSourceFactory,
) : Worker(context, params) {
    private var cacheWriter: CacheWriter? = null
    private val episodeUuid get() = inputData.getString(EPISODE_UUID_KEY)
    private val downloadUrl get() = inputData.getString(URL_KEY)
    private val prefetchBytes get() = inputData.getLong(PREFETCH_BYTES_KEY, DEFAULT_PREFETCH_BYTES)

    override fun doWork(): Result {
        try {
            if (downloadUrl == null || episodeUuid == null) {
                Timber.tag(TAG).e("Error: Episode download url or uuid is null, downloadUrl: '$downloadUrl' episodeUuid: '$episodeUuid' worker id: '$id'")
                return Result.failure()
            }
            val uri = Uri.parse(downloadUrl)

            val dataSpec = DataSpec(uri).buildUpon()
                .setKey(episodeUuid)
                .setLength(prefetchBytes)
                .build()

            val cacheDataSource = sourceFactory.cacheFactory.createDataSource()
            cacheWriter = CacheWriter(cacheDataSource, dataSpec, null, null)
            cacheWriter?.cache()
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Prefetch complete for episode id: $episodeUuid worker id: '$id'")

            return Result.success()
        } catch (exception: Exception) {
            val errorMessage = "Failed to prefetch episode '$episodeUuid' for url '$downloadUrl' worker id: '$id'"
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
        private const val TAG = "PrefetchWorker"
        private const val PREFETCH_WORKER_TAG = "pocket_casts_prefetch_worker_tag"
        private const val URL_KEY = "url_key"
        private const val EPISODE_UUID_KEY = "episode_uuid_key"
        private const val PREFETCH_BYTES_KEY = "prefetch_bytes_key"
        private const val DEFAULT_PREFETCH_BYTES = 1L * 1024 * 1024 // 1 MB

        fun prefetchNextEpisode(
            context: Context,
            episodeUuid: String,
            downloadUrl: String,
            networkConstraint: NetworkType,
        ) {
            val inputData = Data.Builder()
                .putString(URL_KEY, downloadUrl)
                .putString(EPISODE_UUID_KEY, episodeUuid)
                .putLong(PREFETCH_BYTES_KEY, DEFAULT_PREFETCH_BYTES)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkConstraint)
                .build()

            val workRequest = OneTimeWorkRequest.Builder(PrefetchWorker::class.java)
                .addTag(PREFETCH_WORKER_TAG)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(PREFETCH_WORKER_TAG, ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun cancelPrefetch(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PREFETCH_WORKER_TAG)
        }
    }
}
