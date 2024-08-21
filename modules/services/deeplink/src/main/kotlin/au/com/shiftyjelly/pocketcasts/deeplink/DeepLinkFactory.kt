package au.com.shiftyjelly.pocketcasts.deeplink

import android.app.SearchManager
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_STREAM
import android.net.Uri
import android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
import androidx.core.content.IntentCompat
import au.com.shiftyjelly.pocketcasts.deeplink.BuildConfig.SERVER_LIST_HOST
import au.com.shiftyjelly.pocketcasts.deeplink.BuildConfig.SERVER_SHORT_URL
import au.com.shiftyjelly.pocketcasts.deeplink.BuildConfig.WEB_BASE_HOST
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_ADD_BOOKMARK
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_BOOKMARK
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_CHANGE_BOOKMARK_TITLE
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_DELETE_BOOKMARK
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_DOWNLOADS
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_EPISODE
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_PODCAST
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_BOOKMARK_UUID
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_EPISODE_UUID
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_FILTER_ID
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_PAGE
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_PODCAST_UUID
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_SOURCE_VIEW
import timber.log.Timber

class DeepLinkFactory(
    private val webBaseHost: String = WEB_BASE_HOST,
    private val listHost: String = SERVER_LIST_HOST,
    private val shareHost: String = SERVER_SHORT_URL.substringAfter("https://"),
) {
    private val adapters = listOf(
        DownloadsAdapter(),
        AddBookmarkAdapter(),
        ChangeBookmarkTitleAdapter(),
        ShowBookmarkAdapter(),
        DeleteBookmarkAdapter(),
        ShowPodcastAdapter(),
        ShowEpisodeAdapter(),
        ShowPageAdapter(),
        PocketCastsWebsiteAdapter(webBaseHost),
        PodloveAdapter(),
        SonosAdapter(),
        ShareListAdapter(listHost),
        ShareListNativeAdapter(),
        SubscribeOnAndroidAdapter(),
        AppleAdapter(),
        CloudFilesAdapter(),
        UpdageAccountAdapter(),
        PromoCodeAdapter(),
        ShareLinkNativeAdapter(),
        ShareLinkAdapter(shareHost),
        OpmlAdapter(listOf(listHost, shareHost)),
        PodcastUrlSchemeAdapter(listOf(listHost, shareHost)),
        PlayFromSearchAdapter(),
        AssistantAdapter(),
    )

    fun create(intent: Intent): DeepLink? {
        Timber.tag(TAG).i("Deep linking to: $intent")
        val deepLinks = adapters.mapNotNull { it.create(intent) }
        return when (deepLinks.size) {
            1 -> {
                val deepLink = deepLinks.first()
                Timber.tag(TAG).d("Found a matching deep link: $deepLink")
                deepLink
            }
            0 -> {
                Timber.tag(TAG).w("No matching deep links found")
                null
            }
            else -> {
                Timber.tag(TAG).w("Found multiple matching deep links: $deepLinks")
                deepLinks.first()
            }
        }
    }

    private companion object {
        val TAG = "DeepLinking"
    }
}

private interface DeepLinkAdapter {
    fun create(intent: Intent): DeepLink?
}

private class DownloadsAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_OPEN_DOWNLOADS) {
        DownloadsDeepLink
    } else {
        null
    }
}

private class AddBookmarkAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_OPEN_ADD_BOOKMARK) {
        AddBookmarkDeepLink
    } else {
        null
    }
}

private class ChangeBookmarkTitleAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_OPEN_CHANGE_BOOKMARK_TITLE) {
        intent.getStringExtra(EXTRA_BOOKMARK_UUID)?.let(::ChangeBookmarkTitleDeepLink)
    } else {
        null
    }
}

private class ShowBookmarkAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_OPEN_BOOKMARK) {
        intent.getStringExtra(EXTRA_BOOKMARK_UUID)?.let(::ShowBookmarkDeepLink)
    } else {
        null
    }
}

private class DeleteBookmarkAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_OPEN_DELETE_BOOKMARK) {
        intent.getStringExtra(EXTRA_BOOKMARK_UUID)?.let(::DeleteBookmarkDeepLink)
    } else {
        null
    }
}

private class ShowPodcastAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_OPEN_PODCAST) {
        intent.getStringExtra(EXTRA_PODCAST_UUID)?.let { podcastUuid ->
            ShowPodcastDeepLink(
                podcastUuid = podcastUuid,
                sourceView = intent.getStringExtra(EXTRA_SOURCE_VIEW),
            )
        }
    } else {
        null
    }
}

private class ShowEpisodeAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (ACTION_REGEX.matches(intent.action.orEmpty())) {
        intent.getStringExtra(EXTRA_EPISODE_UUID)?.let { episodeUuid ->
            ShowEpisodeDeepLink(
                episodeUuid = episodeUuid,
                podcastUuid = intent.getStringExtra(EXTRA_PODCAST_UUID),
                sourceView = intent.getStringExtra(EXTRA_SOURCE_VIEW),
            )
        }
    } else {
        null
    }

    private companion object {
        // We match on this pattern to handle notification intents that add numbers to actions for pending intents
        private val ACTION_REGEX = ("^" + ACTION_OPEN_EPISODE + """\d*$""").toRegex()
    }
}

private class ShowPageAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_VIEW) {
        when (intent.getStringExtra(EXTRA_PAGE)) {
            "podcasts" -> ShowPodcastsDeepLink
            "search" -> ShowDiscoverDeepLink
            "upnext" -> ShowUpNextDeepLink
            "playlist" -> ShowFilterDeepLink(filterId = intent.getLongExtra(EXTRA_FILTER_ID, -1))
            else -> null
        }
    } else {
        null
    }
}

private class PocketCastsWebsiteAdapter(
    private val webBaseHost: String,
) : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_VIEW && intent.data?.host == webBaseHost) {
        PocketCastsWebsiteDeepLink
    } else {
        null
    }
}

private class PodloveAdapter : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.dataString.orEmpty()
        val groupValues = PODLOVE_REGEX.matchEntire(uriData)?.groupValues

        return if (intent.action == ACTION_VIEW && groupValues != null) {
            val scheme = if (groupValues[1] == "subscribe") "http" else "https"
            ShowPodcastFromUrlDeepLink("$scheme://${groupValues[2]}")
        } else {
            null
        }
    }

    private companion object {
        private val PODLOVE_REGEX = """^pktc://(subscribe|subscribehttps)/(.{3,})$""".toRegex()
    }
}

private class SonosAdapter : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val scheme = uriData?.scheme
        val host = uriData?.host
        val state = uriData?.getQueryParameter("state")

        return if (intent.action == ACTION_VIEW && scheme == "pktc" && host == "applink" && state != null) {
            SonosDeepLink(state)
        } else {
            null
        }
    }
}

private class ShareListAdapter(
    private val listHost: String,
) : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val scheme = uriData?.scheme
        val host = uriData?.host
        val path = uriData?.path?.takeIf { it != "/" }

        return if (intent.action == ACTION_VIEW && scheme in listOf("http", "https") && host == listHost && path != null) {
            ShareListDeepLink(path)
        } else {
            null
        }
    }
}

private class ShareListNativeAdapter : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val scheme = uriData?.scheme
        val host = uriData?.host
        val path = uriData?.path?.takeIf { it != "/" }

        return if (intent.action == ACTION_VIEW && scheme == "pktc" && host == "sharelist" && path != null) {
            ShareListDeepLink(path)
        } else {
            null
        }
    }
}

// http://subscribeonandroid.com/geeknewscentral.com/podcast.xml
private class SubscribeOnAndroidAdapter : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val scheme = uriData?.scheme
        val host = uriData?.host
        val path = uriData?.path?.replaceFirst("/", "")?.takeIf { it.length >= 3 }

        return if (intent.action == ACTION_VIEW &&
            scheme in listOf("http", "https") &&
            host in listOf("subscribeonandroid.com", "www.subscribeonandroid.com") &&
            path != null
        ) {
            ShowPodcastFromUrlDeepLink("$scheme://$path")
        } else {
            null
        }
    }
}

private class AppleAdapter : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val host = uriData?.host

        return if (intent.action == ACTION_VIEW && host in listOf("itunes.apple.com", "podcasts.apple.com") && uriData != null) {
            ShowPodcastFromUrlDeepLink(uriData.toString())
        } else {
            null
        }
    }
}

private class CloudFilesAdapter : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val scheme = uriData?.scheme
        val host = uriData?.host

        return if (intent.action == ACTION_VIEW && scheme == "pktc" && host == "cloudfiles") {
            CloudFilesDeepLink
        } else {
            null
        }
    }
}

private class UpdageAccountAdapter : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val scheme = uriData?.scheme
        val host = uriData?.host

        return if (intent.action == ACTION_VIEW && scheme == "pktc" && host == "upgrade") {
            UpgradeAccountDeepLink
        } else {
            null
        }
    }
}

private class PromoCodeAdapter : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val dataString = uriData?.toString().orEmpty()
        val pathSegments = uriData?.pathSegments.orEmpty()

        return if (intent.action == ACTION_VIEW && dataString.startsWith("pktc://redeem/promo") && pathSegments.size >= 2) {
            PromoCodeDeepLink(pathSegments.last())
        } else {
            null
        }
    }
}

private class ShareLinkNativeAdapter : DeepLinkAdapter {
    private val timestampParser = SharingUrlTimestampParser()

    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val scheme = uriData?.scheme
        val host = uriData?.host
        val pathSegments = uriData?.pathSegments.orEmpty()

        return if (intent.action == ACTION_VIEW && scheme == "pktc" && pathSegments.isNotEmpty() && host !in EXCLUDED_HOSTS) {
            val timestamps = uriData.getQueryParameter("t")?.let(timestampParser::parseTimestamp)
            NativeShareDeepLink(
                uri = uriData,
                startTimestamp = timestamps?.first,
                endTimestamp = timestamps?.second,
            )
        } else {
            null
        }
    }

    private companion object {
        val EXCLUDED_HOSTS = listOf(
            "subscribe",
            "subscribehttps",
            "applink",
            "sharelist",
            "cloudfiles",
            "upgrade",
            "redeem",
        )
    }
}

private class ShareLinkAdapter(
    private val shareHost: String,
) : DeepLinkAdapter {
    private val timestampParser = SharingUrlTimestampParser()

    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val scheme = uriData?.scheme
        val host = uriData?.host

        return if (intent.action == ACTION_VIEW && scheme in listOf("http", "https") && host == shareHost) {
            val timestamps = uriData.getQueryParameter("t")?.let(timestampParser::parseTimestamp)
            when {
                uriData.pathSegments.size < 2 -> NativeShareDeepLink(
                    uri = uriData,
                    startTimestamp = timestamps?.first,
                    endTimestamp = timestamps?.second,
                )
                uriData.pathSegments[0] == "episode" -> ShowEpisodeDeepLink(
                    episodeUuid = uriData.pathSegments[1],
                    podcastUuid = null,
                    startTimestamp = timestamps?.first,
                    endTimestamp = timestamps?.second,
                    sourceView = null,
                )
                // handle the different podcast share links such as /podcast/uuid, /itunes/itunes_id, /feed/feed_url
                else -> ShowPodcastFromUrlDeepLink(uriData.toString())
            }
        } else {
            null
        }
    }
}

private class OpmlAdapter(
    excludedHosts: List<String>,
) : DeepLinkAdapter {
    private val excludedHosts = EXCLUDED_HOSTS + excludedHosts

    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val scheme = uriData?.scheme
        val host = uriData?.host

        return if (intent.action == ACTION_SEND) {
            val uri = IntentCompat.getParcelableExtra(intent, EXTRA_STREAM, Uri::class.java)
            uri?.let(::OpmlImportDeepLink)
        } else if (intent.action == ACTION_VIEW && uriData != null && scheme !in EXCLUDED_SCHEMES && host !in excludedHosts) {
            OpmlImportDeepLink(uriData)
        } else {
            null
        }
    }

    private companion object {
        val EXCLUDED_SCHEMES = listOf("rss", "feed", "pcast", "itpc", "http", "https")

        val EXCLUDED_HOSTS = listOf(
            "subscribe",
            "subscribehttps",
            "applink",
            "sharelist",
            "cloudfiles",
            "upgrade",
            "redeem",
            "subscribeonandroid.com",
            "www.subscribeonandroid.com",
        )
    }
}

private class PodcastUrlSchemeAdapter(
    excludedHosts: List<String>,
) : DeepLinkAdapter {
    private val excludedHosts = EXCLUDED_HOSTS + excludedHosts

    override fun create(intent: Intent): DeepLink? {
        val uriData = intent.data
        val scheme = uriData?.scheme
        val host = uriData?.host

        return if (intent.action == ACTION_VIEW && uriData != null && scheme in ALLOWED_SCHEMES && host !in excludedHosts) {
            ShowPodcastFromUrlDeepLink(uriData.toString())
        } else {
            null
        }
    }

    private companion object {
        val ALLOWED_SCHEMES = listOf("rss", "feed", "pcast", "itpc", "http", "https")

        val EXCLUDED_HOSTS = listOf(
            "subscribe",
            "subscribehttps",
            "applink",
            "sharelist",
            "cloudfiles",
            "upgrade",
            "redeem",
            "subscribeonandroid.com",
            "www.subscribeonandroid.com",
        )
    }
}

private class PlayFromSearchAdapter : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        val query = intent.extras?.getString(SearchManager.QUERY)
        return if (intent.action == INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH && !query.isNullOrBlank()) {
            PlayFromSearchDeepLink(query)
        } else {
            null
        }
    }
}

private class AssistantAdapter : DeepLinkAdapter {
    override fun create(intent: Intent): DeepLink? {
        return if (intent.extras?.getBoolean("extra_accl_intent", false) == true || intent.extras?.getBoolean("handled_by_nga", false) == true) {
            AssistantDeepLink
        } else {
            null
        }
    }
}
