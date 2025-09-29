package au.com.shiftyjelly.pocketcasts.repositories.shortcuts

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.room.concurrent.AtomicBoolean
import au.com.shiftyjelly.pocketcasts.deeplink.ShowPlaylistDeepLink
import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaylistDao
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistShortcut
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.extensions.shortcutDrawableId
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Singleton
class DynamicShortcutsSynchronizer @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val playlistDao: PlaylistDao,
) {
    private val isMonitoring = AtomicBoolean()

    fun keepShortcutsInSync() {
        val shortcutManager = context.getSystemService<ShortcutManager>() ?: return
        if (!isMonitoring.getAndSet(true) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            scope.launch {
                playlistDao
                    .playlistShortcutFlow(allowManual = FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true))
                    .distinctUntilChanged()
                    .collect { playlist -> setDynamicShortcuts(shortcutManager, playlist) }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun setDynamicShortcuts(
        manager: ShortcutManager,
        playlist: PlaylistShortcut?,
    ) {
        if (playlist == null) {
            manager.removeDynamicShortcuts(listOf(TOP_FILTER_SHORTCUT_ID))
            return
        }

        val title = playlist.title.ifBlank { "Top filter" }
        val type = if (playlist.isManual) Playlist.Type.Manual else Playlist.Type.Smart
        val deepLink = ShowPlaylistDeepLink(
            playlistUuid = playlist.uuid,
            playlistType = type.analyticsValue,
        )
        val shortcut = ShortcutInfo.Builder(context, TOP_FILTER_SHORTCUT_ID)
            .setShortLabel(title)
            .setLongLabel(title)
            .setIcon(Icon.createWithResource(context, playlist.icon.shortcutDrawableId))
            .setIntent(deepLink.toIntent(context))
            .build()

        val shortcuts = listOf(shortcut)
        manager.dynamicShortcuts = shortcuts
        if (manager.pinnedShortcuts.isNotEmpty()) {
            manager.updateShortcuts(shortcuts)
        }
    }
}

private const val TOP_FILTER_SHORTCUT_ID = "top_filter"
