package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import au.com.shiftyjelly.pocketcasts.deeplink.BuildConfig.SERVER_LIST_HOST
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
