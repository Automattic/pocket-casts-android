package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.clipToRange
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.round

@HiltViewModel
class PodcastEffectsViewModel
@Inject constructor(
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    lateinit var podcast: LiveData<Podcast>

    fun loadPodcast(uuid: String) {
        podcast = LiveDataReactiveStreams.fromPublisher(podcastManager.observePodcastByUuid(uuid).subscribeOn(Schedulers.io()))
    }

    fun updateOverrideGlobalEffects(override: Boolean) {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.updateOverrideGlobalEffects(podcast, override)
            if (shouldUpdatePlaybackManager()) {
                val effects = if (override) podcast.playbackEffects else settings.getGlobalPlaybackEffects()
                playbackManager.updatePlayerEffects(effects)
            }
        }
    }

    fun updateTrimSilence(trimMode: TrimMode) {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.updateTrimMode(podcast, trimMode)
            if (shouldUpdatePlaybackManager()) {
                playbackManager.updatePlayerEffects(podcast.playbackEffects)
            }
        }
    }

    fun updateBoostVolume(boostVolume: Boolean) {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.updateVolumeBoosted(podcast, boostVolume)
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
        var finalSpeed = speed.clipToRange(0.5, 3.0)
        // to stop the issue 1.2000000000000002
        finalSpeed = round(finalSpeed * 10.0) / 10.0
        podcastManager.updatePlaybackSpeed(podcast, finalSpeed)
        if (shouldUpdatePlaybackManager()) {
            playbackManager.updatePlayerEffects(podcast.playbackEffects)
        }
    }

    private fun shouldUpdatePlaybackManager(): Boolean {
        val podcast = this.podcast.value ?: return false
        val currentEpisode = playbackManager.upNextQueue.currentEpisode
        val podcastUuid = if (currentEpisode is Episode) currentEpisode.podcastUuid else null
        return podcastUuid == podcast.uuid
    }
}
