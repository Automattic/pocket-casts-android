package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.Player
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import okhttp3.OkHttpClient

@Singleton
@OptIn(UnstableApi::class)
class ExoPlayerDataSourceFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    @Player private val client: OkHttpClient,
    private val settings: Settings,
    private val crashLogging: CrashLogging,
) {
    private val cache = runCatching {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        val size = settings.getExoPlayerCacheEntirePlayingEpisodeSizeInMB() * 1024 * 1024L
        val evictor = LeastRecentlyUsedCacheEvictor(size)
        SimpleCache(cacheDir, evictor, StandaloneDatabaseProvider(context))
    }.onFailure { e ->
        val errorMessage = "Failed to instantiate ExoPlayer cache ${e.message}"
        crashLogging.sendReport(Exception(errorMessage))
        LogBuffer.e(LogBuffer.TAG_PLAYBACK, errorMessage)
    }.getOrNull()

    private val httpFactory = if (FeatureFlag.isEnabled(Feature.EXO_OKHTTP)) {
        OkHttpDataSource.Factory(client)
            .setUserAgent(Settings.USER_AGENT_POCKETCASTS_SERVER)
    } else {
        DefaultHttpDataSource.Factory()
            .setUserAgent(Settings.USER_AGENT_POCKETCASTS_SERVER)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(sixtySeconds)
            .setReadTimeoutMs(sixtySeconds)
    }

    private val defaultFactory = DefaultDataSource.Factory(context, httpFactory)

    val cacheFactory get() = CacheDataSource.Factory()
        .setUpstreamDataSourceFactory(defaultFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        .let { if (cache != null) it.setCache(cache) else it }

    fun createMediaSource(
        episodeLocation: EpisodeLocation,
        clipRange: ClosedRange<Long>? = null,
    ): MediaSource? {
        val episodeUri = episodeLocation.uri ?: return null
        val mediaItem = MediaItem.Builder()
            .setUri(episodeUri)
            .let { builder ->
                if (clipRange != null) {
                    builder.setClippingConfiguration(clipRange.toClippingConfiguration())
                } else {
                    builder
                }
            }
            .setCustomCacheKey(episodeLocation.episode.uuid)
            .build()

        val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        if (settings.prioritizeSeekAccuracy.value) {
            extractorsFactory.setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        }

        val cacheFactory = cacheFactory
            .setCacheWriteDataSinkFactory(null)
            .takeIf { episodeLocation.episode.shouldUseCache() }
        if (cacheFactory != null) {
            CacheWorker.startCachingEntireEpisode(context, episodeUri.toString(), episodeLocation.episode.uuid)
        }

        val dataFactory = cacheFactory ?: defaultFactory
        return when {
            episodeLocation.episode.isHLS -> HlsMediaSource.Factory(dataFactory)
            (clipRange != null) -> DefaultMediaSourceFactory(dataFactory, extractorsFactory)
            else -> ProgressiveMediaSource.Factory(dataFactory, extractorsFactory)
        }.createMediaSource(mediaItem)
    }

    private fun BaseEpisode.shouldUseCache() = !isDownloaded && !isDownloading && settings.cacheEntirePlayingEpisode.value && FeatureFlag.isEnabled(Feature.CACHE_ENTIRE_PLAYING_EPISODE)

    private fun ClosedRange<Long>.toClippingConfiguration() = ClippingConfiguration.Builder()
        .setStartPositionMs(start)
        .setEndPositionMs(endInclusive)
        .build()

    private companion object {
        const val CACHE_DIR_NAME = "pocketcasts-exoplayer-cache"
        val sixtySeconds = 60.seconds.inWholeMilliseconds.toInt()
    }
}
