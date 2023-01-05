package au.com.shiftyjelly.pocketcasts.account.onboarding

enum class OnboardingAnalyticsFlow(val value: String) {
    LOGGED_OUT("logged_out"),
    INITIAL_ONBOARDING("initial_onboarding"),
    PLUS_UPSELL("plus_upsell"),
    PLUS_ACCOUNT_UPGRADE("plus_account_upgrade"),
    PLUS_ACCOUNT_UPGRADE_NEEDS_LOGIN("plus_account_upgrade_needs_login"),
}
