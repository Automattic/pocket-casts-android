package au.com.shiftyjelly.pocketcasts.analytics

enum class AnalyticsEvent(val key: String) {

    // App lifecycle events
    APPLICATION_INSTALLED("application_installed"),
    APPLICATION_UPDATED("application_updated"),
    APPLICATION_OPENED("application_opened"),
    APPLICATION_CLOSED("application_closed"),

    // User lifecycle events
    USER_SIGNED_IN("user_signed_in"),
    USER_SIGNIN_FAILED("user_signin_failed")
}
