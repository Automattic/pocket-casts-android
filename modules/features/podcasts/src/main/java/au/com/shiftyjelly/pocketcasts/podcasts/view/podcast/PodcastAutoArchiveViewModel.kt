package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel(assistedFactory = PodcastAutoArchiveViewModel.Factory::class)
class PodcastAutoArchiveViewModel @AssistedInject constructor(
    @Assisted private val podcastUuid: String,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) : ViewModel() {
    val podcast = podcastManager.observePodcastByUuid(podcastUuid)
        .asFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    fun updateGlobalOverride(checked: Boolean) {
        viewModelScope.launch {
            val podcast = podcast.firstOrNull() ?: return@launch
            podcastManager.updateArchiveSettings(
                podcast.uuid,
                checked,
                settings.autoArchiveAfterPlaying.value,
                settings.autoArchiveInactive.value,
            )
        }
    }

    fun updateAfterPlaying(value: AutoArchiveAfterPlaying) {
        viewModelScope.launch {
            podcast.firstOrNull()?.let {
                podcastManager.updateArchiveAfterPlaying(it.uuid, value)
            }
        }
    }

    fun updateInactive(value: AutoArchiveInactive) {
        viewModelScope.launch {
            podcast.firstOrNull()?.let {
                podcastManager.updateArchiveAfterInactive(it.uuid, value)
            }
        }
    }

    fun updateEpisodeLimit(value: Int?) {
        viewModelScope.launch {
            podcast.firstOrNull()?.let {
                podcastManager.updateArchiveEpisodeLimit(it.uuid, value)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(podcastUuid: String): PodcastAutoArchiveViewModel
    }
}
