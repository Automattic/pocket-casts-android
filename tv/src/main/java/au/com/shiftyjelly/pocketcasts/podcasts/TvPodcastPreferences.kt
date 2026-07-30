package au.com.shiftyjelly.pocketcasts.podcasts

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvPodcastPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tv_podcasts", Context.MODE_PRIVATE)

    fun isShowingArchived(podcastUuid: String): Boolean {
        return prefs.getBoolean(showArchivedKey(podcastUuid), false)
    }

    fun setShowingArchived(podcastUuid: String, isShowingArchived: Boolean) {
        prefs.edit().putBoolean(showArchivedKey(podcastUuid), isShowingArchived).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun showArchivedKey(podcastUuid: String) = "show_archived_podcast_$podcastUuid"
}
