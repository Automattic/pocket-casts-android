package au.com.shiftyjelly.pocketcasts.views.multiselect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

sealed class MultiSelectAction(val groupId: Int, val actionId: Int, @StringRes val title: Int, @DrawableRes val iconRes: Int) {
    object DeleteDownload : MultiSelectAction(
        R.id.menu_download,
        UR.id.menu_undownload,
        LR.string.delete_download,
        IR.drawable.ic_undownload
    )
    object Download : MultiSelectAction(
        R.id.menu_download,
        R.id.menu_download,
        LR.string.download,
        IR.drawable.ic_download
    )

    object Archive : MultiSelectAction(
        R.id.menu_archive,
        R.id.menu_archive,
        LR.string.archive,
        IR.drawable.ic_archive
    )
    object Unarchive : MultiSelectAction(
        R.id.menu_archive,
        UR.id.menu_unarchive,
        LR.string.unarchive,
        IR.drawable.ic_unarchive
    )
    object DeleteUserEpisode : MultiSelectAction(
        R.id.menu_archive,
        R.id.menu_delete,
        LR.string.delete,
        R.drawable.ic_delete
    )

    object MarkAsUnplayed : MultiSelectAction(
        R.id.menu_mark_played,
        UR.id.menu_markasunplayed,
        LR.string.mark_as_unplayed,
        IR.drawable.ic_markasunplayed
    )
    object MarkAsPlayed : MultiSelectAction(
        R.id.menu_mark_played,
        R.id.menu_mark_played,
        LR.string.mark_as_played,
        IR.drawable.ic_markasplayed
    )

    object PlayNext : MultiSelectAction(
        R.id.menu_playnext,
        R.id.menu_playnext,
        LR.string.play_next,
        IR.drawable.ic_upnext_playnext
    )

    object PlayLast : MultiSelectAction(
        R.id.menu_playlast,
        R.id.menu_playlast,
        LR.string.play_last,
        IR.drawable.ic_upnext_playlast
    )

    object Unstar : MultiSelectAction(
        R.id.menu_star,
        UR.id.menu_unstar,
        LR.string.unstar,
        IR.drawable.ic_unstar
    )
    object Star : MultiSelectAction(
        R.id.menu_star,
        R.id.menu_star,
        LR.string.star,
        IR.drawable.ic_star
    )

    object SelectAll : MultiSelectAction(
        R.id.menu_select_all,
        R.id.menu_select_all,
        LR.string.select_all,
        IR.drawable.ic_selectall_up
    )

    companion object {
        val STANDARD = listOf(Download, Archive, MarkAsPlayed, PlayNext, PlayLast, Star)
        val ALL = STANDARD + listOf(DeleteDownload, DeleteUserEpisode, MarkAsUnplayed, Unstar, Unarchive)
        val STANDARD_BY_ID = STANDARD.associateBy { it.actionId }
        val ALL_BY_ID = ALL.associateBy { it.actionId }

        fun listFromIds(list: List<Int>): List<MultiSelectAction> {
            val loadedItems = list.mapNotNull { STANDARD_BY_ID[it] }
            val missingItems = STANDARD.subtract(loadedItems) // We need to add on any missing items in case we add actions in the future
            return loadedItems + missingItems
        }

        fun actionForGroup(groupId: Int, selected: List<Playable>): MultiSelectAction? {
            when (groupId) {
                R.id.menu_download -> {
                    for (playable in selected) {
                        if (!playable.isDownloaded) {
                            return Download
                        }
                    }

                    return DeleteDownload
                }
                R.id.menu_archive -> {
                    for (playable in selected) {
                        if (playable is Episode && !playable.isArchived) {
                            return Archive
                        }
                    }

                    return if (selected.filterIsInstance<UserEpisode>().size == selected.size) DeleteUserEpisode else Unarchive
                }
                R.id.menu_mark_played -> {
                    for (playable in selected) {
                        if (!playable.isFinished) {
                            return MarkAsPlayed
                        }
                    }

                    return MarkAsUnplayed
                }
                R.id.menu_star -> {
                    if (selected.filterIsInstance<UserEpisode>().isNotEmpty()) return null
                    for (playable in selected) {
                        if (playable is Episode && !playable.isStarred) {
                            return Star
                        }
                    }

                    return Unstar
                }
                R.id.menu_playnext -> return PlayNext
                R.id.menu_playlast -> return PlayLast
            }

            return null
        }
    }
}
