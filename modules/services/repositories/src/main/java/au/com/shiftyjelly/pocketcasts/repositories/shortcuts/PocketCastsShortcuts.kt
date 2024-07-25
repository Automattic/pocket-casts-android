package au.com.shiftyjelly.pocketcasts.repositories.shortcuts

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import au.com.shiftyjelly.pocketcasts.deeplink.ShowFilterDeepLink
import au.com.shiftyjelly.pocketcasts.repositories.extensions.shortcutDrawableId
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PocketCastsShortcuts {
    /**
     * Icon shortcuts
     * - Podcasts
     * - Search
     * - Up Next
     * - Top Filter
     */
    @TargetApi(Build.VERSION_CODES.N_MR1)
    fun update(
        playlistManager: PlaylistManager,
        force: Boolean,
        coroutineScope: CoroutineScope,
        context: Context,
        source: Source,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 || Util.getAppPlatform(context) != AppPlatform.Phone) {
            return
        }
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

        coroutineScope.launch(Dispatchers.Default) {
            val topPlaylist = playlistManager.findAll().firstOrNull()

            if (topPlaylist == null) {
                if (shortcutManager.dynamicShortcuts.size == 1) {
                    shortcutManager.removeAllDynamicShortcuts()
                }
                return@launch
            }
            LogBuffer.i(PocketCastsShortcuts::class.java.simpleName, "Shortcut update from ${source.value}, top filter title: ${topPlaylist.title}")

            if (shortcutManager.dynamicShortcuts.isEmpty() || force) {
                val filterId = topPlaylist.id ?: return@launch
                val filterIntent = ShowFilterDeepLink(filterId).toIntent(context)

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

    enum class Source(val value: String) {
        REFRESH_APP("refresh_app"),
        CREATE_PLAYLIST("create_playlist"),
        SAVE_PLAYLISTS_ORDER("save_playlists_order"),
        UPDATE_SHORTCUTS("update_shortcuts"),
    }
}
