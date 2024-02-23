package au.com.shiftyjelly.pocketcasts.settings.onboarding

enum class OnboardingUpgradeSource(val analyticsValue: String) {
    ACCOUNT_DETAILS("account_details"),
    APPEARANCE("appearance"),
    BOOKMARKS("bookmarks"),
    BOOKMARKS_SHELF_ACTION("bookmarks_shelf_action"),
    END_OF_YEAR("end_of_year"),
    FILES("files"),
    FOLDERS("folders"),
    HEADPHONE_CONTROLS_SETTINGS("headphone_controls_settings"),
    LOGIN("login"), // for login from within upsell screen
    LOGIN_PLUS_PROMOTION("login_plus_promotion"), // for login from outside upsell screen
    OVERFLOW_MENU("overflow_menu"),
    PLUS_DETAILS("plus_details"),
    PROFILE("profile"),
    RECOMMENDATIONS("recommendations"),
    SETTINGS("settings"),
    SLUMBER_STUDIOS("slumber_studios"),
    UNKNOWN("unknown"),
}
