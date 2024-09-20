package au.com.shiftyjelly.pocketcasts.reimagine.di

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.reimagine.ShareDialogFragment
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareDialogFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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
}
