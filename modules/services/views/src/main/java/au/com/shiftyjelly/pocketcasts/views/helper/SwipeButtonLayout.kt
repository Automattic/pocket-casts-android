package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
        private val onItemUpdated: (BaseEpisode, RowIndex) -> Unit,
        private val swipeSource: EpisodeItemTouchHelper.SwipeSource,
        private val viewModel: SwipeButtonLayoutViewModel,
    ) : SwipeButton {

        override val iconRes = IR.drawable.ic_upnext_movetotop

        override val backgroundColor: (Context) -> Int = { it.getThemeColor(UR.attr.support_04) }

        override val onClick: (BaseEpisode, RowIndex) -> Unit
            get() = { baseEpisode, rowIndex ->
                viewModel.episodeSwipeUpNextTop(
                    episode = baseEpisode,
                    swipeSource = swipeSource,
                )
                onItemUpdated(baseEpisode, rowIndex)
            }
    }

    // Shows the remove from up next button if the episode is already queued
    class AddToUpNextBottom(
        private val onItemUpdated: (BaseEpisode, RowIndex) -> Unit,
        private val swipeSource: EpisodeItemTouchHelper.SwipeSource,
        private val viewModel: SwipeButtonLayoutViewModel,
    ) : SwipeButton {

        override val iconRes = IR.drawable.ic_upnext_movetobottom

        override val backgroundColor: (Context) -> Int = {
            it.getThemeColor(UR.attr.support_03)
        }

        override val onClick: (BaseEpisode, RowIndex) -> Unit = { baseEpisode, rowIndex ->
            viewModel.episodeSwipeUpNextBottom(
                episode = baseEpisode,
                swipeSource = swipeSource,
            )
            onItemUpdated(baseEpisode, rowIndex)
        }
    }

    class RemoveFromUpNext(
        private val onItemUpdated: (BaseEpisode, RowIndex) -> Unit,
        private val swipeSource: EpisodeItemTouchHelper.SwipeSource,
        private val viewModel: SwipeButtonLayoutViewModel,
    ) : SwipeButton {

        override val iconRes = removeUpNextIconRes

        override val backgroundColor: (Context) -> Int = { it.getThemeColor(removeUpNextBackgroundColorAttr) }

        override val onClick: (BaseEpisode, RowIndex) -> Unit = { baseEpisode, rowIndex ->
            viewModel.episodeSwipeRemoveUpNext(
                episode = baseEpisode,
                swipeSource = swipeSource,
            )
            onItemUpdated(baseEpisode, rowIndex)
        }
    }

    class DeleteFileButton(
        private val onItemModified: (UserEpisode, RowIndex) -> Unit,
        private val swipeSource: EpisodeItemTouchHelper.SwipeSource,
        private val fragmentManager: FragmentManager,
        private val viewModel: SwipeButtonLayoutViewModel,
    ) : SwipeButton {

        override val iconRes = VR.drawable.ic_delete

        override val backgroundColor: (Context) -> Int =
            { it.getThemeColor(UR.attr.support_05) }

        override val onClick: (BaseEpisode, RowIndex) -> Unit = { episode, rowIndex ->
            if (episode !is UserEpisode) {
                throw IllegalStateException("Can only delete user episodes, but tried to delete: $episode")
            }
            viewModel.deleteEpisode(
                episode = episode,
                swipeSource = swipeSource,
                fragmentManager = fragmentManager,
                onDismiss = { onItemModified(episode, rowIndex) }
            )
        }
    }

    class ArchiveButton(
        private val episode: BaseEpisode,
        private val onItemUpdated: (BaseEpisode, RowIndex) -> Unit,
        private val swipeSource: EpisodeItemTouchHelper.SwipeSource,
        private val viewModel: SwipeButtonLayoutViewModel,
    ) : SwipeButton {

        override val iconRes
            get() = if (episode.isArchived) {
                IR.drawable.ic_unarchive
            } else {
                IR.drawable.ic_archive
            }

        override val backgroundColor: (Context) -> Int =
            { it.getThemeColor(UR.attr.support_06) }

        override val onClick: (BaseEpisode, RowIndex) -> Unit = { episode, rowIndex ->
            if (episode !is PodcastEpisode) {
                throw IllegalStateException("Can only share podcast episodes, but tried to archive: $episode")
            }
            viewModel.updateArchive(episode, swipeSource)
            onItemUpdated(episode, rowIndex)
        }
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

        override val onClick: (BaseEpisode, RowIndex) -> Unit = { episode, _ ->
            if (episode !is PodcastEpisode) {
                throw IllegalStateException("Can only share podcast episodes: $episode")
            }
            viewModel.share(episode, fragmentManager, context, swipeSource)
        }
    }
}

/**
 * - [onDeleteOrArchiveClick] will delete [UserEpisode]s and archive [PodcastEpisode]s
 */
class SwipeButtonLayoutFactory(
    private val swipeButtonLayoutViewModel: SwipeButtonLayoutViewModel,
    private val onItemUpdated: (BaseEpisode, RowIndex) -> Unit,
    private val showShareButton: Boolean = true,
    private val defaultUpNextSwipeAction: () -> Settings.UpNextAction,
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val swipeSource: EpisodeItemTouchHelper.SwipeSource,
) {
    fun forEpisode(episode: BaseEpisode): SwipeButtonLayout {

        val buttons = object {
            val addToUpNextTop = SwipeButton.AddToUpNextTopOrRemove(
                onItemUpdated = onItemUpdated,
                swipeSource = swipeSource,
                viewModel = swipeButtonLayoutViewModel,
            )
            val addToUpNextBottom = SwipeButton.AddToUpNextBottom(
                onItemUpdated = onItemUpdated,
                swipeSource = swipeSource,
                viewModel = swipeButtonLayoutViewModel,
            )
            val removeFromUpNext = SwipeButton.RemoveFromUpNext(
                onItemUpdated = onItemUpdated,
                swipeSource = swipeSource,
                viewModel = swipeButtonLayoutViewModel,
            )
            val archive = SwipeButton.ArchiveButton(
                episode = episode,
                onItemUpdated = onItemUpdated,
                swipeSource = swipeSource,
                viewModel = swipeButtonLayoutViewModel,
            )
            val deleteFile = SwipeButton.DeleteFileButton(
                onItemModified = onItemUpdated,
                swipeSource = swipeSource,
                fragmentManager = fragmentManager,
                viewModel = swipeButtonLayoutViewModel,
            )
            val share = SwipeButton.ShareButton(
                swipeSource = swipeSource,
                fragmentManager = fragmentManager,
                context = context,
                viewModel = swipeButtonLayoutViewModel,
            )
        }

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
                    if (swipeButtonLayoutViewModel.isEpisodeQueued(episode)) {
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
                    if (swipeButtonLayoutViewModel.isEpisodeQueued(episode)) {
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
}

private fun getOnUpNextQueueScreen(swipeSource: EpisodeItemTouchHelper.SwipeSource) =
    swipeSource == EpisodeItemTouchHelper.SwipeSource.UP_NEXT
