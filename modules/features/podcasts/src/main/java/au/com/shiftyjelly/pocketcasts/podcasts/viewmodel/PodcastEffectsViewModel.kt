package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.Debouncer
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastEffectsViewModel
@Inject constructor(
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
) : ViewModel(),
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    lateinit var podcast: LiveData<Podcast>
    private val playbackSpeedTrackingDebouncer = Debouncer()

    fun loadPodcast(uuid: String) {
        podcast = podcastManager
            .podcastByUuidRxFlowable(uuid)
            .subscribeOn(Schedulers.io())
            .toLiveData()
    }

    fun updateOverrideGlobalEffects(override: Boolean) {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.updateOverrideGlobalEffectsBlocking(podcast, override)
            if (shouldUpdatePlaybackManager()) {
                val effects = if (override) podcast.playbackEffects else settings.globalPlaybackEffects.value
                playbackManager.updatePlayerEffects(effects)
            }
        }
    }

    fun updateTrimSilence(trimMode: TrimMode) {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.updateTrimModeBlocking(podcast, trimMode)
            if (shouldUpdatePlaybackManager()) {
                playbackManager.updatePlayerEffects(podcast.playbackEffects)
            }
        }
    }

    fun updateBoostVolume(boostVolume: Boolean) {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.updateVolumeBoostedBlocking(podcast, boostVolume)
            if (shouldUpdatePlaybackManager()) {
                playbackManager.updatePlayerEffects(podcast.playbackEffects)
            }
        }
    }

    fun increasePlaybackSpeed() {
        val podcast = this.podcast.value ?: return
        launch {
            changePlaybackSpeed(podcast.playbackSpeed + 0.1)
        }
    }

    fun decreasePlaybackSpeed() {
        val podcast = this.podcast.value ?: return
        launch {
            changePlaybackSpeed(podcast.playbackSpeed - 0.1)
        }
    }

    private fun changePlaybackSpeed(speed: Double) {
        val podcast = this.podcast.value ?: return
        val roundedSpeed = speed.roundedSpeed()
        podcastManager.updatePlaybackSpeedBlocking(podcast, roundedSpeed)
        if (shouldUpdatePlaybackManager()) {
            playbackManager.updatePlayerEffects(podcast.playbackEffects)
        }
        viewModelScope.launch {
            playbackSpeedTrackingDebouncer.debounce {
                trackPlaybackEffectsEvent(AnalyticsEvent.PLAYBACK_EFFECT_SPEED_CHANGED, mapOf(PlaybackManager.SPEED_KEY to roundedSpeed))
            }
        }
    }

    private fun shouldUpdatePlaybackManager(): Boolean {
        val podcast = this.podcast.value ?: return false
        val currentEpisode = playbackManager.upNextQueue.currentEpisode
        val podcastUuid = if (currentEpisode is PodcastEpisode) currentEpisode.podcastUuid else null
        return podcastUuid == podcast.uuid
    }

    fun trackPlaybackEffectsEvent(event: AnalyticsEvent, props: Map<String, Any> = emptyMap()) {
        playbackManager.trackPlaybackEffectsEvent(
            event = event,
            props = buildMap {
                putAll(props)
                put("settings", "local")
            },
            sourceView = SourceView.PODCAST_SETTINGS,
        )
    }
}
