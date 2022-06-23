package au.com.shiftyjelly.pocketcasts.models.to

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class PodcastGrouping(@StringRes val groupName: Int, val sortFunction: ((Episode) -> Int)?) {
    companion object {
        val All
            get() = listOf(None, Downloaded, Unplayed, Season, Starred)
    }

    abstract fun groupTitles(index: Int, context: Context): String

    object None : PodcastGrouping(LR.string.none, null) {
        override fun groupTitles(index: Int, context: Context): String {
            return context.getString(LR.string.none)
        }
    }

    object Downloaded : PodcastGrouping(LR.string.podcast_group_downloaded, { if (it.isDownloaded || it.isDownloading || it.isQueued) 0 else 1 }) {
        override fun groupTitles(index: Int, context: Context): String {
            return if (index == 0) context.getString(LR.string.podcast_group_downloaded) else context.getString(
                LR.string.podcast_group_not_downloaded
            )
        }
    }

    object Unplayed : PodcastGrouping(LR.string.podcast_group_unplayed, { if (it.isUnplayed || it.isInProgress) 0 else 1 }) {
        override fun groupTitles(index: Int, context: Context): String {
            return if (index == 0) context.getString(LR.string.podcast_group_unplayed) else context.getString(
                LR.string.podcast_group_played
            )
        }
    }

    object Season : PodcastGrouping(LR.string.podcast_group_season, { it.season?.toInt() ?: 0 }) {
        lateinit var groupTitlesList: List<String>
        override fun groupTitles(index: Int, context: Context): String {
            return groupTitlesList.getOrNull(index) ?: context.getString(LR.string.podcast_no_season)
        }

        override fun formGroups(episodes: List<Episode>, reversedSort: Boolean, resources: Resources): List<List<Episode>> {
            val list = super.formGroups(episodes, reversedSort, resources)
            val titleList = mutableListOf<String>()
            list.forEach {
                val firstEpisode = it.firstOrNull()
                if (firstEpisode != null) {
                    if (firstEpisode.season != null) {
                        titleList.add(resources.getString(LR.string.podcast_season_x, firstEpisode.season ?: 0))
                    } else {
                        titleList.add(resources.getString(LR.string.podcast_no_season))
                    }
                } else {
                    titleList.add("")
                }
            }

            groupTitlesList = titleList.toList()
            return list
        }
    }

    object Starred : PodcastGrouping(LR.string.profile_navigation_starred, { if (it.isStarred) 0 else 1 }) {
        override fun groupTitles(index: Int, context: Context): String {
            return if (index == 0) context.getString(LR.string.profile_navigation_starred) else context.getString(
                LR.string.podcast_group_not_starred
            )
        }
    }

    /**
     * Sorts episodes in to their group order and return the sorted list plus
     * the index of the groups
     * @param episodes A sorted list of episodes
     * @return A pair of episodes and their group indexes
     */
    open fun formGroups(episodes: List<Episode>, reversedSort: Boolean = false, resources: Resources): List<List<Episode>> {
        val sortFunction = this.sortFunction ?: return listOf(episodes)
        val groups = mutableListOf<MutableList<Episode>>()

        episodes.forEach { episode ->
            val groupId = sortFunction(episode)
            val group = groups.getOrNull(groupId)
            if (group == null) {
                if (groupId > groups.size) {
                    for (i in groups.size until groupId) {
                        groups.add(mutableListOf())
                    }
                }
                groups.add(groupId, mutableListOf(episode))
            } else {
                group.add(episode)
            }
        }

        if (reversedSort) {
            groups.reverse()
        }
        return groups.toList()
    }
}
