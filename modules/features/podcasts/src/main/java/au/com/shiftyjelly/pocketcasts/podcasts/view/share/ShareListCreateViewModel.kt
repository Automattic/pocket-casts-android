package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.list.ListServiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ShareListCreateViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val listServiceManager: ListServiceManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    var isFragmentChangingConfigurations: Boolean = false
    data class State(
        val title: String = "",
        val description: String = "",
        val podcasts: List<Podcast> = emptyList(),
        val selectedPodcasts: Set<Podcast> = emptySet(),
    ) {
        val selectedPodcastsOrdered = podcasts.filter { selectedPodcasts.contains(it) }
    }

    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState

    init {
        viewModelScope.launch {
            val podcasts = podcastManager.findPodcastsOrderByTitle()
            mutableState.value = mutableState.value.copy(podcasts = podcasts)
        }
    }

    fun selectPodcast(podcast: Podcast) {
        val state = mutableState.value
        val selectedPodcasts = state.selectedPodcasts
            .toMutableSet()
            .apply { add(podcast) }
        mutableState.value = state.copy(selectedPodcasts = selectedPodcasts)
    }

    fun unselectPodcast(podcast: Podcast) {
        val state = mutableState.value
        val selectedPodcasts = state.selectedPodcasts
            .toMutableSet()
            .apply { remove(podcast) }
        mutableState.value = state.copy(selectedPodcasts = selectedPodcasts)
    }

    fun selectNone() {
        mutableState.value = mutableState.value.copy(selectedPodcasts = emptySet())
    }

    fun selectAll() {
        val state = mutableState.value
        mutableState.value = state.copy(selectedPodcasts = state.podcasts.toSet())
    }

    fun changeTitle(title: String) {
        mutableState.value = mutableState.value.copy(title = title)
    }

    fun changeDescription(description: String) {
        mutableState.value = mutableState.value.copy(description = description)
    }

    fun sharePodcasts(context: Context, label: String, onBefore: () -> Unit, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val stateValue = state.value
        val title = stateValue.title
        val description = stateValue.description

        val selectedPodcasts = stateValue.selectedPodcastsOrdered
        trackShareEvent(
            AnalyticsEvent.SHARE_PODCASTS_LIST_PUBLISH_STARTED,
            AnalyticsProp.countMap(selectedPodcasts.size),
        )
        onBefore()
        viewModelScope.launch {
            try {
                val url = listServiceManager.createPodcastList(
                    title = title,
                    description = description,
                    podcasts = selectedPodcasts,
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(context, Intent.createChooser(intent, label), null)
                trackShareEvent(
                    AnalyticsEvent.SHARE_PODCASTS_LIST_PUBLISH_SUCCEEDED,
                    AnalyticsProp.countMap(selectedPodcasts.size),
                )

                onSuccess()
            } catch (ex: Exception) {
                Timber.e(ex)
                trackShareEvent(
                    AnalyticsEvent.SHARE_PODCASTS_LIST_PUBLISH_FAILED,
                    AnalyticsProp.countMap(selectedPodcasts.size),
                )
                onFailure()
            }
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun trackShareEvent(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        analyticsTracker.track(event, properties)
    }

    private object AnalyticsProp {
        private const val count = "count"
        fun countMap(count: Int) = mapOf(this.count to count)
    }
}
