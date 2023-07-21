package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwipeButtonLayoutViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
    private val episodeAnalytics: EpisodeAnalytics,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val userEpisodeManager: UserEpisodeManager,
) : ViewModel() {

    fun share(
        episode: PodcastEpisode,
        fragmentManager: FragmentManager,
        context: Context,
        swipeSource: EpisodeItemTouchHelper.SwipeSource
    ) {

        viewModelScope.launch(Dispatchers.Default) {

            trackSwipeAction(
                swipeSource = swipeSource,
                swipeAction = EpisodeItemTouchHelper.SwipeAction.SHARE,
            )

            val podcast = podcastManager.findPodcastByUuid(episode.podcastUuid) ?: return@launch

            ShareDialog(
                episode = episode,
                podcast = podcast,
                fragmentManager = fragmentManager,
                context = context,
                shouldShowPodcast = false,
                analyticsTracker = analyticsTracker,
            ).show(sourceView = SourceView.SWIPE_ACTION)
        }
    }

    fun trackSwipeAction(
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
        swipeAction: EpisodeItemTouchHelper.SwipeAction,
    ) {
        analyticsTracker.track(
            AnalyticsEvent.EPISODE_SWIPE_ACTION_PERFORMED,
            mapOf(
                "action" to swipeAction.analyticsValue,
                "source" to swipeSource.analyticsValue
            )
        )
    }

    fun episodeSwipeUpNextTop(
        episode: BaseEpisode,
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            playbackManager.playNext(
                episode = episode,
                source = swipeSourceToSourceView(swipeSource)
            )
            trackSwipeAction(
                swipeSource = swipeSource,
                swipeAction = EpisodeItemTouchHelper.SwipeAction.UP_NEXT_ADD_TOP
            )
        }
    }

    fun episodeSwipeUpNextBottom(
        episode: BaseEpisode,
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            playbackManager.playLast(
                episode = episode,
                source = swipeSourceToSourceView(swipeSource)
            )
            trackSwipeAction(
                swipeSource = swipeSource,
                swipeAction = EpisodeItemTouchHelper.SwipeAction.UP_NEXT_ADD_BOTTOM
            )
        }
    }

    fun episodeSwipeRemoveUpNext(
        episode: BaseEpisode,
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            playbackManager.removeEpisode(
                episodeToRemove = episode,
                source = swipeSourceToSourceView(swipeSource)
            )
            trackSwipeAction(
                swipeSource = swipeSource,
                swipeAction = EpisodeItemTouchHelper.SwipeAction.UP_NEXT_REMOVE
            )
        }
    }

    fun updateArchive(episode: PodcastEpisode, swipeSource: EpisodeItemTouchHelper.SwipeSource) {
        viewModelScope.launch(Dispatchers.Default) {
            if (!episode.isArchived) {
                episodeManager.archive(episode, playbackManager)
                trackSwipeAction(swipeSource, EpisodeItemTouchHelper.SwipeAction.ARCHIVE)
                episodeAnalytics.trackEvent(
                    event = AnalyticsEvent.EPISODE_ARCHIVED,
                    source = swipeSourceToSourceView(swipeSource),
                    uuid = episode.uuid
                )
            } else {
                episodeManager.unarchive(episode)
                trackSwipeAction(swipeSource, EpisodeItemTouchHelper.SwipeAction.UNARCHIVE)
                episodeAnalytics.trackEvent(
                    event = AnalyticsEvent.EPISODE_UNARCHIVED,
                    source = swipeSourceToSourceView(swipeSource),
                    uuid = episode.uuid
                )
            }
        }
    }

    fun deleteEpisode(
        episode: UserEpisode,
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
        fragmentManager: FragmentManager,
        onDismiss: () -> Unit,
    ) {
        trackSwipeAction(swipeSource, EpisodeItemTouchHelper.SwipeAction.DELETE)
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
                )
                episodeAnalytics.trackEvent(
                    event = if (deleteState == DeleteState.Cloud && !episode.isDownloaded) AnalyticsEvent.EPISODE_DELETED_FROM_CLOUD else AnalyticsEvent.EPISODE_DOWNLOAD_DELETED,
                    source = SourceView.FILES,
                    uuid = episode.uuid,
                )
            },
            resources = context.resources
        ).apply {
            setOnDismiss { onDismiss() }
            show(fragmentManager, "delete_confirm")
        }
    }

    private fun isEpisodeQueued(episode: BaseEpisode) = playbackManager.upNextQueue.contains(episode.uuid)

    private fun swipeSourceToSourceView(swipeSource: EpisodeItemTouchHelper.SwipeSource) = when (swipeSource) {
        EpisodeItemTouchHelper.SwipeSource.PODCAST_DETAILS -> SourceView.PODCAST_SCREEN
        EpisodeItemTouchHelper.SwipeSource.FILTERS -> SourceView.FILTERS
        EpisodeItemTouchHelper.SwipeSource.DOWNLOADS -> SourceView.DOWNLOADS
        EpisodeItemTouchHelper.SwipeSource.LISTENING_HISTORY -> SourceView.LISTENING_HISTORY
        EpisodeItemTouchHelper.SwipeSource.STARRED -> SourceView.STARRED
        EpisodeItemTouchHelper.SwipeSource.FILES -> SourceView.FILES
        EpisodeItemTouchHelper.SwipeSource.UP_NEXT -> SourceView.UP_NEXT
    }

    fun getSwipeButtonLayout(
        episode: BaseEpisode,
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
        showShareButton: Boolean,
        defaultUpNextSwipeAction: () -> Settings.UpNextAction,
        buttons: SwipeButtons,
    ): SwipeButtonLayout {

        val onUpNextQueueScreen = swipeSource == EpisodeItemTouchHelper.SwipeSource.UP_NEXT

        return if (onUpNextQueueScreen) {
            SwipeButtonLayout(
                // We ignore the user's swipe preference setting when on the up next screen
                leftPrimary = { buttons.addToUpNextTop },
                leftSecondary = { buttons.addToUpNextBottom },
                rightPrimary = { buttons.removeFromUpNext },
                rightSecondary = { null },
            )
        } else {
            SwipeButtonLayout(
                leftPrimary = {
                    if (isEpisodeQueued(episode)) {
                        buttons.removeFromUpNext
                    } else {
                        // The left primary button is the action that is taken when the user swipes to the right
                        when (defaultUpNextSwipeAction()) {
                            Settings.UpNextAction.PLAY_NEXT -> buttons.addToUpNextTop
                            Settings.UpNextAction.PLAY_LAST -> buttons.addToUpNextBottom
                        }
                    }
                },
                leftSecondary = {
                    if (isEpisodeQueued(episode)) {
                        // Do not show a secondary button on the left when episode queued
                        null
                    } else {
                        when (defaultUpNextSwipeAction()) {
                            Settings.UpNextAction.PLAY_NEXT -> buttons.addToUpNextBottom
                            Settings.UpNextAction.PLAY_LAST -> buttons.addToUpNextTop
                        }
                    }
                },
                rightPrimary = {
                    when (episode) {
                        is UserEpisode -> buttons.deleteFile
                        is PodcastEpisode -> buttons.archive
                    }
                },
                rightSecondary = {
                    when (episode) {
                        is UserEpisode -> null
                        is PodcastEpisode ->
                            if (showShareButton) {
                                buttons.share
                            } else null
                    }
                },
            )
        }
    }

    data class SwipeButtons(
        val addToUpNextTop: SwipeButton.AddToUpNextTop,
        val addToUpNextBottom: SwipeButton.AddToUpNextBottom,
        val removeFromUpNext: SwipeButton.RemoveFromUpNext,
        val archive: SwipeButton.ArchiveButton,
        val deleteFile: SwipeButton.DeleteFileButton,
        val share: SwipeButton.ShareButton,
    )
}
