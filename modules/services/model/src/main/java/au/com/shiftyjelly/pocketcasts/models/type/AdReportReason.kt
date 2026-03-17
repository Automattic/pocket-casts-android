package au.com.shiftyjelly.pocketcasts.models.type

import com.automattic.eventhorizon.BlazeAdReportType

enum class AdReportReason(
    val analyticsValue: BlazeAdReportType,
) {
    Broken(
        analyticsValue = BlazeAdReportType.Broken,
    ),
    Malicious(
        analyticsValue = BlazeAdReportType.Malicious,
    ),
    TooFrequent(
        analyticsValue = BlazeAdReportType.TooOften,
    ),
    Other(
        analyticsValue = BlazeAdReportType.Other,
    ),
}
