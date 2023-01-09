package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.app.Activity

interface OnboardingLauncher {
    fun openOnboardingFlow(onboardingFlow: OnboardingFlow)

    companion object {
        fun openOnboardingFlow(activity: Activity?, onboardingFlow: OnboardingFlow) {
            if (activity is OnboardingLauncher) {
                (activity as OnboardingLauncher).openOnboardingFlow(onboardingFlow)
            } else {
                throw IllegalStateException("Unable to launch onboarding flow because the activity is not an ${OnboardingLauncher::class.simpleName}")
            }
        }
    }
}
