package au.com.shiftyjelly.pocketcasts.models.to

import java.util.Date

sealed class RefreshState {
    data object Never : RefreshState()
    data object Refreshing : RefreshState()
    data class Success(val date: Date) : RefreshState()
    data class Failed(val error: String = "Unknown error") : RefreshState()
}
