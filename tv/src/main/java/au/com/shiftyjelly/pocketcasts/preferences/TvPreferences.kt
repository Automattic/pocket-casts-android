package au.com.shiftyjelly.pocketcasts.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tv_preferences", Context.MODE_PRIVATE)

    fun isPlaylistShowingArchived(playlistUuid: String): Boolean {
        return prefs.getBoolean(playlistArchivedKey(playlistUuid), false)
    }

    fun setPlaylistShowingArchived(playlistUuid: String, isShowingArchived: Boolean) {
        prefs.edit().putBoolean(playlistArchivedKey(playlistUuid), isShowingArchived).apply()
    }

    fun isPodcastShowingArchived(podcastUuid: String): Boolean {
        return prefs.getBoolean(podcastArchivedKey(podcastUuid), false)
    }

    fun setPodcastShowingArchived(podcastUuid: String, isShowingArchived: Boolean) {
        prefs.edit().putBoolean(podcastArchivedKey(podcastUuid), isShowingArchived).apply()
    }

    @SuppressLint("ApplySharedPref")
    fun clearAll() {
        prefs.edit().clear().commit()
    }

    private fun playlistArchivedKey(playlistUuid: String) = "show_archived_playlist_$playlistUuid"

    private fun podcastArchivedKey(podcastUuid: String) = "show_archived_podcast_$podcastUuid"
}
