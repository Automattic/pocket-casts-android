package au.com.shiftyjelly.pocketcasts.settings.onboarding

enum class OnboardingUpgradeSource(val analyticsValue: String) {
    NEEDS_LOGIN("needs_login"),
    PLUS_DETAILS("plus_details"),
    PROFILE("profile"),
    RECOMMENDATIONS("recommendations"),
}
