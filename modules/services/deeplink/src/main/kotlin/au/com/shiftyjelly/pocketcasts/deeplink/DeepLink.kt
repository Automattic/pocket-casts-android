package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
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
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_NOTIFICATION_TAG
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_PAGE
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_PODCAST_UUID
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_SOURCE_VIEW

sealed interface DeepLink {
    companion object {
        const val ACTION_OPEN_DOWNLOADS = "INTENT_OPEN_APP_DOWNLOADING"
        const val ACTION_OPEN_ADD_BOOKMARK = "INTENT_OPEN_APP_ADD_BOOKMARK"
        const val ACTION_OPEN_CHANGE_BOOKMARK_TITLE = "INTENT_OPEN_APP_CHANGE_BOOKMARK_TITLE"
        const val ACTION_OPEN_BOOKMARK = "INTENT_OPEN_APP_VIEW_BOOKMARKS"
        const val ACTION_OPEN_DELETE_BOOKMARK = "INTENT_OPEN_APP_DELETE_BOOKMARK"
        const val ACTION_OPEN_PODCAST = "INTENT_OPEN_APP_PODCAST_UUID"
        const val ACTION_OPEN_EPISODE = "INTENT_OPEN_APP_EPISODE_UUID"

        const val EXTRA_BOOKMARK_UUID = "bookmark_uuid"
        const val EXTRA_PODCAST_UUID = "podcast_uuid"
        const val EXTRA_EPISODE_UUID = "episode_uuid"
        const val EXTRA_SOURCE_VIEW = "source_view"
        const val EXTRA_NOTIFICATION_TAG = "NOTIFICATION_TAG"
        const val EXTRA_PAGE = "launch-page"
        const val EXTRA_FILTER_ID = "playlist-id"
    }
}

sealed interface IntentableDeepLink : DeepLink {
    fun toIntent(context: Context): Intent
}

data object DownloadsDeepLink : IntentableDeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_DOWNLOADS)
}

data object AddBookmarkDeepLink : IntentableDeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_ADD_BOOKMARK)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

data class ChangeBookmarkTitleDeepLink(
    val bookmarkUuid: String,
) : IntentableDeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_CHANGE_BOOKMARK_TITLE)
        .putExtra(EXTRA_BOOKMARK_UUID, bookmarkUuid)
        .putExtra(EXTRA_NOTIFICATION_TAG, "${EXTRA_BOOKMARK_UUID}_$bookmarkUuid")
}

data class ShowBookmarkDeepLink(
    val bookmarkUuid: String,
) : IntentableDeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_BOOKMARK)
        .putExtra(EXTRA_BOOKMARK_UUID, bookmarkUuid)
        .putExtra(EXTRA_NOTIFICATION_TAG, "${EXTRA_BOOKMARK_UUID}_$bookmarkUuid")
}

data class DeleteBookmarkDeepLink(
    val bookmarkUuid: String,
) : IntentableDeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_DELETE_BOOKMARK)
        .putExtra(EXTRA_BOOKMARK_UUID, bookmarkUuid)
        .putExtra(EXTRA_NOTIFICATION_TAG, "${EXTRA_BOOKMARK_UUID}_$bookmarkUuid")
}

data class ShowPodcastDeepLink(
    val podcastUuid: String,
    val sourceView: String?,
) : IntentableDeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_PODCAST)
        .putExtra(EXTRA_PODCAST_UUID, podcastUuid)
        .putExtra(EXTRA_SOURCE_VIEW, sourceView)
}

data class ShowEpisodeDeepLink(
    val episodeUuid: String,
    val podcastUuid: String?,
    val sourceView: String?,
) : IntentableDeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_EPISODE)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(EXTRA_EPISODE_UUID, episodeUuid)
        .putExtra(EXTRA_PODCAST_UUID, podcastUuid)
        .putExtra(EXTRA_SOURCE_VIEW, sourceView)
}

sealed interface ShowPageDeepLink : IntentableDeepLink {
    val pageId: String

    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_VIEW)
        .putExtra(EXTRA_PAGE, pageId)
}

data object ShowPodcastsDeepLink : ShowPageDeepLink {
    override val pageId = "podcasts"
}

data object ShowDiscoverDeepLink : ShowPageDeepLink {
    override val pageId = "search"
}

data object ShowUpNextDeepLink : ShowPageDeepLink {
    override val pageId = "upnext"
}

data class ShowFilterDeepLink(
    val filterId: Long,
) : ShowPageDeepLink {
    override val pageId = "playlist"

    override fun toIntent(context: Context) = super.toIntent(context)
        .putExtra(EXTRA_FILTER_ID, filterId)
}

data object PocketCastsWebsiteDeepLink : DeepLink

data class ShowPodcastFromUrlDeepLink(
    val url: String,
) : DeepLink

private val Context.launcherIntent get() = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)) {
    "Missing launcher intent for $packageName"
}
