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
                    .build()
            }
            .build()
    }
}
