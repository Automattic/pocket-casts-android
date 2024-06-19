package au.com.shiftyjelly.pocketcasts.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.clip.ShareClipFragment
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadCallFactory
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadOkHttpClient
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadRequestBuilder
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareActionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun connectivityManager(@ApplicationContext application: Context): ConnectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    @Singleton
    @DownloadCallFactory
    fun downloadCallFactory(
        @DownloadOkHttpClient phoneCallFactory: OkHttpClient,
    ): Call.Factory = phoneCallFactory

    @Provides
    @DownloadRequestBuilder
    fun downloadRequestBuilder(): Request.Builder = Request.Builder()

    @Provides
    fun shareActionProvider() = object : ShareActionProvider {
        override fun clipAction(podcastEpisode: PodcastEpisode, podcast: Podcast, fragmentManager: FragmentManager) {
            ShareClipFragment.newInstance(podcastEpisode.uuid, podcast.backgroundColor).show(fragmentManager, "share_clip")
        }
    }
}
