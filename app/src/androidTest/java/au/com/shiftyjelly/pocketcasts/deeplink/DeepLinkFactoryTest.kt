package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeepLinkFactoryTest {
    private val factory = DeepLinkFactory()

    @Test
    fun downloads() {
        val intent = Intent().setAction("INTENT_OPEN_APP_DOWNLOADING")

        val deepLink = factory.create(intent)

        assertEquals(DownloadsDeepLink, deepLink)
    }

    @Test
    fun addBookmark() {
        val intent = Intent().setAction("INTENT_OPEN_APP_ADD_BOOKMARK")

        val deepLink = factory.create(intent)

        assertEquals(AddBookmarkDeepLink, deepLink)
    }

    @Test
    fun changeBookmarkTitle() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_CHANGE_BOOKMARK_TITLE")
            .putExtra("bookmark_uuid", "bookmark-id")

        val deepLink = factory.create(intent)

        assertEquals(ChangeBookmarkTitleDeepLink("bookmark-id"), deepLink)
    }

    @Test
    fun changeBookmarkTitleWithoutBookmarkUuid() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_CHANGE_BOOKMARK_TITLE")

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun showBookmark() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_VIEW_BOOKMARKS")
            .putExtra("bookmark_uuid", "bookmark-id")

        val deepLink = factory.create(intent)

        assertEquals(ShowBookmarkDeepLink("bookmark-id"), deepLink)
    }

    @Test
    fun showBookmarkWithoutBookmarkUuid() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_VIEW_BOOKMARKS")

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun deleteBookmark() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_DELETE_BOOKMARK")
            .putExtra("bookmark_uuid", "bookmark-id")

        val deepLink = factory.create(intent)

        assertEquals(DeleteBookmarkDeepLink("bookmark-id"), deepLink)
    }

    @Test
    fun deleteBookmarkWithoutBookmarkUuid() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_DELETE_BOOKMARK")

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun showPodcast() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_PODCAST_UUID")
            .putExtra("podcast_uuid", "Podcast ID")
            .putExtra("source_view", "Source View")

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastDeepLink("Podcast ID", "Source View"), deepLink)
    }

    @Test
    fun showPodcastWithoutPodcastId() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_PODCAST_UUID")

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun showPodcastWithoutSourceView() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_PODCAST_UUID")
            .putExtra("podcast_uuid", "Podcast ID")

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastDeepLink("Podcast ID", sourceView = null), deepLink)
    }

    @Test
    fun showEpisode() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_EPISODE_UUID")
            .putExtra("episode_uuid", "Episode ID")
            .putExtra("podcast_uuid", "Podcast ID")
            .putExtra("source_view", "Source View")

        val deepLink = factory.create(intent)

        assertEquals(ShowEpisodeDeepLink("Episode ID", "Podcast ID", "Source View"), deepLink)
    }

    // Notifications add numbers to the action to display multiple of them
    @Test
    fun showEpisodeWithActionEndingWithNumbers() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_EPISODE_UUID87123648710")
            .putExtra("episode_uuid", "Episode ID")
            .putExtra("podcast_uuid", "Podcast ID")
            .putExtra("source_view", "Source View")

        val deepLink = factory.create(intent)

        assertEquals(ShowEpisodeDeepLink("Episode ID", "Podcast ID", "Source View"), deepLink)
    }

    @Test
    fun showEpisodeWithoutEpisodeId() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_EPISODE_UUID")
            .putExtra("podcast_uuid", "Podcast ID")
            .putExtra("source_view", "Source View")

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun showEpisodeWithoutPodcastId() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_EPISODE_UUID")
            .putExtra("episode_uuid", "Episode ID")
            .putExtra("source_view", "Source View")

        val deepLink = factory.create(intent)

        assertEquals(ShowEpisodeDeepLink("Episode ID", podcastUuid = null, "Source View"), deepLink)
    }

    @Test
    fun showEpisodeWithoutSourceView() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_EPISODE_UUID")
            .putExtra("episode_uuid", "Episode ID")
            .putExtra("podcast_uuid", "Podcast ID")

        val deepLink = factory.create(intent)

        assertEquals(ShowEpisodeDeepLink("Episode ID", "Podcast ID", sourceView = null), deepLink)
    }
}
