package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastAutoArchiveViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val settings: Settings,
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
            podcast.autoArchiveAfterPlaying = settings.autoArchiveAfterPlaying.value
            podcast.autoArchiveInactive = settings.autoArchiveInactive.value
            podcastManager.updatePodcast(podcast)
        }
    }

    fun updateAfterPlaying(value: AutoArchiveAfterPlaying) {
        launch {
            val podcast = podcast.value ?: return@launch
            podcast.autoArchiveAfterPlaying = value
            podcastManager.updatePodcast(podcast)
        }
    }

    fun updateInactive(value: AutoArchiveInactive) {
        launch {
            val podcast = podcast.value ?: return@launch
            podcast.autoArchiveInactive = value
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
