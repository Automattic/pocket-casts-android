package au.com.shiftyjelly.pocketcasts.repositories.shortcuts

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import au.com.shiftyjelly.pocketcasts.repositories.extensions.shortcutDrawableId
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PocketCastsShortcuts {

    const val INTENT_EXTRA_PAGE = "launch-page"
    const val INTENT_EXTRA_PLAYLIST_ID = "playlist-id"

    /**
     * Icon shortcuts
     * - Podcasts
     * - Search
     * - Up Next
     * - Top Filter
     */
    @OptIn(DelicateCoroutinesApi::class)
    @TargetApi(Build.VERSION_CODES.N_MR1)
    fun update(playlistManager: PlaylistManager, force: Boolean, context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return
        }

        GlobalScope.launch(Dispatchers.Main) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return@launch

            val topPlaylist = withContext(Dispatchers.Default) { playlistManager.findAll().firstOrNull() }

            if (topPlaylist == null) {
                if (shortcutManager.dynamicShortcuts.size == 1) {
                    shortcutManager.removeAllDynamicShortcuts()
                }
                return@launch
            }

            if (shortcutManager.dynamicShortcuts.isEmpty() || force) {
                val filterIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return@launch
                filterIntent.action = Intent.ACTION_VIEW
                filterIntent.putExtra(INTENT_EXTRA_PAGE, "playlist")
                filterIntent.putExtra(INTENT_EXTRA_PLAYLIST_ID, topPlaylist.id)

                val playlistTitle = topPlaylist.title.ifEmpty { "Top filter" }

                val playlistShortcut = ShortcutInfo.Builder(context, "top_filter")
                    .setShortLabel(playlistTitle)
                    .setLongLabel(playlistTitle)
                    .setIcon(Icon.createWithResource(context, topPlaylist.shortcutDrawableId))
                    .setIntent(filterIntent)
                    .build()

                shortcutManager.dynamicShortcuts = listOf(playlistShortcut)
                if (shortcutManager.pinnedShortcuts.isNotEmpty()) {
                    shortcutManager.updateShortcuts(listOf(playlistShortcut))
                }
            }
        }
    }
}
