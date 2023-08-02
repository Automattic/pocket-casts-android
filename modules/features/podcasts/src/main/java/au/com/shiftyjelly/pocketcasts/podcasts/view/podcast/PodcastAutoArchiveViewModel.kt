package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class PodcastAutoArchiveViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val settings: Settings
) : ViewModel(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    lateinit var podcast: LiveData<Podcast>

    companion object {
        val EPISODE_LIMITS = listOf(null, 1, 2, 5, 10)
    }

    fun setup(podcastUUID: String) {
        podcast = podcastManager
            .observePodcastByUuid(podcastUUID)
            .subscribeOn(Schedulers.io())
            .toLiveData()
    }

    fun updateGlobalOverride(checked: Boolean) {
        launch {
            val podcast = podcast.value ?: return@launch
            podcast.overrideGlobalArchive = checked
            podcast.autoArchiveAfterPlaying = settings.autoArchiveAfterPlaying.flow.value.toIndex()
            podcast.autoArchiveInactive = settings.autoArchiveInactive.flow.value.toIndex()
            podcastManager.updatePodcast(podcast)
        }
    }

    fun updateAfterPlaying(index: Int) {
        launch {
            val podcast = podcast.value ?: return@launch
            podcast.autoArchiveAfterPlaying = index
            podcastManager.updatePodcast(podcast)
        }
    }

    fun updateInactive(index: Int) {
        launch {
            val podcast = podcast.value ?: return@launch
            podcast.autoArchiveInactive = index
            podcastManager.updatePodcast(podcast)
        }
    }

    fun updateEpisodeLimit(index: Int) {
        launch {
            val podcast = podcast.value ?: return@launch
            podcast.autoArchiveEpisodeLimit = EPISODE_LIMITS[index]
            podcastManager.updatePodcast(podcast)
        }
    }
}
