package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.BuildConfig
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
@OptIn(UnstableApi::class)
class ExoPlayerHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings,
    private val crashLogging: CrashLogging,
) {
    companion object {
        private const val CACHE_DIR_NAME = "pocketcasts-exoplayer-cache"
        private const val TIMEOUT_MILLI_SECS = 60 * 1000
        private const val USER_AGENT = "Pocket Casts"
    }

    private var simpleCache: SimpleCache? = null
    private var dataSourceFactory: DataSource.Factory? = null

    @OptIn(UnstableApi::class)
    @Synchronized
    fun getSimpleCache(): SimpleCache? {
        if (
            simpleCache == null &&
            (FeatureFlag.isEnabled(Feature.CACHE_PLAYING_EPISODE) || FeatureFlag.isEnabled(Feature.CACHE_ENTIRE_PLAYING_EPISODE))
        ) {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            val cacheSizeInBytes = if (settings.cacheEntirePlayingEpisode.value) {
                settings.getExoPlayerCacheEntirePlayingEpisodeSizeInMB()
            } else {
                settings.getExoPlayerCacheSizeInMB()
            } * 1024 * 1024L
            simpleCache = try {
                if (BuildConfig.DEBUG) Timber.d("ExoPlayer cache initialized")
                SimpleCache(
                    cacheDir,
                    LeastRecentlyUsedCacheEvictor(cacheSizeInBytes),
                    StandaloneDatabaseProvider(context),
                )
            } catch (e: Exception) {
                val errorMessage = "Failed to instantiate ExoPlayer cache ${e.message}"
                crashLogging.sendReport(Exception(errorMessage))
                LogBuffer.e(LogBuffer.TAG_PLAYBACK, errorMessage)
                null
            }
        }
        return simpleCache
    }

    fun getDataSourceFactory(): DataSource.Factory {
        if (dataSourceFactory == null) {
            dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(TIMEOUT_MILLI_SECS)
                .setReadTimeoutMs(TIMEOUT_MILLI_SECS)
        }
        return requireNotNull(dataSourceFactory)
    }
}
