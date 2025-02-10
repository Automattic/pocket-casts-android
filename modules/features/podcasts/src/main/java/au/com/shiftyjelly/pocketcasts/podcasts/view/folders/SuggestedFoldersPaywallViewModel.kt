package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SuggestedFoldersPaywallViewModel @Inject constructor(
    private val settings: Settings,
) : ViewModel() {

    fun dismissModal() {
        settings.suggestedFolderPaywallDismissTime.set(System.currentTimeMillis(), updateModifiedAt = false)
    }
}
