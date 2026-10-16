package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import au.com.shiftyjelly.pocketcasts.models.to.FolderItem

internal fun filterPodcastsWithoutUnfinishedEpisodes(
    items: List<FolderItem>,
    unfinishedEpisodeCounts: Map<String, Int>,
): List<FolderItem> = items.mapNotNull { item ->
    when (item) {
        is FolderItem.Podcast -> item.takeIf { (unfinishedEpisodeCounts[item.uuid] ?: 0) > 0 }

        is FolderItem.Folder -> item.copy(
            podcasts = item.podcasts.filter { (unfinishedEpisodeCounts[it.uuid] ?: 0) > 0 },
        )
    }
}
