package au.com.shiftyjelly.pocketcasts.models.to

import java.util.Date

sealed class RefreshState {
    object Never : RefreshState()
    object Refreshing : RefreshState()
    data class Success(val date: Date) : RefreshState()
    data class Failed(val error: String = "Unknown error") : RefreshState()
}
