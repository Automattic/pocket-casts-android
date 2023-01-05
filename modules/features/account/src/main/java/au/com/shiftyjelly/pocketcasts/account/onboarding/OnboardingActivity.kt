package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    @Inject lateinit var theme: Theme
    @Inject lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make content edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val signInState by userManager.getSignInState().asFlow().collectAsState(null)

            OnboardingFlowComposable(
                theme = theme.activeTheme,
                completeOnboarding = { finishWithResult(OnboardingFinish.Completed) },
                completeOnboardingToDiscover = { finishWithResult(OnboardingFinish.CompletedGoToDiscover) },
                signInState = signInState,
            )
        }
    }

    private fun finishWithResult(result: OnboardingFinish) {
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(OnboardingActivityContract.FINISH_KEY, result)
            }
        )
        finish()
    }
}
