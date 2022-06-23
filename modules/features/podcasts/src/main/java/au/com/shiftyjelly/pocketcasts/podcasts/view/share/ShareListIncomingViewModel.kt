package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.ShareServerManager
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
    val shareServerManager: ShareServerManager,
    val playbackManager: PlaybackManager
) : ViewModel(), CoroutineScope {

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
        val id = shareServerManager.extractShareListIdFromWebUrl(url) ?: return
        shareServerManager.loadPodcastList(
            id,
            object : ShareServerManager.PodcastListCallback {
                override fun onSuccess(title: String?, description: String?, podcasts: List<Podcast>?) {
                    share.postValue(ShareState.Loaded(title, description, podcasts))
                }

                override fun onFailed() {
                    share.postValue(ShareState.Error)
                }
            }
        )
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
}

sealed class ShareState {
    object Loading : ShareState()
    data class Loaded(val title: String?, val description: String?, val podcasts: List<Podcast>?) : ShareState()
    object Error : ShareState()
}
