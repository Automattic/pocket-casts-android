package au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@EntryPoint
interface ThemeEntryPoint {
    fun getTheme(): Theme
}
@InstallIn(SingletonComponent::class)
@EntryPoint
interface PlaybackManagerEntryPoint {
    fun getPlaybackManager(): PlaybackManager
}

@InstallIn(SingletonComponent::class)
@EntryPoint
interface PlaylistManagerEntryPoint {
    fun getPlaylistManager(): PlaylistManager
}

@InstallIn(SingletonComponent::class)
@EntryPoint
interface EpisodeManagerEntryPoint {
    fun getEpisodeManager(): EpisodeManager
}

@InstallIn(SingletonComponent::class)
@EntryPoint
interface SettingsEntryPoint {
    fun getSettings(): Settings
}
@InstallIn(SingletonComponent::class)
@EntryPoint
interface PodcastManagerEntryPoint {
    fun getPodcastManager(): PodcastManager
}
@InstallIn(SingletonComponent::class)
@EntryPoint
interface DownloadManagerEntryPoint {
    fun getDownloadManager(): DownloadManager
}

val Context.appTheme get() = EntryPointAccessors.fromApplication(applicationContext, ThemeEntryPoint::class.java).getTheme()
val Context.playbackManager get() = EntryPointAccessors.fromApplication(applicationContext, PlaybackManagerEntryPoint::class.java).getPlaybackManager()
val Context.playlistManager get() = EntryPointAccessors.fromApplication(applicationContext, PlaylistManagerEntryPoint::class.java).getPlaylistManager()
val Context.episodeManager get() = EntryPointAccessors.fromApplication(applicationContext, EpisodeManagerEntryPoint::class.java).getEpisodeManager()
val Context.podcastManager get() = EntryPointAccessors.fromApplication(applicationContext, PodcastManagerEntryPoint::class.java).getPodcastManager()
val Context.downloadManager get() = EntryPointAccessors.fromApplication(applicationContext, DownloadManagerEntryPoint::class.java).getDownloadManager()
val Context.settings get() = EntryPointAccessors.fromApplication(applicationContext, SettingsEntryPoint::class.java).getSettings()
