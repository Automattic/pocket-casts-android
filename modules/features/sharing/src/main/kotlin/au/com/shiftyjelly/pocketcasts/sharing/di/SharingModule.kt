package au.com.shiftyjelly.pocketcasts.sharing.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.FFmpegMediaService
import au.com.shiftyjelly.pocketcasts.sharing.ShareDialogFragment
import au.com.shiftyjelly.pocketcasts.sharing.SharingAnalytics
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingLogger
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareDialogFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
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

    @Provides
    fun provideSharingClient(
        @ApplicationContext context: Context,
        listeners: Set<@JvmSuppressWildcards SharingClient.Listener>,
    ): SharingClient = SharingClient(
        context,
        FFmpegMediaService(context),
        listeners,
    )

    @Provides
    @IntoSet
    fun provideAnalyticsListener(
        analyticsTracker: AnalyticsTracker,
    ): SharingClient.Listener = SharingAnalytics(analyticsTracker)

    @Provides
    @IntoSet
    fun provideLoggingListener(): SharingClient.Listener = SharingLogger()
}
