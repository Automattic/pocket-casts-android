package au.com.shiftyjelly.pocketcasts.analytics.experiments

interface ExperimentType {
    val identifier: String
}

enum class Experiment(override val identifier: String) : ExperimentType {
    PaywallUpgradeABTest("pocketcasts_paywall_upgrade_android_ab_test"),
    NewOnboardingABTest("pocketcasts-new-onboarding-android-aa-test"),
    ;

    companion object {
        fun getAllExperiments(): Set<Experiment> = entries.toSet()
    }
}
