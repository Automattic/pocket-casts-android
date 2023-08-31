package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedBookmarksViewModel @Inject constructor() : ViewModel() {
    var multiSelectHelper: MultiSelectBookmarksHelper? = null
}
