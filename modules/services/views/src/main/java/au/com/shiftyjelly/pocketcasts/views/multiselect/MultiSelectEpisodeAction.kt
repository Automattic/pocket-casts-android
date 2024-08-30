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
    override val groupId: String,
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
    isVisible,
) {
    object DeleteDownload : MultiSelectEpisodeAction(
        groupId = "download",
        actionId = UR.id.menu_undownload,
        title = LR.string.delete_download,
        iconRes = IR.drawable.ic_undownload,
        analyticsValue = "remove_download",
    )
    object Download : MultiSelectEpisodeAction(
        groupId = "download",
        actionId = R.id.menu_download,
        title = LR.string.download,
        iconRes = IR.drawable.ic_download,
        analyticsValue = "download",
    )
    object Archive : MultiSelectEpisodeAction(
        groupId = "archive",
        actionId = R.id.menu_archive,
        title = LR.string.archive,
        iconRes = IR.drawable.ic_archive,
        analyticsValue = "archive",
    )
    object Unarchive : MultiSelectEpisodeAction(
        groupId = "archive",
        actionId = UR.id.menu_unarchive,
        title = LR.string.unarchive,
        iconRes = IR.drawable.ic_unarchive,
        analyticsValue = "unarchive",
    )
    object DeleteUserEpisode : MultiSelectEpisodeAction(
        groupId = "archive",
        actionId = R.id.menu_delete,
        title = LR.string.delete,
        iconRes = R.drawable.ic_delete,
        analyticsValue = "delete",
    )
    object Share : MultiSelectEpisodeAction(
        groupId = "share",
        actionId = R.id.menu_share,
        title = LR.string.share,
        iconRes = IR.drawable.ic_share,
        analyticsValue = "share",
    )
    object MarkAsUnplayed : MultiSelectEpisodeAction(
        groupId = "mark_as_played",
        actionId = UR.id.menu_markasunplayed,
        title = LR.string.mark_as_unplayed,
        iconRes = IR.drawable.ic_markasunplayed,
        analyticsValue = "mark_as_unplayed",
    )
    object MarkAsPlayed : MultiSelectEpisodeAction(
        groupId = "mark_as_played",
        actionId = R.id.menu_mark_played,
        title = LR.string.mark_as_played,
        iconRes = IR.drawable.ic_markasplayed,
        analyticsValue = "mark_as_played",
    )
    object PlayNext : MultiSelectEpisodeAction(
        groupId = "play_next",
        actionId = R.id.menu_playnext,
        title = LR.string.play_next,
        iconRes = IR.drawable.ic_upnext_playnext,
        analyticsValue = "play_next",
    )
    object PlayLast : MultiSelectEpisodeAction(
        groupId = "play_last",
        actionId = R.id.menu_playlast,
        title = LR.string.play_last,
        iconRes = IR.drawable.ic_upnext_playlast,
        analyticsValue = "play_last",
    )
    object Unstar : MultiSelectEpisodeAction(
        groupId = "star",
        actionId = UR.id.menu_unstar,
        title = LR.string.unstar,
        iconRes = IR.drawable.ic_unstar,
        analyticsValue = "unstar",
    )
    object Star : MultiSelectEpisodeAction(
        groupId = "star",
        actionId = R.id.menu_star,
        title = LR.string.star,
        iconRes = IR.drawable.ic_star,
        analyticsValue = "star",
    )

    companion object {
        private val STANDARD = listOf(Download, Archive, MarkAsPlayed, PlayNext, PlayLast, Star, Share)
        private val ALL = STANDARD + listOf(DeleteDownload, DeleteUserEpisode, MarkAsUnplayed, Unstar, Unarchive)
        private val STANDARD_BY_GROUP_ID = STANDARD.associateBy { it.groupId }
        val ALL_BY_ACTION_ID = ALL.associateBy { it.actionId }

        fun listFromIds(list: List<String>): List<MultiSelectEpisodeAction> {
            val loadedItems = list.mapNotNull { STANDARD_BY_GROUP_ID[it] }
            val missingItems = STANDARD.subtract(loadedItems.toHashSet()) // We need to add on any missing items in case we add actions in the future
            return loadedItems + missingItems
        }

        fun actionForGroup(groupId: String, selected: List<BaseEpisode>): MultiSelectEpisodeAction? {
            when (groupId) {
                Download.groupId -> {
                    for (episode in selected) {
                        if (!episode.isDownloaded) {
                            return Download
                        }
                    }

                    return DeleteDownload
                }
                Archive.groupId -> {
                    for (episode in selected) {
                        if (episode is PodcastEpisode && !episode.isArchived) {
                            return Archive
                        }
                    }

                    return if (selected.filterIsInstance<UserEpisode>().size == selected.size) DeleteUserEpisode else Unarchive
                }
                MarkAsPlayed.groupId -> {
                    for (episode in selected) {
                        if (!episode.isFinished) {
                            return MarkAsPlayed
                        }
                    }

                    return MarkAsUnplayed
                }
                Star.groupId -> {
                    if (selected.filterIsInstance<UserEpisode>().isNotEmpty()) return null
                    for (episode in selected) {
                        if (episode is PodcastEpisode && !episode.isStarred) {
                            return Star
                        }
                    }

                    return Unstar
                }
                PlayNext.groupId -> return PlayNext
                PlayLast.groupId -> return PlayLast
                Share.groupId -> {
                    if (selected.size == 1 &&
                        selected.firstOrNull() is PodcastEpisode
                    ) {
                        return Share
                    }
                }
            }

            return null
        }
    }
}
