package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoggingInScreenViewModel @Inject constructor(
    private val syncManager: SyncManager,
) : ViewModel() {

    fun getEmail() = syncManager.getEmail()
}
