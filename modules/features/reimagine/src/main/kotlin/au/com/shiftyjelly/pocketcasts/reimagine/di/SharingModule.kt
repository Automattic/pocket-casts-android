package au.com.shiftyjelly.pocketcasts.reimagine.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.reimagine.ShareDialogFragment
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareDialogFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharingModule {
    @Provides
    fun provideShareDialogFactory(): ShareDialogFactory = object : ShareDialogFactory {
        override fun shareEpisode(podcast: Podcast, episode: PodcastEpisode, source: SourceView) =
            ShareDialogFragment.newInstance(
                podcast,
                episode,
                source,
                options = listOf(ShareDialogFragment.Options.Episode),
            )
    }

    @Provides
    @Singleton
    @ClipSimpleCache
    @OptIn(UnstableApi::class)
    fun simpleCache(@ApplicationContext context: Context): SimpleCache = SimpleCache(
        File(context.cacheDir, "pocketcasts-exoplayer-clips-cache"),
        LeastRecentlyUsedCacheEvictor(25 * 1024 * 1024L),
        StandaloneDatabaseProvider(context),
    )
}
