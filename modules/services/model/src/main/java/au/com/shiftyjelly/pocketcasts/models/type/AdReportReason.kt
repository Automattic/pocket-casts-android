package au.com.shiftyjelly.pocketcasts.models.type

import com.automattic.eventhorizon.BlazeAdReportType

enum class AdReportReason(
    val eventHorizonValue: BlazeAdReportType,
) {
    Broken(
        eventHorizonValue = BlazeAdReportType.Broken,
    ),
    Malicious(
        eventHorizonValue = BlazeAdReportType.Malicious,
    ),
    TooFrequent(
        eventHorizonValue = BlazeAdReportType.TooOften,
    ),
    Other(
        eventHorizonValue = BlazeAdReportType.Other,
    ),
}
