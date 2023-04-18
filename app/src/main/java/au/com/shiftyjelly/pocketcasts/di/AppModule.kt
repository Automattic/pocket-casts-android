package au.com.shiftyjelly.pocketcasts.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadCallFactory
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadOkHttpClient
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadRequestBuilder
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackService
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Singleton

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

    @UnstableApi
    @ServiceScoped
    @Provides
    fun librarySessionCallback(
        @ApplicationContext context: Context,
        episodeManager: EpisodeManager,
        folderManager: FolderManager,
        playbackManager: PlaybackManager,
        playlistManager: PlaylistManager,
        podcastManager: PodcastManager,
        serverManager: ServerManager,
        @ServiceScoped serviceCoroutineScope: CoroutineScope,
        settings: Settings,
        subscriptionManager: SubscriptionManager,
        userEpisodeManager: UserEpisodeManager,
    ): MediaLibraryService.MediaLibrarySession.Callback =
        PlaybackService.CustomMediaLibrarySessionCallback(
            context = context,
            episodeManager = episodeManager,
            folderManager = folderManager,
            playbackManager = playbackManager,
            playlistManager = playlistManager,
            podcastManager = podcastManager,
            serverManager = serverManager,
            serviceScope = serviceCoroutineScope,
            settings = settings,
            subscriptionManager = subscriptionManager,
            userEpisodeManager = userEpisodeManager,
        )
}
