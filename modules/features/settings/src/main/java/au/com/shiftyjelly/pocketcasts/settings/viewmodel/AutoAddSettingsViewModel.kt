package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.rxkotlin.combineLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AutoAddSettingsState(val autoAddPodcasts: List<Podcast>, val limit: Int, val behaviour: Settings.AutoAddUpNextLimitBehaviour)

@HiltViewModel
class AutoAddSettingsViewModel @Inject constructor(val podcastManager: PodcastManager, val settings: Settings) : ViewModel() {
    val autoAddPodcasts = LiveDataReactiveStreams.fromPublisher(
        podcastManager.observeAutoAddToUpNextPodcasts()
            .combineLatest(settings.autoAddUpNextLimit.toFlowable(BackpressureStrategy.LATEST), settings.autoAddUpNextLimitBehaviour.toFlowable(BackpressureStrategy.LATEST))
            .map { AutoAddSettingsState(it.first, it.second, it.third) }
    )

    fun updatePodcast(podcast: Podcast, autoAddOption: Int) {
        viewModelScope.launch {
            podcastManager.updateAutoAddToUpNext(podcast, autoAddOption)
            Timber.d("Updating ${podcast.title} to $autoAddOption")
        }
    }

    fun selectionUpdated(newSelection: List<String>) {
        viewModelScope.launch {
            val currentUuids = podcastManager.findAutoAddToUpNextPodcasts().map { it.uuid }
            val removedUuids = currentUuids - newSelection

            podcastManager.updateAutoAddToUpNextsIf(podcastUuids = newSelection, newValue = Podcast.AUTO_ADD_TO_UP_NEXT_PLAY_LAST, onlyIfValue = Podcast.AUTO_ADD_TO_UP_NEXT_OFF)
            podcastManager.updateAutoAddToUpNexts(podcastUuids = removedUuids, autoAddToUpNext = Podcast.AUTO_ADD_TO_UP_NEXT_OFF)
        }
    }
}
