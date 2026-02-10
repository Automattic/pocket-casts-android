package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import android.os.Build
import android.os.Vibrator
import au.com.shiftyjelly.pocketcasts.servers.di.Artwork
import au.com.shiftyjelly.pocketcasts.ui.di.WearImageLoader
import au.com.shiftyjelly.pocketcasts.wear.ui.AppConfig
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.media3.audio.AudioOutputSelector
import com.google.android.horologist.media3.audio.BluetoothSettingsOutputSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {
    @Singleton
    @Provides
    @IsEmulator
    fun isEmulator() = listOf(Build.PRODUCT, Build.MODEL).any { it.startsWith("sdk_gwear") }

    @Singleton
    @Provides
    fun appConfig(): AppConfig = AppConfig()

    @Singleton
    @Provides
    fun audioOutputSelector(
        systemAudioRepository: SystemAudioRepository,
    ): AudioOutputSelector = BluetoothSettingsOutputSelector(systemAudioRepository)

    @Singleton
    @Provides
    fun systemAudioRepository(
        @ApplicationContext application: Context,
    ): SystemAudioRepository = SystemAudioRepository.fromContext(application)

    @Singleton
    @Provides
    fun vibrator(
        @ApplicationContext application: Context,
    ): Vibrator = application.getSystemService(Vibrator::class.java)

    /**
     * Wear-optimized ImageLoader with smaller caches for limited Wear OS resources.
     * Memory: 10MB, Disk: 50MB (vs 25% RAM / 250MB default).
     */
    @Singleton
    @Provides
    @WearImageLoader
    fun wearImageLoader(
        @ApplicationContext context: Context,
        @Artwork httpClient: dagger.Lazy<OkHttpClient>,
    ): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = { httpClient.get() },
                ),
            )
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizeBytes(10 * 1024 * 1024)
                .weakReferencesEnabled(true)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("WearImageCache"))
                .maxSizeBytes(50 * 1024 * 1024)
                .build()
        }
        .apply {
            if (au.com.shiftyjelly.pocketcasts.BuildConfig.DEBUG) {
                logger(DebugLogger())
            }
        }
        .build()
}
