package au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@EntryPoint
interface TaskerEntryPoint {
    fun getTheme(): Theme
    fun getSettings(): Settings
    fun getPlaybackManager(): PlaybackManager
    fun getPodcastManager(): PodcastManager
    fun getEpisodeManager(): EpisodeManager
    fun getPlaylistManager(): PlaylistManager
}

val Context.appTheme get() = EntryPointAccessors.fromApplication(applicationContext, TaskerEntryPoint::class.java).getTheme()
val Context.playbackManager get() = EntryPointAccessors.fromApplication(applicationContext, TaskerEntryPoint::class.java).getPlaybackManager()
val Context.playlistManager get() = EntryPointAccessors.fromApplication(applicationContext, TaskerEntryPoint::class.java).getPlaylistManager()
val Context.episodeManager get() = EntryPointAccessors.fromApplication(applicationContext, TaskerEntryPoint::class.java).getEpisodeManager()
val Context.podcastManager get() = EntryPointAccessors.fromApplication(applicationContext, TaskerEntryPoint::class.java).getPodcastManager()
val Context.settings get() = EntryPointAccessors.fromApplication(applicationContext, TaskerEntryPoint::class.java).getSettings()
