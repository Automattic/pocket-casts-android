package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeepLinkFactoryTest {
    private val factory = DeepLinkFactory(
        webBaseHost = "pocketcasts.com",
    )

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

    @Test
    fun showPodcasts() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .putExtra("launch-page", "podcasts")

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastsDeepLink, deepLink)
    }

    @Test
    fun showDiscover() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .putExtra("launch-page", "search")

        val deepLink = factory.create(intent)

        assertEquals(ShowDiscoverDeepLink, deepLink)
    }

    @Test
    fun showUpNext() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .putExtra("launch-page", "upnext")

        val deepLink = factory.create(intent)

        assertEquals(ShowUpNextDeepLink, deepLink)
    }

    @Test
    fun showFilter() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .putExtra("launch-page", "playlist")
            .putExtra("playlist-id", 10L)

        val deepLink = factory.create(intent)

        assertEquals(ShowFilterDeepLink(filterId = 10), deepLink)
    }

    @Test
    fun pocketCastsWebsiteDeepLink() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pocketcasts.com"))

        val deepLink = factory.create(intent)

        assertEquals(PocketCastsWebsiteDeepLink, deepLink)
    }

    @Test
    fun podloveHttps() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://subscribehttps/mypodcast.com/rss/123"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("https://mypodcast.com/rss/123"), deepLink)
    }

    @Test
    fun podloveHttpsWithParams() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://subscribehttps/mypodcast.com/rss/123?someKey=someValue"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("https://mypodcast.com/rss/123?someKey=someValue"), deepLink)
    }

    @Test
    fun podloveHttp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://subscribe/mypodcast.com/rss/123"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("http://mypodcast.com/rss/123"), deepLink)
    }

    @Test
    fun podloveHttpWithParams() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://subscribe/mypodcast.com/rss/123?someKey=someValue"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("http://mypodcast.com/rss/123?someKey=someValue"), deepLink)
    }

    @Test
    fun podloveWithWrongScheme() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://subscribehttps/mypodcast.com/rss/123"))

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun podloveWithWrongHost() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://subscribehttp/mypodcast.com/rss/123"))

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun podloveWithShortPath() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://subscribe/aa"))

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun sonos() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://applink?state=hello_world"))

        val deepLink = factory.create(intent)

        assertEquals(SonosDeepLink("hello_world"), deepLink)
    }

    @Test
    fun sonosWithoutState() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://applink"))

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }
}
