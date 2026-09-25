package au.com.shiftyjelly.pocketcasts.profile.blogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.BlogsListPodcastTappedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class BlogsViewModel @Inject constructor(
    podcastManager: PodcastManager,
    settings: Settings,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    val blogPodcasts: StateFlow<List<Podcast>?> = podcastManager.observeSubscribedWebFeedPodcasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val bottomInset: StateFlow<Int> = settings.bottomInset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun onPodcastTapped(podcastUuid: String) {
        eventHorizon.track(BlogsListPodcastTappedEvent(uuid = podcastUuid))
    }
}
