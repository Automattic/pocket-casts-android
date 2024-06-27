package au.com.shiftyjelly.pocketcasts.clip

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClipModule {
    @Provides
    @Singleton
    @ClipSimpleCache
    @OptIn(UnstableApi::class)
    fun simpleCache(@ApplicationContext context: Context) = SimpleCache(
        File(context.cacheDir, "pocketcasts-exoplayer-clips-cache"),
        LeastRecentlyUsedCacheEvictor(25 * 1024 * 1024L),
        StandaloneDatabaseProvider(context),
    )
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClipSimpleCache
