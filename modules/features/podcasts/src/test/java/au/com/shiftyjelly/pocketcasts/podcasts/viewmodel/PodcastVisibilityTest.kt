package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastVisibilityTest {
    private val unfinished = Podcast(uuid = "unfinished", isSubscribed = true)
    private val inProgress = Podcast(uuid = "in-progress", isSubscribed = true)
    private val caughtUp = Podcast(uuid = "caught-up", isSubscribed = true)

    private val folder = Folder(
        uuid = "folder",
        name = "Folder",
        color = 0,
        addedDate = Date(0),
        sortPosition = 0,
        podcastsSortType = PodcastsSortType.DRAG_DROP,
        deleted = false,
        syncModified = 0L,
    )

    @Test
    fun hidesPodcastsWithNoUnfinishedEpisodes() {
        val items = listOf(
            FolderItem.Podcast(unfinished),
            FolderItem.Podcast(caughtUp),
            FolderItem.Podcast(inProgress),
        )

        val filtered = filterPodcastsWithoutUnfinishedEpisodes(
            items,
            mapOf("unfinished" to 3, "in-progress" to 1),
        )

        assertEquals(listOf("unfinished", "in-progress"), filtered.map { it.uuid })
    }

    @Test
    fun missingAndZeroEpisodeCountsHidePodcasts() {
        val items = listOf(FolderItem.Podcast(unfinished), FolderItem.Podcast(caughtUp))
        val filtered = filterPodcastsWithoutUnfinishedEpisodes(items, mapOf("unfinished" to 0))

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun newUnfinishedEpisodeBringsPodcastBack() {
        val items = listOf(FolderItem.Podcast(caughtUp))
        val initially = filterPodcastsWithoutUnfinishedEpisodes(items, emptyMap())
        val updated = filterPodcastsWithoutUnfinishedEpisodes(items, mapOf("caught-up" to 1))

        assertTrue(initially.isEmpty())
        assertEquals(items, updated)
    }

    @Test
    fun foldersRemainVisibleWhenEveryPodcastIsCaughtUp() {
        val item = FolderItem.Folder(folder, listOf(unfinished, caughtUp))
        val filtered = filterPodcastsWithoutUnfinishedEpisodes(listOf(item), emptyMap())

        assertEquals("folder", filtered.single().uuid)
        assertTrue((filtered.single() as FolderItem.Folder).podcasts.isEmpty())
    }

    @Test
    fun filtersFolderPodcastsWithoutChangingMembership() {
        val item = FolderItem.Folder(folder, listOf(unfinished, caughtUp, inProgress))
        val filtered = filterPodcastsWithoutUnfinishedEpisodes(
            listOf(item),
            mapOf("unfinished" to 4, "in-progress" to 1),
        )

        assertEquals(listOf("unfinished", "in-progress"), (filtered.single() as FolderItem.Folder).podcasts.map { it.uuid })
        assertEquals(listOf("unfinished", "caught-up", "in-progress"), item.podcasts.map { it.uuid })
    }

    @Test
    fun keepsOriginalOrderingWhenNothingIsHidden() {
        val items = listOf(FolderItem.Podcast(inProgress), FolderItem.Podcast(unfinished))
        val filtered = filterPodcastsWithoutUnfinishedEpisodes(
            items,
            mapOf("unfinished" to 1, "in-progress" to 1),
        )

        assertEquals(items, filtered)
    }

    @Test
    fun unarchivingUnfinishedEpisodeBringsPodcastBack() {
        val items = listOf(FolderItem.Podcast(unfinished))

        assertTrue(filterPodcastsWithoutUnfinishedEpisodes(items, emptyMap()).isEmpty())
        assertEquals(items, filterPodcastsWithoutUnfinishedEpisodes(items, mapOf("unfinished" to 1)))
    }
}
