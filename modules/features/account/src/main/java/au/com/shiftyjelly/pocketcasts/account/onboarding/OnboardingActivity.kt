package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingActivityViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.reactive.asFlow

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {
    @Inject
    lateinit var theme: Theme

    @Inject
    lateinit var userManager: UserManager

    private val viewModel by viewModels<OnboardingActivityViewModel>()

    private val upgradeFeaturesViewModel by viewModels<OnboardingUpgradeFeaturesViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<OnboardingUpgradeFeaturesViewModel.Factory> { factory ->
                factory.create(onboardingFlow)
            }
        },
    )

    private val onboardingFlow
        get() = requireNotNull(IntentCompat.getParcelableExtra(intent, ANALYTICS_FLOW_KEY, OnboardingFlow::class.java)) {
            "Onboarding flow is not set"
        }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make content edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        setContent {
            val onboardingState by upgradeFeaturesViewModel.state.collectAsState()
            val signInState = userManager.getSignInState().asFlow().collectAsState(null)
            val currentSignInState = signInState.value

            val finishState = viewModel.finishState.collectAsState(null)
            finishState.value?.let { finishWithResult(it) }

            if (currentSignInState != null) {
                if (shouldSetupTheme(onboardingFlow)) {
                    theme.setupThemeForConfig(this, resources.configuration)
                }

                enableEdgeToEdge()

                OnboardingFlowComposable(
                    featuresViewModel = upgradeFeaturesViewModel,
                    state = onboardingState,
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
            Intent().putExtra(OnboardingActivityContract.FINISH_KEY, result),
        )
        finish()
    }

    private fun shouldSetupTheme(onboardingFlow: OnboardingFlow) = (onboardingFlow !is OnboardingFlow.PlusAccountUpgrade)

    companion object {
        fun newInstance(context: Context, onboardingFlow: OnboardingFlow): Intent {
            return Intent(context, OnboardingActivity::class.java).putExtra(ANALYTICS_FLOW_KEY, onboardingFlow)
        }

        private const val ANALYTICS_FLOW_KEY = "analytics_flow"
    }
}
