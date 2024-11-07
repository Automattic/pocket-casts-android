package au.com.shiftyjelly.pocketcasts.analytics.experiments

interface ExperimentType {
    val identifier: String
}

enum class Experiment(override val identifier: String) : ExperimentType {
    PaywallUpgradeAATest("pocketcasts_paywall_android_aa_test"),
    ;

    companion object {
        fun getAllExperiments(): Set<Experiment> = entries.toSet()
    }
}
