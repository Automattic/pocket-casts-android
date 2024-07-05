package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationBroadcastReceiver : BroadcastReceiver(), CoroutineScope {

    @Inject lateinit var settings: Settings

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var episodeManager: EpisodeManager

    @Inject lateinit var downloadManager: DownloadManager

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var episodeAnalytics: EpisodeAnalytics

    private val source = SourceView.NOTIFICATION

    companion object {

        const val NOTIFICATION_ID = 541251
        const val NOTIFICATION_TAG_NEW_EPISODES_PREFIX = "au.com.shiftyjelly.pocketcasts.NEW_EPISODE_NOTIFICATION_"
        const val NOTIFICATION_TAG_NEW_EPISODES_PRIMARY = "au.com.shiftyjelly.pocketcasts.NEW_EPISODE_NOTIFICATION_PRIMARY"
        const val NOTIFICATION_TAG_PLAYBACK_ERROR = "au.com.shiftyjelly.pocketcasts.PLAYBACK_ERROR"

        const val INTENT_ACTION_NOTIFICATION_DELETED = "au.com.shiftyjelly.pocketcasts.action.NOTIFICATION_DELETED"

        const val INTENT_ACTION_PLAY_EPISODE = "au.com.shiftyjelly.pocketcasts.action.NOTIFICATION_PLAY_EPISODE"
        const val INTENT_ACTION_DOWNLOAD_EPISODE = "au.com.shiftyjelly.pocketcasts.action.NOTIFICATION_DOWNLOAD_EPISODE"
        const val INTENT_ACTION_PLAY_LAST = "au.com.shiftyjelly.pocketcasts.action.INTENT_ACTION_PLAY_LAST"
        const val INTENT_ACTION_PLAY_NEXT = "au.com.shiftyjelly.pocketcasts.action.NOTIFICATION_PLAY_NEXT"
        const val INTENT_ACTION_MARK_AS_PLAYED = "au.com.shiftyjelly.pocketcasts.action.NOTIFICATION_MARK_AS_PLAYED"
        const val INTENT_ACTION_STREAM_EPISODE_FROM_STREAM_WARNING = "au.com.shiftyjelly.pocketcasts.action.NOTIFICATION_STREAM_EPISODE"
        const val INTENT_ACTION_ARCHIVE = "au.com.shiftyjelly.pocketcasts.action.INTENT_ACTION_ARCHIVE"
        const val INTENT_ACTION_PLAY_DOWNLOADED = "au.com.shiftyjelly.pocketcasts.action.PLAY_DOWNLOADED"

        const val INTENT_EXTRA_ACTION = "EXTRA_ACTION"
        const val INTENT_EXTRA_NOTIFICATION_TAG = "NOTIFICATION_TAG"
        const val INTENT_EXTRA_EPISODE_UUID = "EPISODE_UUID"
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return

        val episodeUuid = bundle.getString(INTENT_EXTRA_EPISODE_UUID, null)
        if (episodeUuid.isNullOrBlank()) {
            return
        }

        // remove the notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationTag = bundle.getString(INTENT_EXTRA_NOTIFICATION_TAG, null)
        if (notificationTag != null && notificationTag.isNotBlank()) {
            manager.cancel(notificationTag, NOTIFICATION_ID)
        }

        val extraAction = bundle.getString(INTENT_EXTRA_ACTION, null) ?: return

        if (extraAction == INTENT_ACTION_PLAY_EPISODE) {
            playNow(episodeUuid, notificationTag == NOTIFICATION_TAG_PLAYBACK_ERROR)
        } else if (extraAction == INTENT_ACTION_STREAM_EPISODE_FROM_STREAM_WARNING) {
            playNow(episodeUuid, showedStreamWarning = true, forceStream = notificationTag == NOTIFICATION_TAG_PLAYBACK_ERROR)
        } else if (extraAction == INTENT_ACTION_PLAY_LAST) {
            playLast(episodeUuid, notificationTag == NOTIFICATION_TAG_PLAYBACK_ERROR)
        } else if (extraAction == INTENT_ACTION_PLAY_NEXT) {
            playNext(episodeUuid)
        } else if (extraAction == INTENT_ACTION_MARK_AS_PLAYED) {
            markAsPlayed(episodeUuid)
        } else if (extraAction == INTENT_ACTION_DOWNLOAD_EPISODE) {
            downloadEpisode(episodeUuid)
        } else if (extraAction == INTENT_ACTION_ARCHIVE) {
            archiveEpisode(episodeUuid)
        } else if (extraAction == INTENT_ACTION_PLAY_DOWNLOADED) {
            playDownloaded()
        }
    }

    private fun playNow(episodeUuid: String, forceStream: Boolean, showedStreamWarning: Boolean = false) {
        launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                playbackManager.playNow(
                    episode = episode,
                    showedStreamWarning = showedStreamWarning,
                    forceStream = forceStream,
                    sourceView = source,
                )
            }
        }
    }

    private fun downloadEpisode(episodeUuid: String) {
        launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                DownloadHelper.manuallyDownloadEpisodeNow(episode, "download from intent", downloadManager, episodeManager, source = source)
            }
        }
    }

    private fun markAsPlayed(episodeUuid: String) {
        launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
                episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_MARKED_AS_PLAYED, source, episodeUuid)
            }
        }
    }

    private fun archiveEpisode(episodeUuid: String) {
        launch {
            episodeManager.findByUuid(episodeUuid)?.let { episode ->
                episodeManager.archive(episode, playbackManager, true)
                episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_ARCHIVED, source, episodeUuid)
            }
        }
    }

    private fun playNext(episodeUuid: String) {
        launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                playbackManager.playNext(episode = episode, source = source)
            }
        }
    }

    private fun playLast(episodeUuid: String, playNext: Boolean) {
        launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                playbackManager.playLast(episode = episode, source = source)
                if (playNext) {
                    playbackManager.playNextInQueue(sourceView = source)
                }
            }
        }
    }

    private fun playDownloaded() {
        launch {
            val downloadedIndex = playbackManager.upNextQueue.queueEpisodes.indexOfFirst { it.isDownloaded }
            if (downloadedIndex >= 0) {
                val downloadedEpisode = playbackManager.upNextQueue.queueEpisodes[downloadedIndex]
                val undownloadedEpisodes = playbackManager.upNextQueue.queueEpisodes.subList(0, downloadedIndex)
                playbackManager.upNextQueue.currentEpisode?.let {
                    playbackManager.playEpisodesLast(episodes = listOf(it) + undownloadedEpisodes, source = source)
                }

                playbackManager.playNow(downloadedEpisode)
            }
        }
    }
}
