package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import au.com.shiftyjelly.pocketcasts.onboarding.TvOnboardingNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvOnboardingNavHost()
        }
    }
}
