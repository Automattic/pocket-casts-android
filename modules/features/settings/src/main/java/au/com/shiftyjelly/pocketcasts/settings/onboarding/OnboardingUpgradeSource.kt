package au.com.shiftyjelly.pocketcasts.settings.onboarding

enum class OnboardingUpgradeSource(val analyticsValue: String) {
    ACCOUNT_DETAILS("account_details"),
    APPEARANCE("appearance"),
    BANNER_AD("banner_ad"),
    ICONS("icons"),
    THEMES("themes"),
    BOOKMARKS("bookmarks"),
    BOOKMARKS_SHELF_ACTION("bookmarks_shelf_action"),
    END_OF_YEAR("end_of_year"),
    FILES("files"),
    FOLDERS("folders"),
    SUGGESTED_FOLDERS("suggested_folders"),
    FOLDERS_PODCAST_SCREEN("folders_podcast_screen"),
    HEADPHONE_CONTROLS_SETTINGS("headphone_controls_settings"),
    LOGIN("login"), // for login from within upsell screen
    LOGIN_PLUS_PROMOTION("login_plus_promotion"), // for login from outside upsell screen
    OVERFLOW_MENU("overflow_menu"),
    PLUS_DETAILS("plus_details"),
    PROFILE("profile"),
    RECOMMENDATIONS("recommendations"),
    SKIP_CHAPTERS("skip_chapters"),
    SETTINGS("settings"),
    SLUMBER_STUDIOS("slumber_studios"),
    UP_NEXT_SHUFFLE("up_next_shuffle"),
    GENERATED_TRANSCRIPTS("generated_transcripts"),
    DEEP_LINK("deep_link"),
    UNKNOWN("unknown"),
}
