package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.list.ListServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class ShareListIncomingViewModel
@Inject constructor(
    val podcastManager: PodcastManager,
    val listServerManager: ListServerManager,
    val playbackManager: PlaybackManager,
    val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel(), CoroutineScope {
    var isFragmentChangingConfigurations: Boolean = false
    val share = MutableLiveData<ShareState>()
    val subscribedUuids = LiveDataReactiveStreams.fromPublisher(
        podcastManager.getSubscribedPodcastUuids()
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .mergeWith(podcastManager.observePodcastSubscriptions())
    )

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    fun loadShareUrl(url: String) {
        share.postValue(ShareState.Loading)
        val id = listServerManager.extractShareListIdFromWebUrl(url) ?: return
        viewModelScope.launch {
            try {
                val list = listServerManager.openPodcastList(id)
                share.postValue(ShareState.Loaded(title = list.title, description = list.description, podcasts = list.fullPodcasts))
            } catch (ex: Exception) {
                share.postValue(ShareState.Error)
            }
        }
    }

    fun subscribeToPodcast(uuid: String) {
        launch {
            val podcast = podcastManager.findPodcastByUuid(uuid)
            if (podcast == null || !podcast.isSubscribed) {
                podcastManager.subscribeToPodcast(uuid, true)
            }
        }
    }

    fun unsubscribeFromPodcast(uuid: String) {
        podcastManager.unsubscribeAsync(uuid, playbackManager)
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun trackShareEvent(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        analyticsTracker.track(event, properties)
    }
}

sealed class ShareState {
    object Loading : ShareState()
    data class Loaded(val title: String?, val description: String?, val podcasts: List<Podcast>?) : ShareState()
    object Error : ShareState()
}
