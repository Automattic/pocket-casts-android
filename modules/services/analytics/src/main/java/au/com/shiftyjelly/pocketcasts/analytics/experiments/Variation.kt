package au.com.shiftyjelly.pocketcasts.analytics.experiments

sealed class Variation {
    data object Control : Variation()
    data object Treatment : Variation()

    companion object {
        private const val TREATMENT = "treatment"
        fun fromName(name: String) = when (name) {
            TREATMENT -> Treatment
            else -> Control
        }
    }
}
