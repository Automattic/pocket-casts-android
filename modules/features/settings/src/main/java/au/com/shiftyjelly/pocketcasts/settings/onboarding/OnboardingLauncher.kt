package au.com.shiftyjelly.pocketcasts.settings.onboarding

import android.app.Activity
import android.content.Intent

interface OnboardingLauncher {
    fun launchIntent(onboardingFlow: OnboardingFlow): Intent

    fun openOnboardingFlow(onboardingFlow: OnboardingFlow)

    companion object {
        fun launchIntent(activity: Activity, onboardingFlow: OnboardingFlow): Intent {
            require(activity is OnboardingLauncher) {
                "Unable to launch onboarding flow because the activity is not an ${OnboardingLauncher::class.simpleName}"
            }
            return activity.launchIntent(onboardingFlow)
        }

        fun openOnboardingFlow(activity: Activity, onboardingFlow: OnboardingFlow) {
            require(activity is OnboardingLauncher) {
                "Unable to launch onboarding flow because the activity is not an ${OnboardingLauncher::class.simpleName}"
            }
            activity.openOnboardingFlow(onboardingFlow)
        }
    }
}
