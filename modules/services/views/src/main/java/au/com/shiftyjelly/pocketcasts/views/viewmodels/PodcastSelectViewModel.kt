package au.com.shiftyjelly.pocketcasts.views.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.views.fragments.SelectablePodcast
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PodcastSelectViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
) : ViewModel() {

    private val _selectablePodcasts = MutableStateFlow<List<SelectablePodcast>>(emptyList())
    val selectablePodcasts: StateFlow<List<SelectablePodcast>> = _selectablePodcasts

    fun loadSelectablePodcasts(selectedUuids: List<String>) {
        viewModelScope.launch {
            val podcasts = withContext(Dispatchers.IO) {
                podcastManager.findSubscribedBlocking()
            }

            val sortedSelectable = podcasts
                .sortedBy { PodcastsSortType.cleanStringForSort(it.title) }
                .map { SelectablePodcast(it, selectedUuids.contains(it.uuid)) }

            _selectablePodcasts.value = sortedSelectable
        }
    }
}
