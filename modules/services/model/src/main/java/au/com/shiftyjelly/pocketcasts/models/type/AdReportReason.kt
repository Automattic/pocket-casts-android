package au.com.shiftyjelly.pocketcasts.models.type

enum class AdReportReason(val analyticsName: String) {
    Broken(analyticsName = "broken"),
    Malicious(analyticsName = "malicious"),
    TooFrequent(analyticsName = "too_often"),
    Other(analyticsName = "other"),
}
