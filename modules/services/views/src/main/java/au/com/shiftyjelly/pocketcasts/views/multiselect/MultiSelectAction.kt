package au.com.shiftyjelly.pocketcasts.views.multiselect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

sealed class MultiSelectAction(
    open val groupId: Int,
    open val actionId: Int,
    @StringRes open val title: Int,
    @DrawableRes open val iconRes: Int,
    open val analyticsValue: String,
    open val isVisible: Boolean = true,
) {
    object SelectAll : MultiSelectAction(
        R.id.menu_select_all,
        R.id.menu_select_all,
        LR.string.select_all,
        IR.drawable.ic_selectall_up,
        "select_all"
    )

    sealed class BaseEpisodeAction(
        override val groupId: Int,
        override val actionId: Int,
        @StringRes override val title: Int,
        @DrawableRes override val iconRes: Int,
        override val analyticsValue: String,
        override val isVisible: Boolean = true,
    ) : MultiSelectAction(
        groupId,
        actionId,
        title,
        iconRes,
        analyticsValue,
        isVisible
    ) {
        object DeleteDownload : BaseEpisodeAction(
            R.id.menu_download,
            UR.id.menu_undownload,
            LR.string.delete_download,
            IR.drawable.ic_undownload,
            "remove_download"
        )
        object Download : BaseEpisodeAction(
            R.id.menu_download,
            R.id.menu_download,
            LR.string.download,
            IR.drawable.ic_download,
            "download"
        )
        object Archive : BaseEpisodeAction(
            R.id.menu_archive,
            R.id.menu_archive,
            LR.string.archive,
            IR.drawable.ic_archive,
            "archive"
        )
        object Unarchive : BaseEpisodeAction(
            R.id.menu_archive,
            UR.id.menu_unarchive,
            LR.string.unarchive,
            IR.drawable.ic_unarchive,
            "unarchive"
        )
        object DeleteUserEpisode : BaseEpisodeAction(
            R.id.menu_archive,
            R.id.menu_delete,
            LR.string.delete,
            R.drawable.ic_delete,
            "delete"
        )
        object MarkAsUnplayed : BaseEpisodeAction(
            R.id.menu_mark_played,
            UR.id.menu_markasunplayed,
            LR.string.mark_as_unplayed,
            IR.drawable.ic_markasunplayed,
            "mark_as_unplayed"
        )
        object MarkAsPlayed : BaseEpisodeAction(
            R.id.menu_mark_played,
            R.id.menu_mark_played,
            LR.string.mark_as_played,
            IR.drawable.ic_markasplayed,
            "mark_as_played"
        )
        object PlayNext : BaseEpisodeAction(
            R.id.menu_playnext,
            R.id.menu_playnext,
            LR.string.play_next,
            IR.drawable.ic_upnext_playnext,
            "play_next"
        )
        object PlayLast : BaseEpisodeAction(
            R.id.menu_playlast,
            R.id.menu_playlast,
            LR.string.play_last,
            IR.drawable.ic_upnext_playlast,
            "play_last"
        )
        object Unstar : BaseEpisodeAction(
            R.id.menu_star,
            UR.id.menu_unstar,
            LR.string.unstar,
            IR.drawable.ic_unstar,
            "unstar"
        )
        object Star : BaseEpisodeAction(
            R.id.menu_star,
            R.id.menu_star,
            LR.string.star,
            IR.drawable.ic_star,
            "star"
        )
    }

    sealed class BookmarkAction(
        override val groupId: Int,
        override val actionId: Int,
        @StringRes override val title: Int,
        @DrawableRes override val iconRes: Int,
        override val analyticsValue: String,
        override val isVisible: Boolean = true,
    ) : MultiSelectAction(
        groupId,
        actionId,
        title,
        iconRes,
        analyticsValue,
        isVisible
    ) {
        object DeleteBookmark : BookmarkAction(
            R.id.menu_delete,
            R.id.menu_delete,
            LR.string.delete,
            R.drawable.ic_delete,
            "delete",
        )
        data class EditBookmark(override val isVisible: Boolean) : BookmarkAction(
            UR.id.menu_edit,
            UR.id.menu_edit,
            LR.string.delete,
            IR.drawable.ic_edit,
            "edit",
            isVisible = isVisible
        )
    }

    companion object {
        val STANDARD = listOf(BaseEpisodeAction.Download, BaseEpisodeAction.Archive, BaseEpisodeAction.MarkAsPlayed, BaseEpisodeAction.PlayNext, BaseEpisodeAction.PlayLast, BaseEpisodeAction.Star)
        val ALL = STANDARD + listOf(BaseEpisodeAction.DeleteDownload, BaseEpisodeAction.DeleteUserEpisode, BaseEpisodeAction.MarkAsUnplayed, BaseEpisodeAction.Unstar, BaseEpisodeAction.Unarchive)
        val STANDARD_BY_ID = STANDARD.associateBy { it.actionId }
        val ALL_BY_ID = ALL.associateBy { it.actionId }

        fun listFromIds(list: List<Int>): List<BaseEpisodeAction> {
            val loadedItems = list.mapNotNull { STANDARD_BY_ID[it] }
            val missingItems = STANDARD.subtract(loadedItems) // We need to add on any missing items in case we add actions in the future
            return loadedItems + missingItems
        }

        fun actionForGroup(groupId: Int, selected: List<BaseEpisode>): BaseEpisodeAction? {
            when (groupId) {
                R.id.menu_download -> {
                    for (episode in selected) {
                        if (!episode.isDownloaded) {
                            return BaseEpisodeAction.Download
                        }
                    }

                    return BaseEpisodeAction.DeleteDownload
                }
                R.id.menu_archive -> {
                    for (episode in selected) {
                        if (episode is PodcastEpisode && !episode.isArchived) {
                            return BaseEpisodeAction.Archive
                        }
                    }

                    return if (selected.filterIsInstance<UserEpisode>().size == selected.size) BaseEpisodeAction.DeleteUserEpisode else BaseEpisodeAction.Unarchive
                }
                R.id.menu_mark_played -> {
                    for (episode in selected) {
                        if (!episode.isFinished) {
                            return BaseEpisodeAction.MarkAsPlayed
                        }
                    }

                    return BaseEpisodeAction.MarkAsUnplayed
                }
                R.id.menu_star -> {
                    if (selected.filterIsInstance<UserEpisode>().isNotEmpty()) return null
                    for (episode in selected) {
                        if (episode is PodcastEpisode && !episode.isStarred) {
                            return BaseEpisodeAction.Star
                        }
                    }

                    return BaseEpisodeAction.Unstar
                }
                R.id.menu_playnext -> return BaseEpisodeAction.PlayNext
                R.id.menu_playlast -> return BaseEpisodeAction.PlayLast
            }

            return null
        }
    }
}
