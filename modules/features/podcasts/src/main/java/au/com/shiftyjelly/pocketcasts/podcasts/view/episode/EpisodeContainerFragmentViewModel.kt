package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import androidx.lifecycle.ViewModel
import com.automattic.eventhorizon.EpisodeDetailTabChangedEvent
import com.automattic.eventhorizon.EpisodeTabType
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EpisodeContainerFragmentViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    private var initialized = false

    fun onPageSelected(tabType: EpisodeTabType) {
        // Don't track the initial page selection because that is just the screen loading
        if (initialized) {
            eventHorizon.track(
                EpisodeDetailTabChangedEvent(
                    value = tabType,
                ),
            )
        }
        initialized = true
    }
}
