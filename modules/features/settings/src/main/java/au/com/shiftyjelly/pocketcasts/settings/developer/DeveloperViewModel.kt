package au.com.shiftyjelly.pocketcasts.settings.developer

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.UpdateEpisodeDetailsTask
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.stream.Collectors.toList
import javax.inject.Inject

@HiltViewModel
class DeveloperViewModel
@Inject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val disposables = CompositeDisposable()

    fun forceRefresh() {
        podcastManager.refreshPodcasts(fromLog = "dev")
        Toast.makeText(context, "Refresh started", Toast.LENGTH_LONG).show()
    }

    fun triggerNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val podcasts = podcastManager.findSubscribed()
                val countNotificationsOn = podcasts.count { it.isShowNotifications }
                if (countNotificationsOn == 0) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No notifications turned on", Toast.LENGTH_LONG).show()
                    }
                } else {
                    for (podcast in podcasts) {
                        if (podcast.isShowNotifications) {
                            // find first podcast with more than one episode
                            val episodes = episodeManager.findEpisodesByPodcastOrderedByPublishDate(podcast)
                            if (episodes.size <= 1) {
                                continue
                            } else {
                                val episode = episodes[1]
                                // link the second oldest to the podcast
                                episodeManager.markAsNotPlayed(episode)
                                podcastManager.updateLatestEpisode(podcast, episode)
                                // remove the latest episode
                                val episodeToDelete = episodes[0]
                                Timber.i("Creating a notification for ${podcast.title} - ${episodeToDelete.title}")
                                episodeManager.deleteEpisodeWithoutSync(episodeToDelete, playbackManager)
                                settings.setNotificationLastSeenToNow()
                                continue
                            }
                        }
                    }
                    forceRefresh()
                }
            } catch (e: Exception) {
                Timber.e(e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to trigger notifications", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun deleteFirstEpisode() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val podcasts = podcastManager.findSubscribed()
                for (podcast in podcasts) {
                    // find first podcast with more than one episode
                    val episodes = episodeManager.findEpisodesByPodcastOrderedByPublishDate(podcast)
                    if (episodes.size <= 1) {
                        continue
                    } else {
                        // remove the latest episode
                        val episodeToDelete = episodes.first()
                        val newLatest = episodes.getOrNull(1)
                        Timber.i("Deleted episode ${podcast.title} - ${episodeToDelete.title}")
                        episodeManager.deleteEpisodeWithoutSync(episodeToDelete, playbackManager)

                        podcast.latestEpisodeUuid = newLatest?.uuid
                        podcast.latestEpisodeDate = newLatest?.publishedDate
                        podcastManager.updatePodcast(podcast)

                        continue
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Episodes deleted", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Timber.e(e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to delete episodes", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    @OptIn(FlowPreview::class)
    fun triggerUpdateEpisodeDetails() {
        viewModelScope.launch {
            try {
                val episodes = podcastManager.findSubscribedNoOrder()
                    .shuffled()
                    .asFlow()
                    .flatMapConcat {
                        episodeManager.findEpisodesByPodcastOrderedSuspend(it).asFlow()
                    }
                    .take(5)
                    .toList()
                if (episodes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No episodes found, subscribe to some podcasts", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                UpdateEpisodeDetailsTask.enqueue(episodes, context.applicationContext)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Running update episode details", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Timber.e(e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to run update episode details", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
