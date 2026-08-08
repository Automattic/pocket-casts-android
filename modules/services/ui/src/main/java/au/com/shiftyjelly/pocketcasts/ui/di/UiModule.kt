package au.com.shiftyjelly.pocketcasts.ui.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.servers.di.Artwork
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import coil3.ImageLoader
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
                    ),
                )
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("ImageCache"))
                    .apply {
                        // Only the phone app schedules the artwork-healing worker. Keep Coil's
                        // adaptive default on storage-constrained Wear, Automotive, and TV devices.
                        if (Util.getAppPlatform(context) == AppPlatform.Phone && !Util.isTv(context)) {
                            // Raise Coil's free-space-relative default so artwork isn't immediately
                            // evicted on phones under storage pressure. This is an LRU ceiling only.
                            maxSizeBytes(ARTWORK_DISK_CACHE_SIZE_BYTES)
                        }
                    }
                    .build()
            }
            .build()
    }

    private companion object {
        // Shared by all images the phone app loads, not just podcast covers. Roughly enough for a
        // few hundred podcasts at three artwork sizes each (~200KB per image), and matching the
        // ceiling Coil would apply on a device with plenty of free space.
        const val ARTWORK_DISK_CACHE_SIZE_BYTES = 250L * 1024 * 1024
    }
}
