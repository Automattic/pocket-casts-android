package au.com.shiftyjelly.pocketcasts.analytics.experiments

sealed class Variation {
    data object Control : Variation()
    data class Treatment(val name: String? = null) : Variation()
}
