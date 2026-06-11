package au.com.shiftyjelly.pocketcasts.analytics.experiments

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

enum class TrialCtaCopyTreatment(val treatmentName: String) {
    START_30_DAY_TRIAL("start_30_day_trial"), // "Start your 30-day Free Trial"
    TRY_30_DAYS_FREE("try_30_days_free"), // "Try 30 days for free"
    ;

    companion object {
        fun fromTreatmentName(name: String?): TrialCtaCopyTreatment? = entries.firstOrNull { it.treatmentName == name }
    }
}

/**
 * Resolves the trial CTA copy treatment for the current user.
 *
 * Returns `null` (i.e. show the control copy) when the [Feature.TRIAL_CTA_COPY_AB_TEST] kill-switch
 * is off, when the user is in the control variation, or when the variation name is unrecognized.
 */
fun ExperimentProvider.getTrialCtaCopyTreatment(): TrialCtaCopyTreatment? {
    if (!FeatureFlag.isEnabled(Feature.TRIAL_CTA_COPY_AB_TEST)) {
        return null
    }
    val variation = getVariation(Experiment.TrialCtaCopyABTest)
    return (variation as? Variation.Treatment)?.let { TrialCtaCopyTreatment.fromTreatmentName(it.name) }
}
