package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareActionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WearAppModule {

    @Provides
    fun connectivityManager(
        @ApplicationContext application: Context,
    ): ConnectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    fun shareActionProvider() = object : ShareActionProvider {
        override fun clipAction(podcastEpisode: PodcastEpisode, podcast: Podcast, fragmentManager: FragmentManager, source: SourceView) = Unit
    }
}
