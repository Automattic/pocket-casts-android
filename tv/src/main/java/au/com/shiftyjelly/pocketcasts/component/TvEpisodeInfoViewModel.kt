package au.com.shiftyjelly.pocketcasts.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TvEpisodeInfoViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
) : ViewModel() {
    private val _podcastTitle = MutableStateFlow<PodcastTitle?>(null)
    val podcastTitle: StateFlow<PodcastTitle?> = _podcastTitle.asStateFlow()

    private var loadedUuid: String? = null

    fun load(podcastUuid: String) {
        if (podcastUuid == loadedUuid) {
            return
        }
        loadedUuid = podcastUuid
        viewModelScope.launch {
            _podcastTitle.value = PodcastTitle(podcastUuid, podcastManager.findPodcastByUuid(podcastUuid)?.title)
        }
    }

    data class PodcastTitle(val podcastUuid: String, val title: String?)
}
