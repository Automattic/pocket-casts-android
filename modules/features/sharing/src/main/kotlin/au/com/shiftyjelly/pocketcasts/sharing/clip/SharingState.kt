package au.com.shiftyjelly.pocketcasts.sharing.clip

data class SharingState(
    val step: Step,
    val iSharing: Boolean,
) {
    enum class Step {
        ClipSelection,
        PlatformSelection,
    }
}
