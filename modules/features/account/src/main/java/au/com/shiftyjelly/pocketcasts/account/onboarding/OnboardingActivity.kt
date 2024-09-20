package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingActivityViewModel
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.reactive.asFlow

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    @Inject lateinit var theme: Theme

    @Inject lateinit var userManager: UserManager

    private val viewModel: OnboardingActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make content edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val signInState = userManager.getSignInState().asFlow().collectAsState(null)
            val currentSignInState = signInState.value

            val finishState = viewModel.finishState.collectAsState(null)
            finishState.value?.let { finishWithResult(it) }

            if (currentSignInState != null) {
                val onboardingFlow = remember(savedInstanceState) {
                    IntentCompat.getParcelableExtra(intent, ANALYTICS_FLOW_KEY, OnboardingFlow::class.java)
                } ?: throw IllegalStateException("Analytics flow not set")

                if (shouldSetupTheme(onboardingFlow)) {
                    theme.setupThemeForConfig(this, resources.configuration)
                }

                enableEdgeToEdge()

                OnboardingFlowComposable(
                    theme = theme.activeTheme,
                    flow = onboardingFlow,
                    exitOnboarding = { viewModel.onExitOnboarding(it) },
                    completeOnboardingToDiscover = { finishWithResult(OnboardingFinish.DoneGoToDiscover) },
                    signInState = currentSignInState,
                    onUpdateSystemBars = { value ->
                        enableEdgeToEdge(value.statusBarStyle, value.navigationBarStyle)
                    },
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
            },
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
