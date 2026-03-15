package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoAddUpNextLimitBehaviour
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsAutoAddUpNextAutoAddLimitChangedEvent
import com.automattic.eventhorizon.SettingsAutoAddUpNextLimitReachedChangedEvent
import com.automattic.eventhorizon.SettingsAutoAddUpNextPodcastPositionOptionChangedEvent
import com.automattic.eventhorizon.SettingsAutoAddUpNextShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.rxkotlin.combineLatest
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asObservable
import timber.log.Timber

data class AutoAddSettingsState(val autoAddPodcasts: List<Podcast>, val limit: Int, val behaviour: AutoAddUpNextLimitBehaviour)

@HiltViewModel
class AutoAddSettingsViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) : ViewModel() {

    private var isFragmentChangingConfigurations: Boolean = false

    fun onShown() {
        if (!isFragmentChangingConfigurations) {
            eventHorizon.track(SettingsAutoAddUpNextShownEvent)
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    val autoAddPodcasts =
        podcastManager.autoAddToUpNextPodcastsRxFlowable()
            .combineLatest(
                settings.autoAddUpNextLimit.flow
                    .asObservable(viewModelScope.coroutineContext)
                    .toFlowable(BackpressureStrategy.LATEST),
                settings.autoAddUpNextLimitBehaviour.flow
                    .asObservable(viewModelScope.coroutineContext)
                    .toFlowable(BackpressureStrategy.LATEST),
            )
            .map { AutoAddSettingsState(it.first, it.second, it.third) }
            .toLiveData()

    fun updatePodcast(podcast: Podcast, autoAddOption: Podcast.AutoAddUpNext) {
        viewModelScope.launch {
            Timber.d("Updating ${podcast.title} to $autoAddOption")
            podcastManager.updateAutoAddToUpNext(podcast, autoAddOption)
            eventHorizon.track(
                SettingsAutoAddUpNextPodcastPositionOptionChangedEvent(
                    value = autoAddOption.eventHorizonValue,
                ),
            )
        }
    }

    fun selectionUpdated(newSelection: List<String>) {
        viewModelScope.launch {
            val currentUuids = podcastManager.findAutoAddToUpNextPodcasts().map { it.uuid }
            val removedUuids = currentUuids - newSelection

            podcastManager.updateAutoAddToUpNextsIf(podcastUuids = newSelection, newValue = Podcast.AutoAddUpNext.PLAY_LAST, onlyIfValue = Podcast.AutoAddUpNext.OFF)
            podcastManager.updateAutoAddToUpNexts(podcastUuids = removedUuids, autoAddToUpNext = Podcast.AutoAddUpNext.OFF)
        }
    }

    fun autoAddUpNextLimitChanged(limit: Int) {
        settings.autoAddUpNextLimit.set(limit, updateModifiedAt = true)
        eventHorizon.track(
            SettingsAutoAddUpNextAutoAddLimitChangedEvent(
                value = limit.toLong(),
            ),
        )
    }

    fun autoAddUpNextLimitBehaviorChanged(behavior: AutoAddUpNextLimitBehaviour) {
        settings.autoAddUpNextLimitBehaviour.set(behavior, updateModifiedAt = true)
        eventHorizon.track(
            SettingsAutoAddUpNextLimitReachedChangedEvent(
                value = behavior.eventHorizonValue,
            ),
        )
    }
}
