package au.com.shiftyjelly.pocketcasts.analytics.experiments

enum class TrialCtaCopyTreatment(val treatmentName: String) {
    START_30_DAY_TRIAL("start_30_day_trial"), // "Start your 30-day Free Trial"
    TRY_30_DAYS_FREE("try_30_days_free"), // "Try 30 days for free"
    ;

    companion object {
        fun fromTreatmentName(name: String?): TrialCtaCopyTreatment? =
            entries.firstOrNull { it.treatmentName == name }
    }
}
