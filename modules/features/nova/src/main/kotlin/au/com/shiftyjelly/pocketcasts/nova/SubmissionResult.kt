package au.com.shiftyjelly.pocketcasts.nova

internal class SubmissionResult(
    val label: String,
    val itemCount: Int,
) {
    override fun toString() = "$label: $itemCount"
}
