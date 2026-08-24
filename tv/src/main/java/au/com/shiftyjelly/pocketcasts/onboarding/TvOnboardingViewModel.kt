package au.com.shiftyjelly.pocketcasts.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.UpNextSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class TvOnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncManager: SyncManager,
    private val podcastManager: PodcastManager,
    settings: Settings,
) : ViewModel() {
    val startDestination: String = if (syncManager.isLoggedIn() && !settings.getFullySignedOut()) {
        TvOnboardingRoutes.HOME
    } else {
        TvOnboardingRoutes.LANDING
    }

    fun refreshOnLaunch() {
        podcastManager.refreshPodcasts("tv launch")
        UpNextSyncWorker.enqueue(syncManager, context)
    }
}
