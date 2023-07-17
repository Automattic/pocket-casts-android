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

sealed class MultiSelectEpisodeAction(
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
    object DeleteDownload : MultiSelectEpisodeAction(
        R.id.menu_download,
        UR.id.menu_undownload,
        LR.string.delete_download,
        IR.drawable.ic_undownload,
        "remove_download"
    )
    object Download : MultiSelectEpisodeAction(
        R.id.menu_download,
        R.id.menu_download,
        LR.string.download,
        IR.drawable.ic_download,
        "download"
    )
    object Archive : MultiSelectEpisodeAction(
        R.id.menu_archive,
        R.id.menu_archive,
        LR.string.archive,
        IR.drawable.ic_archive,
        "archive"
    )
    object Unarchive : MultiSelectEpisodeAction(
        R.id.menu_archive,
        UR.id.menu_unarchive,
        LR.string.unarchive,
        IR.drawable.ic_unarchive,
        "unarchive"
    )
    object DeleteUserEpisode : MultiSelectEpisodeAction(
        R.id.menu_archive,
        R.id.menu_delete,
        LR.string.delete,
        R.drawable.ic_delete,
        "delete"
    )
    object MarkAsUnplayed : MultiSelectEpisodeAction(
        R.id.menu_mark_played,
        UR.id.menu_markasunplayed,
        LR.string.mark_as_unplayed,
        IR.drawable.ic_markasunplayed,
        "mark_as_unplayed"
    )
    object MarkAsPlayed : MultiSelectEpisodeAction(
        R.id.menu_mark_played,
        R.id.menu_mark_played,
        LR.string.mark_as_played,
        IR.drawable.ic_markasplayed,
        "mark_as_played"
    )
    object PlayNext : MultiSelectEpisodeAction(
        R.id.menu_playnext,
        R.id.menu_playnext,
        LR.string.play_next,
        IR.drawable.ic_upnext_playnext,
        "play_next"
    )
    object PlayLast : MultiSelectEpisodeAction(
        R.id.menu_playlast,
        R.id.menu_playlast,
        LR.string.play_last,
        IR.drawable.ic_upnext_playlast,
        "play_last"
    )
    object Unstar : MultiSelectEpisodeAction(
        R.id.menu_star,
        UR.id.menu_unstar,
        LR.string.unstar,
        IR.drawable.ic_unstar,
        "unstar"
    )
    object Star : MultiSelectEpisodeAction(
        R.id.menu_star,
        R.id.menu_star,
        LR.string.star,
        IR.drawable.ic_star,
        "star"
    )

    companion object {
        val STANDARD = listOf(Download, Archive, MarkAsPlayed, PlayNext, PlayLast, Star)
        val ALL = STANDARD + listOf(DeleteDownload, DeleteUserEpisode, MarkAsUnplayed, Unstar, Unarchive)
        val STANDARD_BY_ID = STANDARD.associateBy { it.actionId }
        val ALL_BY_ID = ALL.associateBy { it.actionId }

        fun listFromIds(list: List<Int>): List<MultiSelectEpisodeAction> {
            val loadedItems = list.mapNotNull { STANDARD_BY_ID[it] }
            val missingItems = STANDARD.subtract(loadedItems) // We need to add on any missing items in case we add actions in the future
            return loadedItems + missingItems
        }

        fun actionForGroup(groupId: Int, selected: List<BaseEpisode>): MultiSelectEpisodeAction? {
            when (groupId) {
                R.id.menu_download -> {
                    for (episode in selected) {
                        if (!episode.isDownloaded) {
                            return Download
                        }
                    }

                    return DeleteDownload
                }
                R.id.menu_archive -> {
                    for (episode in selected) {
                        if (episode is PodcastEpisode && !episode.isArchived) {
                            return Archive
                        }
                    }

                    return if (selected.filterIsInstance<UserEpisode>().size == selected.size) DeleteUserEpisode else Unarchive
                }
                R.id.menu_mark_played -> {
                    for (episode in selected) {
                        if (!episode.isFinished) {
                            return MarkAsPlayed
                        }
                    }

                    return MarkAsUnplayed
                }
                R.id.menu_star -> {
                    if (selected.filterIsInstance<UserEpisode>().isNotEmpty()) return null
                    for (episode in selected) {
                        if (episode is PodcastEpisode && !episode.isStarred) {
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
