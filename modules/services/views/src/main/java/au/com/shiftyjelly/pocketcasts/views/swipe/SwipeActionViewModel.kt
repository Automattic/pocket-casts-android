package au.com.shiftyjelly.pocketcasts.views.swipe

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareDialogFactory
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.DeleteState
import com.automattic.eventhorizon.EpisodeDeletedFromCloudEvent
import com.automattic.eventhorizon.EpisodeRemovedFromListEvent
import com.automattic.eventhorizon.EpisodeSwipeActionPerformedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PlaylistRemoveEpisodeSource
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
    private val downloadQueue: DownloadQueue,
    private val addToPlaylistFragmentFactory: AddToPlaylistFragmentFactory,
    private val eventHorizon: EventHorizon,
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
                episodeManager.disableAutoDownload(episode)
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

    fun removeFromPlaylist(episodeUuid: String, podcastUuid: String) {
        trackAction(SwipeAction.RemoveFromPlaylist)

        val playlistUuid = playlistUuid ?: return
        viewModelScope.launch {
            playlistManager.deleteManualEpisode(
                episodeUuid = episodeUuid,
                playlistUuid = playlistUuid,
            )
            val playlistName = playlistManager.findPlaylistPreview(playlistUuid)?.title
            eventHorizon.track(
                EpisodeRemovedFromListEvent(
                    playlistName = playlistName ?: Tracker.INVALID_OR_NULL_VALUE,
                    playlistUuid = playlistUuid,
                    episodeUuid = episodeUuid,
                    podcastUuid = podcastUuid,
                    source = PlaylistRemoveEpisodeSource.SwipeRemove,
                ),
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
            .shareEpisode(podcast, episode, swipeSource.toSourceView())
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
                    sourceView = swipeSource.toSourceView(),
                    downloadQueue = downloadQueue,
                    playbackManager = playbackManager,
                    userEpisodeManager = userEpisodeManager,
                    applicationScope = applicationScope,
                )
                if (deleteState == DeleteState.Cloud && !episode.isDownloaded) {
                    eventHorizon.track(
                        EpisodeDeletedFromCloudEvent(
                            episodeUuid = episode.uuid,
                            source = swipeSource.toSourceView().eventHorizonValue,
                        ),
                    )
                }
                viewModelScope.launch {
                    episodeManager.disableAutoDownload(episode)
                }
            },
            resources = context.resources,
        ).show(fragmentManager, "delete_confirm")
    }

    fun addToPlaylist(episodeUuid: String, podcastUuid: String, fragmentManager: FragmentManager) {
        trackAction(SwipeAction.AddToPlaylist)

        if (fragmentManager.findFragmentByTag("add-to-playlist") == null) {
            val fragment = addToPlaylistFragmentFactory.create(
                source = AddToPlaylistFragmentFactory.Source.Swipe,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            )
            fragment.show(fragmentManager, "add-to-playlist")
        }
    }

    private fun trackAction(action: SwipeAction) {
        eventHorizon.track(
            EpisodeSwipeActionPerformedEvent(
                source = swipeSource.eventHorizonValue,
                action = action.eventHorizonValue,
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
    podcastUuid: String,
    fragmentManager: FragmentManager,
) = when (action) {
    SwipeAction.AddToUpNextTop -> addToUpNextTop(
        episodeUuid = episodeUuid,
    )

    SwipeAction.AddToUpNextBottom -> addToUpNextBottom(episodeUuid = episodeUuid)

    SwipeAction.RemoveFromUpNext -> removeFromUpNext(episodeUuid = episodeUuid)

    SwipeAction.Share -> shareEpisode(
        episodeUuid = episodeUuid,
        fragmentManager = fragmentManager,
    )

    SwipeAction.Archive -> archive(episodeUuid = episodeUuid)

    SwipeAction.Unarchive -> unarchive(episodeUuid = episodeUuid)

    SwipeAction.RemoveFromPlaylist -> removeFromPlaylist(
        episodeUuid = episodeUuid,
        podcastUuid = podcastUuid,
    )

    SwipeAction.DeleteUserEpisode -> deleteUserEpisode(
        episodeUuid = episodeUuid,
        fragmentManager = fragmentManager,
    )

    SwipeAction.AddToPlaylist -> addToPlaylist(
        episodeUuid = episodeUuid,
        podcastUuid = podcastUuid,
        fragmentManager = fragmentManager,
    )
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
