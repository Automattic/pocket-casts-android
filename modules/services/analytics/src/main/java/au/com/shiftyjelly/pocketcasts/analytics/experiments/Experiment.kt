package au.com.shiftyjelly.pocketcasts.analytics.experiments

enum class Experiment(val identifier: String) {
    PaywallAATest("pocketcasts_paywall_android_aa_test"),
    ;

    companion object {
        fun getAllExperiments(): Set<Experiment> = entries.toSet()
    }
}
