package au.com.shiftyjelly.pocketcasts.analytics.experiments

sealed class Experiment(val identifier: String) {
    data object PaywallAATest : Experiment(identifier = "pocketcasts_paywall_android_aa_test")
}
