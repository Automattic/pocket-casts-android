package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import au.com.shiftyjelly.pocketcasts.models.to.PodcastFolder
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType

object PodcastFolderHelper {

    private val aToZComparator = compareBy<PodcastFolder> { PodcastsSortType.cleanStringForSort(it.podcast.title) }
    private val addDateComparator = compareBy<PodcastFolder> { it.podcast.addedDate }

    fun filter(searchText: String, list: List<PodcastFolder>): List<PodcastFolder> {
        return if (searchText.isBlank()) {
            list
        } else {
            list.filter { podcastFolder ->
                val podcast = podcastFolder.podcast
                podcast.title.contains(searchText, ignoreCase = true) || podcast.author.contains(searchText, ignoreCase = true)
            }
        }
    }

    fun sortForSelectingPodcasts(sortType: PodcastsSortType, podcastsSortedByReleaseDate: List<PodcastFolder>, currentFolderUuid: String?): List<PodcastFolder> {
        val folderComparator: Comparator<PodcastFolder> = Comparator { itemOne, itemTwo ->
            var folderOne = itemOne.folder
            var folderTwo = itemTwo.folder
            // don't sort the current folder to the bottom of the list
            if (currentFolderUuid != null) {
                if (folderOne != null && folderOne.uuid == currentFolderUuid) {
                    folderOne = null
                }
                if (folderTwo != null && folderTwo.uuid == currentFolderUuid) {
                    folderTwo = null
                }
            }
            // sort folders to the bottom of the list, apart from the current folder
            when {
                folderOne === folderTwo -> 0
                folderOne == null -> -1
                folderTwo == null -> 1
                else -> 0
            }
        }
        val podcastComparator: Comparator<PodcastFolder>? = when (sortType) {
            PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST -> addDateComparator
            PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST -> null
            else -> aToZComparator
        }
        val comparator = if (podcastComparator == null) folderComparator else folderComparator.then(podcastComparator)

        // move the folders to the bottom of the list
        return podcastsSortedByReleaseDate.sortedWith(comparator)
    }
}
