package au.com.shiftyjelly.pocketcasts.podcasts.helper

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayButtonListener @Inject constructor(
    val downloadManager: DownloadManager,
    val episodeManager: EpisodeManager,
    val playbackManager: PlaybackManager,
    val settings: Settings,
    private val warningsHelper: WarningsHelper,
    @ActivityContext private val activity: Context,
    private val episodeAnalytics: EpisodeAnalytics,
) : PlayButton.OnClickListener, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override var source = SourceView.UNKNOWN

    override fun onPlayClicked(episodeUuid: String) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "In app play button pushed for $episodeUuid")
        launch {
            // show the stream warning if the episode isn't downloaded
            episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                if (playbackManager.shouldWarnAboutPlayback(episodeUuid)) {
                    launch(Dispatchers.Main) {
                        if (episode.isDownloaded) {
                            play(episode)
                        } else if (activity is AppCompatActivity) {
                            warningsHelper.streamingWarningDialog(episode = episode, sourceView = source)
                                .show(activity.supportFragmentManager, "streaming dialog")
                        }
                    }
                } else {
                    play(episode)
                }
            }
        }
    }

    private fun play(episode: BaseEpisode, force: Boolean = true) {
        playbackManager.playNow(episode = episode, forceStream = force, sourceView = source)
        warningsHelper.showBatteryWarningSnackbarIfAppropriate()
    }

    override fun onPauseClicked() {
        playbackManager.pause(sourceView = source)
    }

    override fun onPlayedClicked(episodeUuid: String) {
        launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                episodeManager.markAsNotPlayed(episode)
            }
        }
    }

    override fun onPlayNext(episodeUuid: String) {
        launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                playbackManager.playNext(episode = episode, source = source)
            }
        }
    }

    override fun onPlayLast(episodeUuid: String) {
        launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                playbackManager.playLast(episode = episode, source = source)
            }
        }
    }

    override fun onDownload(episodeUuid: String) {
        if (settings.warnOnMeteredNetwork.value && !Network.isUnmeteredConnection(activity) && activity is AppCompatActivity) {
            warningsHelper.downloadWarning(episodeUuid, "play button")
                .show(activity.supportFragmentManager, "download warning")
        } else {
            download(episodeUuid, waitForWifi = settings.warnOnMeteredNetwork.value)
        }
    }

    private fun download(episodeUuid: String, waitForWifi: Boolean = false) {
        launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let {
                if (it.isDownloading) {
                    episodeManager.stopDownloadAndCleanUp(episodeUuid, "play button")
                } else if (!it.isDownloaded) {
                    if (!waitForWifi) {
                        it.autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI
                    } else {
                        it.autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED
                    }
                    downloadManager.addEpisodeToQueue(it, "play button", fireEvent = true, fireToast = false)
                    episodeAnalytics.trackEvent(
                        AnalyticsEvent.EPISODE_DOWNLOAD_QUEUED,
                        source = source,
                        uuid = episodeUuid,
                    )
                    launch {
                        episodeManager.unarchive(it)
                    }
                }
            }
        }
    }

    override fun onStopDownloading(episodeUuid: String) {
        launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let {
                if (it.isDownloading || it.isQueued) {
                    downloadManager.removeEpisodeFromQueue(it, "play button")
                }
            }
        }
    }
}
