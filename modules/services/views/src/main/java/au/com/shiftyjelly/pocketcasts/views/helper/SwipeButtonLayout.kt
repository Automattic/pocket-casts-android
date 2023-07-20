package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

data class SwipeButtonLayout(
    val leftPrimary: () -> SwipeButton,
    val leftSecondary: () -> SwipeButton?,
    val rightPrimary: () -> SwipeButton,
    val rightSecondary: () -> SwipeButton?,
)

typealias RowIndex = Int

sealed interface SwipeButton {
    val iconRes: Int
    val backgroundColor: (Context) -> Int
    val onClick: (BaseEpisode, RowIndex) -> Unit

    companion object {
        private val removeUpNextIconRes = IR.drawable.ic_upnext_remove
        private val removeUpNextBackgroundColorAttr = UR.attr.support_05
    }

    // Shows the remove from up next button if the episode is already queued
    class AddToUpNextTopOrRemove(
        private val episode: BaseEpisode,
        private val playbackManager: PlaybackManager,
        private val alwaysShowAddQueueOptions: Boolean,
        override val onClick: (BaseEpisode, RowIndex) -> Unit,
    ) : SwipeButton {

        override val iconRes
            get() =
                if (playbackManager.upNextQueue.contains(episode.uuid) && !alwaysShowAddQueueOptions) {
                    removeUpNextIconRes
                } else {
                    IR.drawable.ic_upnext_movetotop
                }

        override val backgroundColor: (Context) -> Int
            get() = {
                if (playbackManager.upNextQueue.contains(episode.uuid) && !alwaysShowAddQueueOptions) {
                    it.getThemeColor(removeUpNextBackgroundColorAttr)
                } else {
                    it.getThemeColor(UR.attr.support_04)
                }
            }
    }

    // Shows the remove from up next button if the episode is already queued
    class AddToUpNextBottom(
        private val episode: BaseEpisode,
        private val playbackManager: PlaybackManager,
        private val alwaysShowAddQueueOptions: Boolean,
        override val onClick: (BaseEpisode, RowIndex) -> Unit
    ) : SwipeButton {

        override val iconRes
            get() = if (playbackManager.upNextQueue.contains(episode.uuid) && !alwaysShowAddQueueOptions) {
                removeUpNextIconRes
            } else {
                IR.drawable.ic_upnext_movetobottom
            }

        override val backgroundColor: (Context) -> Int
            get() = {
                it.getThemeColor(
                    if (playbackManager.upNextQueue.contains(episode.uuid) && !alwaysShowAddQueueOptions) {
                        removeUpNextBackgroundColorAttr
                    } else {
                        UR.attr.support_03
                    }
                )
            }
    }

    class DeleteFileButton(
        override val onClick: (BaseEpisode, RowIndex) -> Unit
    ) : SwipeButton {
        override val iconRes = VR.drawable.ic_delete
        override val backgroundColor: (Context) -> Int =
            { it.getThemeColor(UR.attr.support_05) }
    }

    class ArchiveButton(
        private val episode: BaseEpisode,
        override val onClick: (BaseEpisode, RowIndex) -> Unit
    ) : SwipeButton {
        override val iconRes
            get() = if (episode.isArchived) {
                IR.drawable.ic_unarchive
            } else {
                IR.drawable.ic_archive
            }
        override val backgroundColor: (Context) -> Int =
            { it.getThemeColor(UR.attr.support_06) }
    }

    class ShareButton(
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
        fragmentManager: FragmentManager,
        context: Context,
        viewModel: SwipeButtonLayoutViewModel,
    ) : SwipeButton {
        override val iconRes = IR.drawable.ic_share
        override val backgroundColor: (Context) -> Int =
            { it.getThemeColor(UR.attr.support_01) }
        override val onClick: (BaseEpisode, RowIndex) -> Unit = { baseEpisode, _ ->
            (baseEpisode as? PodcastEpisode)?.let { episode ->
                viewModel.share(episode, fragmentManager, context, swipeSource)
            }
        }
    }
}

/**
 * - [onDeleteOrArchiveClick] will delete [UserEpisode]s and archive [PodcastEpisode]s
 */
class SwipeButtonLayoutFactory(
    private val swipeButtonLayoutViewModel: SwipeButtonLayoutViewModel,
    private val onQueueUpNextTopClick: (BaseEpisode, RowIndex) -> Unit,
    private val onQueueUpNextBottomClick: (BaseEpisode, RowIndex) -> Unit,
    private val onDeleteOrArchiveClick: (BaseEpisode, RowIndex) -> Unit,
    private val showShareButton: Boolean = true,
    private val playbackManager: PlaybackManager,
    private val defaultUpNextSwipeAction: () -> Settings.UpNextAction,
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val swipeSource: EpisodeItemTouchHelper.SwipeSource,
) {
    fun forEpisode(
        episode: BaseEpisode,
        isShowingUpNextQueue: Boolean = false,
        ignoreUserSwipePreference: Boolean = false,
    ): SwipeButtonLayout {
        val isQueued = { playbackManager.upNextQueue.contains(episode.uuid) }
        val addToUpNextTop = SwipeButton.AddToUpNextTopOrRemove(
            episode = episode,
            playbackManager = playbackManager,
            alwaysShowAddQueueOptions = isShowingUpNextQueue,
            onClick = onQueueUpNextTopClick,
        )
        val addToUpNextBottom = SwipeButton.AddToUpNextBottom(
            episode = episode,
            playbackManager = playbackManager,
            alwaysShowAddQueueOptions = isShowingUpNextQueue,
            onClick = onQueueUpNextBottomClick,
        )

        return SwipeButtonLayout(
            leftPrimary = {
                if (ignoreUserSwipePreference) {
                    addToUpNextTop
                } else {
                    // The left primary button is the action that is taken when the user swipes to the right
                    when (defaultUpNextSwipeAction()) {
                        Settings.UpNextAction.PLAY_NEXT -> addToUpNextTop
                        Settings.UpNextAction.PLAY_LAST -> addToUpNextBottom
                    }
                }
            },
            leftSecondary = {
                if (isQueued() && !isShowingUpNextQueue) {
                    null
                } else {
                    if (ignoreUserSwipePreference) {
                        addToUpNextBottom
                    } else {
                        when (defaultUpNextSwipeAction()) {
                            Settings.UpNextAction.PLAY_NEXT -> addToUpNextBottom
                            Settings.UpNextAction.PLAY_LAST -> addToUpNextTop
                        }
                    }
                }
            },
            rightPrimary = {
                if (isShowingUpNextQueue) {
                    // When an episode is queued, the button shows the remove from queue option
                    SwipeButton.AddToUpNextTopOrRemove(
                        episode = episode,
                        playbackManager = playbackManager,
                        alwaysShowAddQueueOptions = false, // ensures this button shows the remove option
                        onClick = onQueueUpNextTopClick,
                    )
                } else {
                    when (episode) {
                        is PodcastEpisode -> SwipeButton.ArchiveButton(episode, onDeleteOrArchiveClick)

                        is UserEpisode -> SwipeButton.DeleteFileButton(onDeleteOrArchiveClick)
                    }
                }
            },
            rightSecondary = {
                if (isShowingUpNextQueue) {
                    null
                } else {
                    when (episode) {
                        is PodcastEpisode -> if (showShareButton) {
                            SwipeButton.ShareButton(
                                swipeSource = swipeSource,
                                fragmentManager = fragmentManager,
                                context = context,
                                viewModel = swipeButtonLayoutViewModel,
                            )
                        } else null
                        is UserEpisode -> null
                    }
                }
            },
        )
    }
}
