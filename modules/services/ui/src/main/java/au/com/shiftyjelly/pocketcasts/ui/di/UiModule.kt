package au.com.shiftyjelly.pocketcasts.ui.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.servers.di.Artwork
import coil.ImageLoader
import coil.disk.DiskCache
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
        @Artwork client: OkHttpClient,
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .okHttpClient(client)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("ImageCache"))
                    .build()
            }
            .build()
    }
}
