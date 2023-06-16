package au.com.shiftyjelly.pocketcasts.servers.shownotes

sealed class ShowNotesState {
    data class Loaded(val showNotes: String) : ShowNotesState()
    object NotFound : ShowNotesState()
    object Loading : ShowNotesState()
    data class Error(val error: Throwable) : ShowNotesState()
}
