package au.com.shiftyjelly.pocketcasts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import au.com.shiftyjelly.pocketcasts.onboarding.TvOnboardingNavHost
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TvActivity : ComponentActivity() {

    @Inject
    lateinit var launchRequests: TvLaunchRequests

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleLaunchIntent(intent)
        setContent {
            TvTheme {
                TvOnboardingNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
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
