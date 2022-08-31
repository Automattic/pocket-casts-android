package au.com.shiftyjelly.pocketcasts.analytics

enum class AnalyticsEvent(val key: String) {
    /* App Lifecycle */
    APPLICATION_INSTALLED("application_installed"),
    APPLICATION_UPDATED("application_updated"),
    APPLICATION_OPENED("application_opened"),
    APPLICATION_CLOSED("application_closed"),

    /* Plus Upsell */
    PLUS_PROMOTION_SHOWN("plus_promotion_shown"),
    PLUS_PROMOTION_DISMISSED("plus_promotion_dismissed"),
    PLUS_PROMOTION_UPGRADE_BUTTON_TAPPED("plus_promotion_upgrade_button_tapped"),

    /* Setup Account */
    SETUP_ACCOUNT_SHOWN("setup_account_shown"),
    SETUP_ACCOUNT_DISMISSED("setup_account_dismissed"),
    SETUP_ACCOUNT_BUTTON_TAPPED("setup_account_button_tapped"),

    /* Sign in */
    SIGNIN_SHOWN("signin_shown"),
    SIGNIN_DISMISSED("signin_dismissed"),

    /* Select Account Type */
    SELECT_ACCOUNT_TYPE_SHOWN("select_account_type_shown"),
    SELECT_ACCOUNT_TYPE_DISMISSED("select_account_type_dismissed"),
    SELECT_ACCOUNT_TYPE_BUTTON_TAPPED("select_account_type_button_tapped"),
}
