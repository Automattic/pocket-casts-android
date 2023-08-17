package au.com.shiftyjelly.pocketcasts.settings.onboarding

enum class OnboardingUpgradeSource(val analyticsValue: String) {
    APPEARANCE("appearance"),
    FILES("files"),
    FOLDERS("folders"),
    LOGIN("login"),
    PLUS_DETAILS("plus_details"),
    PROFILE("profile"),
    ACCOUNT_DETAILS("account_details"),
    RECOMMENDATIONS("recommendations"),
    SETTINGS("settings"),
    BOOKMARKS("bookmarks"),
}
