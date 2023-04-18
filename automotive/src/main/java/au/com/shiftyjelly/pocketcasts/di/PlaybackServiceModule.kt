package au.com.shiftyjelly.pocketcasts.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import au.com.shiftyjelly.pocketcasts.AutoPlaybackService.AutoMediaLibrarySessionCallback
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope

@UnstableApi
@Module
@InstallIn(ServiceComponent::class)
object PlaybackServiceModule {
    @ServiceScoped
    @Provides
    fun librarySessionCallback(
        @ApplicationContext context: Context,
        episodeManager: EpisodeManager,
        folderManager: FolderManager,
        listRepository: ListRepository,
        playbackManager: PlaybackManager,
        playlistManager: PlaylistManager,
        podcastManager: PodcastManager,
        serverManager: ServerManager,
        @ServiceScoped serviceCoroutineScope: CoroutineScope,
        settings: Settings,
        subscriptionManager: SubscriptionManager,
        userEpisodeManager: UserEpisodeManager,
    ): MediaLibraryService.MediaLibrarySession.Callback =
        AutoMediaLibrarySessionCallback(
            context = context,
            episodeManager = episodeManager,
            folderManager = folderManager,
            listRepository = listRepository,
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
