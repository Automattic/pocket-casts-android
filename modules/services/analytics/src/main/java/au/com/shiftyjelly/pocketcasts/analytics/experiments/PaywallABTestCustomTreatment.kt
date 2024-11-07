package au.com.shiftyjelly.pocketcasts.analytics.experiments

enum class PaywallABTestCustomTreatment(val treatmentName: String) {
    FEATURES_TREATMENT("features_treatment"),
    REVIEWS_TREATMENT("reviews_treatment"),
}
