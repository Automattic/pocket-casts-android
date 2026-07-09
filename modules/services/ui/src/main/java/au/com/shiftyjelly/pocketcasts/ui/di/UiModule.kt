package au.com.shiftyjelly.pocketcasts.ui.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.servers.di.Artwork
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
                    // Coil's default size is 2% of the free disk space with a 10MB floor, which on
                    // devices low on storage evicts artwork almost immediately.
                    .maxSizeBytes(ARTWORK_DISK_CACHE_SIZE_BYTES)
                    .build()
            }
            .build()
    }

    private companion object {
        // Shared by all images the app loads, not just podcast covers. Roughly enough for a few
        // hundred podcasts at three artwork sizes each (~200KB per image), and matching the
        // ceiling Coil would apply on a device with plenty of free space.
        const val ARTWORK_DISK_CACHE_SIZE_BYTES = 250L * 1024 * 1024
    }
}
