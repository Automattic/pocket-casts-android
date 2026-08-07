package au.com.shiftyjelly.pocketcasts.analytics.experiments

interface ExperimentType {
    val identifier: String
}

enum class Experiment(override val identifier: String) : ExperimentType {
    PaywallUpgradeABTest("pocketcasts_paywall_upgrade_android_ab_test"),
    NewOnboardingABTest("pocketcasts_android_onboarding_promote_free_trial"),
    YearlyInstallments("pocketcasts_android_yearly_installments"),
    ;

    companion object {
        fun getAllExperiments(): Set<Experiment> = entries.toSet()
    }
}
