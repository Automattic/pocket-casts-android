package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.hilt.work.HiltWorker
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.work.Worker
import androidx.work.WorkerParameters
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

    override fun doWork(): Result {
        try {
            val url = inputData.getString(URL_KEY)
            val episodeUuid = inputData.getString(EPISODE_UUID_KEY)
            val uri = Uri.parse(url)

            val dataSourceFactory = exoPlayerHelper.getDataSourceFactory()
            var dataSpec = DataSpec(uri)
            if (episodeUuid != null) {
                dataSpec = dataSpec.buildUpon().setKey(episodeUuid).build()
            }

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
            ) { requestLength: Long, bytesCached: Long, _: Long ->
                if (bytesCached == requestLength) {
                    Timber.d("$TAG: Caching complete for episode id: $episodeUuid")
                }
            }
            cacheWriter?.cache()
        } catch (exception: Exception) {
            Timber.e("$TAG: ${exception.message}")
        }
        return Result.success()
    }

    override fun onStopped() {
        try {
            cacheWriter?.cancel()
            super.onStopped()
        } catch (exception: Exception) {
            Timber.e("$TAG: ${exception.message}")
        }
    }

    companion object {
        private const val TAG = "CacheWorker"
        const val URL_KEY = "url_key"
        const val EPISODE_UUID_KEY = "episode_uuid_key"
    }
}
