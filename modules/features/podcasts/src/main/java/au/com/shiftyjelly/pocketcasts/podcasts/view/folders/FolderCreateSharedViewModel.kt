package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Used to share the created folder uuid from the FolderCreateFragment to the PodcastsFragment.
 * The PodcastsViewModel isn't shared as it would mean storing the view model in the activity rather than just the fragment.
 */
class FolderCreateSharedViewModel : ViewModel() {

    private val folderUuidMutable = MutableLiveData<String?>()
    val folderUuidLive: LiveData<String?>
        get() = folderUuidMutable

    var folderUuid: String?
        get() = folderUuidMutable.value
        set(value) { folderUuidMutable.value = value }
}
