package au.com.shiftyjelly.pocketcasts.playlists.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvPlaylistPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tv_playlists", Context.MODE_PRIVATE)

    fun isShowingArchived(playlistUuid: String): Boolean {
        return prefs.getBoolean(showArchivedKey(playlistUuid), false)
    }

    fun setShowingArchived(playlistUuid: String, isShowingArchived: Boolean) {
        prefs.edit().putBoolean(showArchivedKey(playlistUuid), isShowingArchived).apply()
    }

    @SuppressLint("ApplySharedPref")
    fun clearAll() {
        prefs.edit().clear().commit()
    }

    private fun showArchivedKey(playlistUuid: String) = "show_archived_playlist_$playlistUuid"
}
