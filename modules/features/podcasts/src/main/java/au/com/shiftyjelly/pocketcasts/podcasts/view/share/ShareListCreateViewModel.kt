package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@HiltViewModel
class ShareListCreateViewModel @Inject constructor(val podcastManager: PodcastManager) : ViewModel() {

    val podcastsLive = LiveDataReactiveStreams.fromPublisher(podcastManager.findPodcastsOrderByTitleRx().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).toFlowable())
    val selectedUuidsLive = MutableLiveData<Set<String>>().apply { postValue(selectedUuids) }

    val podcasts: List<Podcast>
        get() { return podcastsLive.value ?: emptyList() }

    val selectedUuids = mutableSetOf<String>()

    private val disposables: CompositeDisposable = CompositeDisposable()

    val allPodcastSelected: Boolean
        get() = podcasts.isNotEmpty() && podcasts.size == selectedUuids.size

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun selectPodcast(podcast: Podcast) {
        selectedUuids.add(podcast.uuid)
        selectedUuidsLive.postValue(selectedUuids)
    }

    fun unselectPodcast(podcast: Podcast) {
        selectedUuids.remove(podcast.uuid)
        selectedUuidsLive.postValue(selectedUuids)
    }

    fun unselectAll() {
        selectedUuids.clear()
        selectedUuidsLive.postValue(selectedUuids)
    }

    fun selectAll() {
        for (podcast in podcasts) {
            selectedUuids.add(podcast.uuid)
        }
        selectedUuidsLive.postValue(selectedUuids)
    }

    // sort podcasts by title before sending them to the server
    fun getSelectedPodcasts(): List<Podcast> {
        val uuidToPodcast = mutableMapOf<String, Podcast>()
        for (podcast in podcasts) {
            uuidToPodcast[podcast.uuid] = podcast
        }
        val selectedPodcasts = mutableListOf<Podcast>()
        for (uuid in selectedUuids) {
            val podcast = uuidToPodcast[uuid] ?: continue
            selectedPodcasts.add(podcast)
        }
        selectedPodcasts.sortedWith { podcastOne, podcastTwo -> podcastOne.title.compareTo(podcastTwo.title, ignoreCase = true) }

        return selectedPodcasts
    }
}
