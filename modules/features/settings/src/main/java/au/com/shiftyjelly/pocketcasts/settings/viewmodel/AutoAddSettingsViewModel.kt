package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoAddUpNextLimitBehaviour
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.rxkotlin.combineLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asObservable
import timber.log.Timber
import javax.inject.Inject

data class AutoAddSettingsState(val autoAddPodcasts: List<Podcast>, val limit: Int, val behaviour: AutoAddUpNextLimitBehaviour)

@HiltViewModel
class AutoAddSettingsViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) : ViewModel() {

    private var isFragmentChangingConfigurations: Boolean = false

    fun onShown() {
        if (!isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.SETTINGS_AUTO_ADD_UP_NEXT_SHOWN)
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    val autoAddPodcasts =
        podcastManager.observeAutoAddToUpNextPodcasts()
            .combineLatest(
                settings.autoAddUpNextLimit.flow
                    .asObservable(viewModelScope.coroutineContext)
                    .toFlowable(BackpressureStrategy.LATEST),
                settings.autoAddUpNextLimitBehaviour.flow
                    .asObservable(viewModelScope.coroutineContext)
                    .toFlowable(BackpressureStrategy.LATEST)
            )
            .map { AutoAddSettingsState(it.first, it.second, it.third) }
            .toLiveData()

    fun updatePodcast(podcast: Podcast, autoAddOption: Podcast.AutoAddUpNext) {
        viewModelScope.launch {
            Timber.d("Updating ${podcast.title} to $autoAddOption")
            podcastManager.updateAutoAddToUpNext(podcast, autoAddOption)
            analyticsTracker.track(
                AnalyticsEvent.SETTINGS_AUTO_ADD_UP_NEXT_PODCAST_POSITION_OPTION_CHANGED,
                mapOf("value" to autoAddOption.analyticsValue)
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
        settings.autoAddUpNextLimit.set(limit)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ADD_UP_NEXT_AUTO_ADD_LIMIT_CHANGED,
            mapOf("value" to limit)
        )
    }

    fun autoAddUpNextLimitBehaviorChanged(behavior: AutoAddUpNextLimitBehaviour) {
        settings.autoAddUpNextLimitBehaviour.set(behavior)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ADD_UP_NEXT_LIMIT_REACHED_CHANGED,
            mapOf(
                "value" to when (behavior) {
                    AutoAddUpNextLimitBehaviour.STOP_ADDING -> "stop_adding"
                    AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP -> "only_add_top"
                }
            )
        )
    }
}
