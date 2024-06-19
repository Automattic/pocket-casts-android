package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
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
    }

    private var simpleCache: SimpleCache? = null

    @OptIn(UnstableApi::class)
    @Synchronized
    fun getSimpleCache(): SimpleCache? {
        if (FeatureFlag.isEnabled(Feature.CACHE_PLAYING_EPISODE) && simpleCache == null) {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            val cacheSizeInBytes = settings.getExoPlayerCacheSizeInMB() * 1024 * 1024L
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
}
