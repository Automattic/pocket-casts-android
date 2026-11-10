package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
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
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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

    val autoAddPodcasts = combine(
        podcastManager.autoAddToUpNextPodcastsFlow(),
        settings.autoAddUpNextLimit.flow,
        settings.autoAddUpNextLimitBehaviour.flow,
    ) { podcasts, limit, behaviour ->
        AutoAddSettingsState(podcasts, limit, behaviour)
    }.asLiveData()

    fun updatePodcast(podcast: Podcast, autoAddOption: Podcast.AutoAddUpNext) {
        viewModelScope.launch {
            Timber.d("Updating ${podcast.title} to $autoAddOption")
            podcastManager.updateAutoAddToUpNext(podcast, autoAddOption)
            eventHorizon.track(
                SettingsAutoAddUpNextPodcastPositionOptionChangedEvent(
                    value = autoAddOption.analyticsValue,
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
                value = behavior.analyticsValue,
            ),
        )
    }
}
