package au.com.shiftyjelly.pocketcasts.analytics

enum class AnalyticsEvent(val key: String) {
    APPLICATION_INSTALLED("application_installed"),
    APPLICATION_UPDATED("application_updated"),
    APPLICATION_OPENED("application_opened"),
    APPLICATION_CLOSED("application_closed"),
}
