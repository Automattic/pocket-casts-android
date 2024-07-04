package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.hilt.work.HiltWorker
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.work.Constraints
import androidx.work.Data
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
class CacheWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val exoPlayerHelper: ExoPlayerHelper,
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

            val dataSourceFactory = exoPlayerHelper.getDataSourceFactory()
            val dataSpec = DataSpec(uri).buildUpon().setKey(episodeUuid).build()

            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setUpstreamDataSourceFactory(dataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            exoPlayerHelper.getSimpleCache()?.let {
                cacheDataSourceFactory.setCache(it)
            }

            cacheWriter = CacheWriter(
                cacheDataSourceFactory.createDataSource(),
                dataSpec,
                null,
                null,
            )
            cacheWriter?.cache()
            Timber.tag(TAG).d("Caching complete for episode id: $episodeUuid worker id: '$id'")
        } catch (exception: Exception) {
            val errorMessage = "Failed to cache episode '$episodeUuid' for url '$downloadUrl' worker id: '$id'"
            Timber.tag(TAG).e(exception, errorMessage)
            LogBuffer.e(LogBuffer.TAG_PLAYBACK, errorMessage)
            return Result.failure()
        }
        return Result.success()
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
            url: String?,
            episodeUuid: String?,
        ) {
            val inputData = Data.Builder()
                .putString(URL_KEY, url)
                .putString(EPISODE_UUID_KEY, episodeUuid)
                .build()

            // Cancel previous caching work
            WorkManager.getInstance(context).cancelAllWorkByTag(CACHE_WORKER_TAG)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val cacheWorkRequest = OneTimeWorkRequest.Builder(CacheWorker::class.java)
                .addTag(CACHE_WORKER_TAG)
                .setConstraints(constraints)
                .setInputData(inputData).build()

            // Enqueue new caching work
            WorkManager.getInstance(context).enqueue(cacheWorkRequest)
        }
    }
}
