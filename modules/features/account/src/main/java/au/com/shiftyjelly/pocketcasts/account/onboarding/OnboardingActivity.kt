package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
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
            val signInState = userManager.getSignInState().asFlow().collectAsState(null)
            val currentSignInState = signInState.value

            if (currentSignInState != null) {

                val onboardingFlow = remember(savedInstanceState) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent?.getParcelableExtra(ANALYTICS_FLOW_KEY, OnboardingFlow::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent?.getParcelableExtra(ANALYTICS_FLOW_KEY) as OnboardingFlow?
                    }
                } ?: throw IllegalStateException("Analytics flow not set")

                if (shouldSetupTheme(onboardingFlow)) {
                    theme.setupThemeForConfig(this, resources.configuration)
                }

                OnboardingFlowComposable(
                    theme = theme.activeTheme,
                    flow = onboardingFlow,
                    exitOnboarding = { finishWithResult(OnboardingFinish.Done) },
                    completeOnboardingToDiscover = { finishWithResult(OnboardingFinish.DoneGoToDiscover) },
                    signInState = currentSignInState,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        theme.setupThemeForConfig(this, resources.configuration)
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

    private fun shouldSetupTheme(onboardingFlow: OnboardingFlow) =
        (onboardingFlow !is OnboardingFlow.PlusAccountUpgrade)

    companion object {
        fun newInstance(context: Context, onboardingFlow: OnboardingFlow) =
            Intent(context, OnboardingActivity::class.java).apply {
                putExtra(ANALYTICS_FLOW_KEY, onboardingFlow)
            }

        private const val ANALYTICS_FLOW_KEY = "analytics_flow"
    }
}
