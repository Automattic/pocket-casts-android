package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

sealed interface WatchSyncState {
    data object Idle : WatchSyncState
    data object Syncing : WatchSyncState
    data object Success : WatchSyncState
    data class Failed(val error: WatchSyncError) : WatchSyncState
}

sealed interface WatchSyncError {
    data object Timeout : WatchSyncError
    data object NoPhoneConnection : WatchSyncError
    data class LoginFailed(val message: String?) : WatchSyncError
    data class Unknown(val message: String?) : WatchSyncError
}
