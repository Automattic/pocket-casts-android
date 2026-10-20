package au.com.shiftyjelly.pocketcasts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.KeepScreenOnEffect
import au.com.shiftyjelly.pocketcasts.onboarding.TvOnboardingNavHost
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class TvActivity : ComponentActivity() {

    @Inject
    lateinit var launchRequests: TvLaunchRequests

    @Inject
    lateinit var playbackManager: PlaybackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleLaunchIntent(intent)
        }
        setContent {
            TvTheme {
                val isPlaybackActive by remember {
                    playbackManager.playbackStateFlow
                        .map { it.isPlaying || it.isBuffering }
                        .distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = false)
                KeepScreenOnEffect(keepOn = isPlaybackActive)
                TvOnboardingNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        val isMediaCardLaunch = intent != null &&
            intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
            !intent.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        if (isMediaCardLaunch) {
            launchRequests.requestOpenNowPlaying()
        }
    }
}
