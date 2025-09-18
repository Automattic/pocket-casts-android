package au.com.shiftyjelly.pocketcasts.views.swipe

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareDialogFactory
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.DeleteState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = SwipeActionViewModel.Factory::class)
class SwipeActionViewModel @AssistedInject constructor(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playlistManager: PlaylistManager,
    private val userEpisodeManager: UserEpisodeManager,
    private val shareDialogFactory: ShareDialogFactory,
    private val addToPlaylistFragmentFactory: AddToPlaylistFragmentFactory,
    private val tracker: AnalyticsTracker,
    private val episodeAnalytics: EpisodeAnalytics,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @Assisted private val swipeSource: SwipeSource,
    @Assisted private val playlistUuid: String?,
) : ViewModel() {
    fun addToUpNextTop(episodeUuid: String) {
        trackAction(SwipeAction.AddToUpNextTop)

        // Using dispatcher because playback manager has some broken internal logic
        // and runs blocking code on the main thread.
        viewModelScope.launch {
            val episode = episodeManager.findEpisodeByUuid(episodeUuid) ?: return@launch
            playbackManager.playNext(episode, swipeSource.toSourceView())
        }
    }

    fun addToUpNextBottom(episodeUuid: String) {
        trackAction(SwipeAction.AddToUpNextBottom)

        // Using dispatcher because playback manager has some broken internal logic
        // and runs blocking code on the main thread.
        viewModelScope.launch(Dispatchers.IO) {
            val episode = episodeManager.findEpisodeByUuid(episodeUuid) ?: return@launch
            playbackManager.playLast(episode, swipeSource.toSourceView())
        }
    }

    fun removeFromUpNext(episodeUuid: String) {
        trackAction(SwipeAction.RemoveFromPlaylist)

        // Using dispatcher because playback manager has some broken internal logic
        // and runs blocking code on the main thread.
        viewModelScope.launch {
            val episode = episodeManager.findEpisodeByUuid(episodeUuid) ?: return@launch
            playbackManager.removeEpisode(episode, swipeSource.toSourceView())
        }
    }

    fun archive(episodeUuid: String) {
        trackAction(SwipeAction.Archive)

        viewModelScope.launch {
            val episode = episodeManager.findEpisodeByUuid(episodeUuid) as? PodcastEpisode ?: return@launch
            withContext(Dispatchers.IO) {
                episodeManager.archiveBlocking(episode, playbackManager)
            }
        }
    }

    fun unarchive(episodeUuid: String) {
        trackAction(SwipeAction.Unarchive)

        viewModelScope.launch {
            val episode = episodeManager.findEpisodeByUuid(episodeUuid) as? PodcastEpisode ?: return@launch
            withContext(Dispatchers.IO) {
                episodeManager.unarchiveBlocking(episode)
            }
        }
    }

    fun removeFromPlaylist(episodeUuid: String) {
        trackAction(SwipeAction.RemoveFromPlaylist)

        val playlistUuid = playlistUuid ?: return
        viewModelScope.launch {
            playlistManager.deleteManualEpisode(
                episodeUuid = episodeUuid,
                playlistUuid = playlistUuid,
            )
        }
    }

    // This function is suspending because it needs FragmentManager.
    //
    // If it was executed in view model's scope it could memory leaks if the manager was
    // retained in memory the underlying fragment gets destroyed.
    suspend fun shareEpisode(episodeUuid: String, fragmentManager: FragmentManager) {
        trackAction(SwipeAction.Share)

        val episode = episodeManager.findEpisodeByUuid(episodeUuid) as? PodcastEpisode ?: return
        val podcast = podcastManager.findPodcastByUuid(episode.podcastUuid) ?: return

        shareDialogFactory
            .shareEpisode(podcast, episode, SourceView.EPISODE_SWIPE_ACTION)
            .show(fragmentManager, "share_dialog")
    }

    // This function is suspending because it needs FragmentManager.
    //
    // If it was executed in view model's scope it could memory leaks if the manager was
    // retained in memory the underlying fragment gets destroyed.
    suspend fun deleteUserEpisode(episodeUuid: String, fragmentManager: FragmentManager) {
        trackAction(SwipeAction.DeleteUserEpisode)

        val episode = episodeManager.findEpisodeByUuid(episodeUuid) as? UserEpisode ?: return

        CloudDeleteHelper.getDeleteDialog(
            episode = episode,
            deleteState = CloudDeleteHelper.getDeleteState(episode),
            deleteFunction = { userEpisode, deleteState ->
                CloudDeleteHelper.deleteEpisode(
                    episode = userEpisode,
                    deleteState = deleteState,
                    playbackManager = playbackManager,
                    episodeManager = episodeManager,
                    userEpisodeManager = userEpisodeManager,
                    applicationScope = applicationScope,
                )
                episodeAnalytics.trackEvent(
                    event = if (deleteState == DeleteState.Cloud && !episode.isDownloaded) {
                        AnalyticsEvent.EPISODE_DELETED_FROM_CLOUD
                    } else {
                        AnalyticsEvent.EPISODE_DOWNLOAD_DELETED
                    },
                    source = SourceView.FILES,
                    uuid = episode.uuid,
                )
            },
            resources = context.resources,
        ).show(fragmentManager, "delete_confirm")
    }

    fun addToPlaylist(episodeUuid: String, fragmentManager: FragmentManager) {
        trackAction(SwipeAction.AddToPlaylist)

        if (fragmentManager.findFragmentByTag("add-to-playlist") == null) {
            val fragment = addToPlaylistFragmentFactory.create(
                source = AddToPlaylistFragmentFactory.Source.Swipe,
                episodeUuid = episodeUuid,
            )
            fragment.show(fragmentManager, "add-to-playlist")
        }
    }

    private fun trackAction(action: SwipeAction) {
        tracker.track(
            AnalyticsEvent.EPISODE_SWIPE_ACTION_PERFORMED,
            mapOf(
                "action" to action.analyticsValue,
                "source" to swipeSource.analyticsValue,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted swipeSource: SwipeSource,
            @Assisted playlistUuid: String?,
        ): SwipeActionViewModel
    }
}

suspend fun SwipeActionViewModel.handleAction(
    action: SwipeAction,
    episodeUuid: String,
    fragmentManager: FragmentManager,
) = when (action) {
    SwipeAction.AddToUpNextTop -> addToUpNextTop(episodeUuid)
    SwipeAction.AddToUpNextBottom -> addToUpNextBottom(episodeUuid)
    SwipeAction.RemoveFromUpNext -> removeFromUpNext(episodeUuid)
    SwipeAction.Share -> shareEpisode(episodeUuid, fragmentManager)
    SwipeAction.Archive -> archive(episodeUuid)
    SwipeAction.Unarchive -> unarchive(episodeUuid)
    SwipeAction.RemoveFromPlaylist -> removeFromPlaylist(episodeUuid)
    SwipeAction.DeleteUserEpisode -> deleteUserEpisode(episodeUuid, fragmentManager)
    SwipeAction.AddToPlaylist -> addToPlaylist(episodeUuid, fragmentManager)
}

private fun SwipeSource.toSourceView() = when (this) {
    SwipeSource.PodcastDetails -> SourceView.PODCAST_SCREEN
    SwipeSource.Filters -> SourceView.FILTERS
    SwipeSource.Downloads -> SourceView.DOWNLOADS
    SwipeSource.ListeningHistory -> SourceView.LISTENING_HISTORY
    SwipeSource.Starred -> SourceView.STARRED
    SwipeSource.Files -> SourceView.FILES
    SwipeSource.UpNext -> SourceView.UP_NEXT
}
