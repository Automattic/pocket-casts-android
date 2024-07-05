package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_ADD_BOOKMARK
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_BOOKMARK
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_CHANGE_BOOKMARK_TITLE
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_DOWNLOADS
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_BOOKMARK_UUID
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_NOTIFICATION_TAG

sealed interface DeepLink {
    fun toIntent(context: Context): Intent

    companion object {
        const val ACTION_OPEN_DOWNLOADS = "INTENT_OPEN_APP_DOWNLOADING"
        const val ACTION_OPEN_ADD_BOOKMARK = "INTENT_OPEN_APP_ADD_BOOKMARK"
        const val ACTION_OPEN_CHANGE_BOOKMARK_TITLE = "INTENT_OPEN_APP_CHANGE_BOOKMARK_TITLE"
        const val ACTION_OPEN_BOOKMARK = "INTENT_OPEN_APP_VIEW_BOOKMARKS"

        const val EXTRA_BOOKMARK_UUID = "bookmark_uuid"
        const val EXTRA_NOTIFICATION_TAG = "NOTIFICATION_TAG"
    }
}

data object DownloadsDeepLink : DeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_DOWNLOADS)
}

data object AddBookmarkDeepLink : DeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_ADD_BOOKMARK)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

data class ChangeBookmarkTitleDeepLink(
    val bookmarkUuid: String,
) : DeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_CHANGE_BOOKMARK_TITLE)
        .putExtra(EXTRA_BOOKMARK_UUID, bookmarkUuid)
        .putExtra(EXTRA_NOTIFICATION_TAG, "${EXTRA_BOOKMARK_UUID}_$bookmarkUuid")
}

data class ShowBookmarkDeepLink(
    val bookmarkUuid: String,
) : DeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_BOOKMARK)
        .putExtra(EXTRA_BOOKMARK_UUID, bookmarkUuid)
        .putExtra(EXTRA_NOTIFICATION_TAG, "${EXTRA_BOOKMARK_UUID}_$bookmarkUuid")
}

private val Context.launcherIntent get() = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)) {
    "Missing launcher intent for $packageName"
}
