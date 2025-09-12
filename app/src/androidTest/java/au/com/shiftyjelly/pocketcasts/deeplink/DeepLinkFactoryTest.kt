package au.com.shiftyjelly.pocketcasts.deeplink

import android.app.SearchManager
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_STREAM
import android.net.Uri
import android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeepLinkFactoryTest {
    private val factory = DeepLinkFactory(
        webBaseHost = "pocketcasts.com",
        listHost = "lists.pocketcasts.com",
        shareHost = "pca.st",
        webPlayerHost = "play.pocketcasts.com",
    )

    @Test
    fun downloads() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_DOWNLOADING")
            .setData(Uri.parse("pktc://profile/downloads"))

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

        assertEquals(ShowEpisodeDeepLink("Episode ID", "Podcast ID", "Source View", autoPlay = false), deepLink)
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

        assertEquals(ShowEpisodeDeepLink("Episode ID", "Podcast ID", "Source View", autoPlay = false), deepLink)
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

        assertEquals(ShowEpisodeDeepLink("Episode ID", podcastUuid = null, "Source View", autoPlay = false), deepLink)
    }

    @Test
    fun showEpisodeWithoutSourceView() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_EPISODE_UUID")
            .putExtra("episode_uuid", "Episode ID")
            .putExtra("podcast_uuid", "Podcast ID")

        val deepLink = factory.create(intent)

        assertEquals(ShowEpisodeDeepLink("Episode ID", "Podcast ID", sourceView = null, autoPlay = false), deepLink)
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
    fun showUpNextModal() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .putExtra("launch-page", "upnext")

        val deepLink = factory.create(intent)

        assertEquals(ShowUpNextModalDeepLink, deepLink)
    }

    @Test
    fun showUpNextTab() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://upnext?location=tab"))

        val deepLink = factory.create(intent)

        assertEquals(ShowUpNextTabDeepLink, deepLink)
    }

    @Test
    fun showFilter() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .putExtra("launch-page", "playlist")
            .putExtra("playlist_uuid", "id")
            .putExtra("playlist_type", "manual")

        val deepLink = factory.create(intent)

        assertEquals(ShowPlaylistDeepLink(playlistUuid = "id", playlistType = "manual"), deepLink)
    }

    @Test
    fun pocketCastsWebsiteGetDeepLink() {
        val urls = listOf(
            "https://pocketcasts.com/get",
            "https://pocketcasts.com/get/",
            "https://pocketcasts.com/get/something?query=value",
        )

        urls.forEach { url ->
            val intent = Intent()
                .setAction(ACTION_VIEW)
                .setData(Uri.parse(url))

            val deepLink = factory.create(intent)

            assertEquals(PocketCastsWebsiteGetDeepLink, deepLink)
        }
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

    @Test
    fun shareListHttp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("http://lists.pocketcasts.com/path/to/list"))

        val deepLink = factory.create(intent)

        assertEquals(ShareListDeepLink("/path/to/list", sourceView = null), deepLink)
    }

    @Test
    fun shareListHttps() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://lists.pocketcasts.com/path/to/list"))

        val deepLink = factory.create(intent)

        assertEquals(ShareListDeepLink("/path/to/list", sourceView = null), deepLink)
    }

    @Test
    fun shareListNative() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://sharelist/path/to/list"))

        val deepLink = factory.create(intent)

        assertEquals(ShareListDeepLink("/path/to/list", sourceView = null), deepLink)
    }

    @Test
    fun shareListWithoutPath() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://lists.pocketcasts.com/"))

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun shareListNativeWithoutPath() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://sharelist/"))

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun shareListWithQuery() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://lists.pocketcasts.com/path/to/list?someKey=someValue"))

        val deepLink = factory.create(intent)

        assertEquals(ShareListDeepLink("/path/to/list", sourceView = null), deepLink)
    }

    @Test
    fun shareListNativeWithQuery() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://sharelist/path/to/list?someKey=someValue"))

        val deepLink = factory.create(intent)

        assertEquals(ShareListDeepLink("/path/to/list", sourceView = null), deepLink)
    }

    @Test
    fun shareListWithSource() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://lists.pocketcasts.com/path/to/list?source_view=someValue"))

        val deepLink = factory.create(intent)

        assertEquals(ShareListDeepLink("/path/to/list", sourceView = "someValue"), deepLink)
    }

    @Test
    fun shareListNativeWithSource() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://sharelist/path/to/list?source_view=someValue"))

        val deepLink = factory.create(intent)

        assertEquals(ShareListDeepLink("/path/to/list", sourceView = "someValue"), deepLink)
    }

    @Test
    fun subscribeOnAndroid() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://subscribeonandroid.com/blubrry.com/feed/podcast/"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("https://blubrry.com/feed/podcast/"), deepLink)
    }

    @Test
    fun subscribeOnAndroidHttp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("http://subscribeonandroid.com/blubrry.com/feed/podcast/"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("http://blubrry.com/feed/podcast/"), deepLink)
    }

    @Test
    fun subscribeOnAndroidWithWwwHost() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://www.subscribeonandroid.com/blubrry.com/feed/podcast/"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("https://blubrry.com/feed/podcast/"), deepLink)
    }

    @Test
    fun subscribeOnAndroidWithTooShortPath() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://subscribeonandroid.com/bl"))

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun iTunes() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://itunes.apple.com/some/podcast"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("https://itunes.apple.com/some/podcast"), deepLink)
    }

    @Test
    fun applePodcasts() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://podcasts.apple.com/some/podcast"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("https://podcasts.apple.com/some/podcast"), deepLink)
    }

    @Test
    fun cloudFiles() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://cloudfiles"))

        val deepLink = factory.create(intent)

        assertEquals(CloudFilesDeepLink, deepLink)
    }

    @Test
    fun filtersTab() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://filters"))

        val deepLink = factory.create(intent)

        assertEquals(ShowFiltersDeepLink, deepLink)
    }

    @Test
    fun createAccount() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://signup"))

        val deepLink = factory.create(intent)

        assertEquals(CreateAccountDeepLink, deepLink)
    }

    @Test
    fun openApp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://open"))

        val deepLink = factory.create(intent)

        assertEquals(AppOpenDeepLink, deepLink)
    }

    @Test
    fun upgradeAccount() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://upgrade"))

        val deepLink = factory.create(intent)

        assertEquals(UpgradeAccountDeepLink, deepLink)
    }

    @Test
    fun promoCode() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://redeem/promo/ABC-123"))

        val deepLink = factory.create(intent)

        assertEquals(PromoCodeDeepLink("ABC-123"), deepLink)
    }

    @Test
    fun promoCodeWithLongPath() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://redeem/promo/with/some/long/path/ABC-123"))

        val deepLink = factory.create(intent)

        assertEquals(PromoCodeDeepLink("ABC-123"), deepLink)
    }

    @Test
    fun import() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://settings/import"))

        val deepLink = factory.create(intent)

        assertEquals(ImportDeepLink, deepLink)
    }

    @Test
    fun staffPicks() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://discover/staffpicks"))

        val deepLink = factory.create(intent)

        assertEquals(StaffPicksDeepLink, deepLink)
    }

    @Test
    fun trending() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://discover/trending"))

        val deepLink = factory.create(intent)

        assertEquals(TrendingDeepLink, deepLink)
    }

    @Test
    fun recommendations() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://discover/recommendations"))

        val deepLink = factory.create(intent)

        assertEquals(RecommendationsDeepLink, deepLink)
    }

    @Test
    fun nativeShare() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://some/link"))

        val deepLink = factory.create(intent)

        assertEquals(
            NativeShareDeepLink(
                uri = Uri.parse("pktc://some/link"),
                startTimestamp = null,
                endTimestamp = null,
            ),
            deepLink,
        )
    }

    @Test
    fun upsell() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://upsell"))

        val deepLink = factory.create(intent)

        assertEquals(UpsellDeepLink, deepLink)
    }

    @Test
    fun nativeShareWithStartTimestamp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://some/link?t=15"))

        val deepLink = factory.create(intent)

        assertEquals(
            NativeShareDeepLink(
                uri = Uri.parse("pktc://some/link?t=15"),
                startTimestamp = 15.seconds,
                endTimestamp = null,
            ),
            deepLink,
        )
    }

    @Test
    fun nativeShareWithTimestamps() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://some/link?t=75,125"))

        val deepLink = factory.create(intent)

        assertEquals(
            NativeShareDeepLink(
                uri = Uri.parse("pktc://some/link?t=75,125"),
                startTimestamp = 75.seconds,
                endTimestamp = 125.seconds,
            ),
            deepLink,
        )
    }

    @Test
    fun shareAsNativeShare() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/something"))

        val deepLink = factory.create(intent)

        assertEquals(
            NativeShareDeepLink(
                uri = Uri.parse("https://pca.st/something"),
                startTimestamp = null,
                endTimestamp = null,
            ),
            deepLink,
        )
    }

    @Test
    fun shareAsNativeShareWithStartTimestamp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/something?t=11"))

        val deepLink = factory.create(intent)

        assertEquals(
            NativeShareDeepLink(
                uri = Uri.parse("https://pca.st/something?t=11"),
                startTimestamp = 11.seconds,
                endTimestamp = null,
            ),
            deepLink,
        )
    }

    @Test
    fun shareAsNativeShareWithTimestamps() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/something?t=40,90"))

        val deepLink = factory.create(intent)

        assertEquals(
            NativeShareDeepLink(
                uri = Uri.parse("https://pca.st/something?t=40,90"),
                startTimestamp = 40.seconds,
                endTimestamp = 90.seconds,
            ),
            deepLink,
        )
    }

    @Test
    fun sharePodcast() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/podcast/podcast-id?source_view=source"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastDeepLink("podcast-id", sourceView = "source"), deepLink)
    }

    @Test
    fun shareEpisode() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/episode/episode-id?source_view=source"))

        val deepLink = factory.create(intent)

        assertEquals(
            ShowEpisodeDeepLink(
                episodeUuid = "episode-id",
                podcastUuid = null,
                sourceView = "source",
                autoPlay = false,
            ),
            deepLink,
        )
    }

    @Test
    fun shareEpisodeWithStartTimestamp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/episode/episode-id?t=15"))

        val deepLink = factory.create(intent)

        assertEquals(
            ShowEpisodeDeepLink(
                episodeUuid = "episode-id",
                podcastUuid = null,
                startTimestamp = 15.seconds,
                sourceView = null,
                autoPlay = false,
            ),
            deepLink,
        )
    }

    @Test
    fun shareEpisodeWithTimestamps() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/episode/episode-id?t=15,55"))

        val deepLink = factory.create(intent)

        assertEquals(
            ShowEpisodeDeepLink(
                episodeUuid = "episode-id",
                podcastUuid = null,
                startTimestamp = 15.seconds,
                endTimestamp = 55.seconds,
                sourceView = null,
                autoPlay = false,
            ),
            deepLink,
        )
    }

    @Test
    fun webPlayerSharePodcast() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://play.pocketcasts.com/podcasts/podcast-id?source_view=source"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastDeepLink("podcast-id", sourceView = "source"), deepLink)
    }

    @Test
    fun webBaseSharePodcast() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pocketcasts.com/podcasts/podcast-id?source_view=source"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastDeepLink("podcast-id", sourceView = "source"), deepLink)
    }

    @Test
    fun webPlayerShareEpisode() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://play.pocketcasts.com/podcasts/podcast-id/episode-id?source_view=source"))

        val deepLink = factory.create(intent)

        assertEquals(
            ShowEpisodeDeepLink(
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                sourceView = "source",
                autoPlay = false,
            ),
            deepLink,
        )
    }

    @Test
    fun webPlayerShareEpisodeWithStartTimestamp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://play.pocketcasts.com/podcasts/podcast-id/episode-id?source_view=source&t=15"))

        val deepLink = factory.create(intent)

        assertEquals(
            ShowEpisodeDeepLink(
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                startTimestamp = 15.seconds,
                sourceView = "source",
                autoPlay = false,
            ),
            deepLink,
        )
    }

    @Test
    fun shareEpisodeAutoPlayTrue() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/episode/episode-id?auto_play=true"))

        val deepLink = factory.create(intent)

        assertEquals(
            ShowEpisodeDeepLink(
                episodeUuid = "episode-id",
                podcastUuid = null,
                sourceView = null,
                autoPlay = true,
            ),
            deepLink,
        )
    }

    @Test
    fun shareEpisodeWithAutoPlayFalse() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/episode/episode-id?auto_play-false"))

        val deepLink = factory.create(intent)

        assertEquals(
            ShowEpisodeDeepLink(
                episodeUuid = "episode-id",
                podcastUuid = null,
                sourceView = null,
                autoPlay = false,
            ),
            deepLink,
        )
    }

    @Test
    fun shareItunesId() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/itunes/1671087656"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("https://pca.st/itunes/1671087656"), deepLink)
    }

    @Test
    fun shareHttp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("http://pca.st/podcast/podcast-id"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastDeepLink("podcast-id", sourceView = null), deepLink)
    }

    @Test
    fun opmlFromSend() {
        val intent = Intent()
            .setAction(ACTION_SEND)
            .putExtra(EXTRA_STREAM, Uri.parse("https://file.com"))

        val deepLink = factory.create(intent)

        assertEquals(OpmlImportDeepLink(Uri.parse("https://file.com")), deepLink)
    }

    @Test
    fun opmlFromSendWithoutExtra() {
        val intent = Intent()
            .setAction(ACTION_SEND)

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun opmlFromView() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("content://file.com"))

        val deepLink = factory.create(intent)

        assertEquals(OpmlImportDeepLink(Uri.parse("content://file.com")), deepLink)
    }

    @Test
    fun podcastUrlFromRss() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("rss://podcast.com"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("rss://podcast.com"), deepLink)
    }

    @Test
    fun podcastUrlFromFeed() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("feed://podcast.com"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("feed://podcast.com"), deepLink)
    }

    @Test
    fun podcastUrlFromPcast() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pcast://podcast.com"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("pcast://podcast.com"), deepLink)
    }

    @Test
    fun podcastUrlFromItpc() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("itpc://podcast.com"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("itpc://podcast.com"), deepLink)
    }

    @Test
    fun podcastUrlFromHttp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("http://podcast.com"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("http://podcast.com"), deepLink)
    }

    @Test
    fun podcastUrlFromHttps() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://podcast.com"))

        val deepLink = factory.create(intent)

        assertEquals(ShowPodcastFromUrlDeepLink("https://podcast.com"), deepLink)
    }

    @Test
    fun playFromSearch() {
        val intent = Intent()
            .setAction(INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            .putExtra(SearchManager.QUERY, "Search term")

        val deepLink = factory.create(intent)

        assertEquals(PlayFromSearchDeepLink("Search term"), deepLink)
    }

    @Test
    fun assistant1() {
        val intent = Intent()
            .putExtra("extra_accl_intent", true)

        val deepLink = factory.create(intent)

        assertEquals(AssistantDeepLink, deepLink)
    }

    @Test
    fun assistant2() {
        val intent = Intent()
            .putExtra("handled_by_nga", true)

        val deepLink = factory.create(intent)

        assertEquals(AssistantDeepLink, deepLink)
    }

    @Test
    fun signIn() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/sign-in"))

        val deepLink = factory.create(intent)

        assertEquals(SignInDeepLink(sourceView = null), deepLink)
    }

    @Test
    fun signInHttp() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("http://pca.st/sign-in"))

        val deepLink = factory.create(intent)

        assertEquals(SignInDeepLink(sourceView = null), deepLink)
    }

    @Test
    fun signInWithSource() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pca.st/sign-in?source_view=hello"))

        val deepLink = factory.create(intent)

        assertEquals(SignInDeepLink(sourceView = "hello"), deepLink)
    }

    @Test
    fun referralDeepLink() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("https://pocketcasts.com/redeem/abc"))

        val deepLink = factory.create(intent)

        assertEquals(ReferralsDeepLink(code = "abc"), deepLink)
    }

    @Test
    fun themes() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://settings/themes"))

        val deepLink = factory.create(intent)

        assertEquals(ThemesDeepLink, deepLink)
    }

    @Test
    fun smartFolders() {
        val intent = Intent()
            .setAction(ACTION_VIEW)
            .setData(Uri.parse("pktc://features/suggestedFolders"))

        val deepLink = factory.create(intent)

        assertEquals(SmartFoldersDeepLink, deepLink)
    }

    @Test
    fun developerOptions() {
        val intent = Intent(ACTION_VIEW)
            .setData(Uri.parse("pktc://developer_options"))

        val deeplink = factory.create(intent)

        assertEquals(DeveloperOptionsDeeplink, deeplink)
    }
}
