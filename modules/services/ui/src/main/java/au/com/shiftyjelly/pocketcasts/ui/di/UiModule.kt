package au.com.shiftyjelly.pocketcasts.ui.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.servers.di.Artwork
import au.com.shiftyjelly.pocketcasts.ui.images.ArtworkCacheStrategy
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
class UiModule {

    @OptIn(ExperimentalCoilApi::class)
    @Provides
    @Singleton
    internal fun provideCoilImageLoader(
        @ApplicationContext context: Context,
        @Artwork httpClient: Lazy<OkHttpClient>,
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { httpClient.get() },
                        cacheStrategy = { ArtworkCacheStrategy() },
                    ),
                )
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("ImageCache"))
                    .apply {
                        // Coil's 2% default falls to its 10MB floor on a full phone, which evicts artwork almost immediately.
                        if (Util.getAppPlatform(context) == AppPlatform.Phone && !Util.isTv(context)) {
                            maxSizePercent(ARTWORK_DISK_CACHE_FREE_SPACE_PERCENT)
                            minimumMaxSizeBytes(ARTWORK_DISK_CACHE_MINIMUM_BYTES)
                        }
                    }
                    .build()
            }
            .build()
    }

    private companion object {
        const val ARTWORK_DISK_CACHE_FREE_SPACE_PERCENT = 0.05
        const val ARTWORK_DISK_CACHE_MINIMUM_BYTES = 64L * 1024 * 1024
    }
}
