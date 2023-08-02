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

    // Shows the remove from up next button if the episode is already queued
    class AddToUpNextTop(
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

        override val iconRes = IR.drawable.ic_upnext_remove

        override val backgroundColor: (Context) -> Int = { it.getThemeColor(UR.attr.support_05) }

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

class SwipeButtonLayoutFactory(
    private val swipeButtonLayoutViewModel: SwipeButtonLayoutViewModel,
    private val onItemUpdated: (BaseEpisode, RowIndex) -> Unit,
    private val showShareButton: Boolean = true,
    private val defaultUpNextSwipeAction: () -> Settings.UpNextAction,
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val swipeSource: EpisodeItemTouchHelper.SwipeSource,
) {
    fun forEpisode(episode: BaseEpisode): SwipeButtonLayout =
        swipeButtonLayoutViewModel.getSwipeButtonLayout(
            episode = episode,
            swipeSource = swipeSource,
            defaultUpNextSwipeAction = defaultUpNextSwipeAction,
            showShareButton = showShareButton,
            buttons = SwipeButtonLayoutViewModel.SwipeButtons(
                addToUpNextTop = SwipeButton.AddToUpNextTop(
                    onItemUpdated = onItemUpdated,
                    swipeSource = swipeSource,
                    viewModel = swipeButtonLayoutViewModel,
                ),
                addToUpNextBottom = SwipeButton.AddToUpNextBottom(
                    onItemUpdated = onItemUpdated,
                    swipeSource = swipeSource,
                    viewModel = swipeButtonLayoutViewModel,
                ),
                removeFromUpNext = SwipeButton.RemoveFromUpNext(
                    onItemUpdated = onItemUpdated,
                    swipeSource = swipeSource,
                    viewModel = swipeButtonLayoutViewModel,
                ),
                archive = SwipeButton.ArchiveButton(
                    episode = episode,
                    onItemUpdated = onItemUpdated,
                    swipeSource = swipeSource,
                    viewModel = swipeButtonLayoutViewModel,
                ),
                deleteFile = SwipeButton.DeleteFileButton(
                    onItemModified = onItemUpdated,
                    swipeSource = swipeSource,
                    fragmentManager = fragmentManager,
                    viewModel = swipeButtonLayoutViewModel,
                ),
                share = SwipeButton.ShareButton(
                    swipeSource = swipeSource,
                    fragmentManager = fragmentManager,
                    context = context,
                    viewModel = swipeButtonLayoutViewModel,
                ),
            ),
        )
}
