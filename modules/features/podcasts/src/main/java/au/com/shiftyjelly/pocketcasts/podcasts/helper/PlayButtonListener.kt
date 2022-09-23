package au.com.shiftyjelly.pocketcasts.podcasts.helper

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class PlayButtonListener @Inject constructor(
    val downloadManager: DownloadManager,
    val episodeManager: EpisodeManager,
    val playbackManager: PlaybackManager,
    val settings: Settings,
    private val warningsHelper: WarningsHelper,
    @ActivityContext private val activity: Context,
) : PlayButton.OnClickListener, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override var playbackSource = PlaybackManager.PlaybackSource.UNKNOWN

    override fun onPlayClicked(episodeUuid: String) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "In app play button pushed for $episodeUuid")
        launch {
            // show the stream warning if the episode isn't downloaded
            episodeManager.findPlayableByUuid(episodeUuid)?.let { episode ->
                if (playbackManager.shouldWarnAboutPlayback(episodeUuid)) {
                    launch(Dispatchers.Main) {
                        if (episode.isDownloaded) {
                            play(episode)
                        } else if (activity is AppCompatActivity) {
                            warningsHelper.streamingWarningDialog(episode)
                                .show(activity.supportFragmentManager, "streaming dialog")
                        }
                    }
                } else {
                    play(episode)
                }
            }
        }
    }

    private fun play(episode: Playable, force: Boolean = true) {
        playbackManager.playNow(episode, force)
        warningsHelper.showBatteryWarningSnackbarIfAppropriate()
        playbackManager.playbackSource = playbackSource
    }

    override fun onPauseClicked() {
        playbackManager.pause()
        playbackManager.playbackSource = playbackSource
    }

    override fun onPlayedClicked(episodeUuid: String) {
        launch {
            episodeManager.findPlayableByUuid(episodeUuid)?.let { episode ->
                episodeManager.markAsNotPlayed(episode)
            }
        }
    }

    override fun onPlayNext(episodeUuid: String) {
        launch {
            episodeManager.findPlayableByUuid(episodeUuid)?.let { episode ->
                playbackManager.playNext(episode)
            }
        }
    }

    override fun onPlayLast(episodeUuid: String) {
        launch {
            episodeManager.findPlayableByUuid(episodeUuid)?.let { episode ->
                playbackManager.playLast(episode)
            }
        }
    }

    override fun onDownload(episodeUuid: String) {
        if (settings.warnOnMeteredNetwork() && !Network.isUnmeteredConnection(activity) && activity is AppCompatActivity) {
            warningsHelper.downloadWarning(episodeUuid, "play button")
                .show(activity.supportFragmentManager, "download warning")
        } else {
            download(episodeUuid, waitForWifi = settings.warnOnMeteredNetwork())
        }
    }

    private fun download(episodeUuid: String, waitForWifi: Boolean = false) {
        launch {
            episodeManager.findPlayableByUuid(episodeUuid)?.let {
                if (it.isDownloading) {
                    episodeManager.stopDownloadAndCleanUp(episodeUuid, "play button")
                } else if (!it.isDownloaded) {
                    if (!waitForWifi) {
                        it.autoDownloadStatus = Episode.AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI
                    } else {
                        it.autoDownloadStatus = Episode.AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED
                    }
                    downloadManager.addEpisodeToQueue(it, "play button", true)
                    launch {
                        episodeManager.unarchive(it)
                    }
                }
            }
        }
    }

    override fun onStopDownloading(episodeUuid: String) {
        launch {
            episodeManager.findPlayableByUuid(episodeUuid)?.let {
                if (it.isDownloading || it.isQueued) {
                    downloadManager.removeEpisodeFromQueue(it, "play button")
                }
            }
        }
    }
}
