package au.com.shiftyjelly.pocketcasts.analytics.experiments

sealed class Variation {
    data object Control : Variation()
    data object Treatment : Variation()

    companion object {
        private const val CONTROL = "control"
        fun fromName(name: String?) = when (name) {
            CONTROL, null -> Control
            else -> Treatment
        }
    }
}
