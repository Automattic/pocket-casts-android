package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.model.Media
import com.google.android.horologist.media.ui.state.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel
@Inject constructor(
    private val episodeManager: EpisodeManager,
    private val playerRepository: PlayerRepositoryImpl,
    private val playbackManager: PlaybackManager,
    savedStateHandle: SavedStateHandle,
) : PlayerViewModel(playerRepository) {
    private val playableUuid: String = savedStateHandle[NowPlayingScreen.argument] ?: ""

    init {
        viewModelScope.launch(Dispatchers.Default) {
            waitForConnection()
            val playable = episodeManager.findByUuid(playableUuid)
            playable?.let {
                withContext(Dispatchers.Main) {
                    play(playable)
                }
            }
        }
    }

    private fun play(playable: Playable) {
        val media = playable.toMedia()

        val isSelectedMediaPlaying = playerRepository.player.value?.isPlaying == true &&
            playerRepository.currentMedia.value?.id == media.id
        if (isSelectedMediaPlaying) return

        viewModelScope.launch {
            playbackManager.upNextQueue.playNow(playable) {
                launch(Dispatchers.Main) {
                    playerRepository.setMedia(media)
                    playerRepository.play()
                }
            }
        }
    }

    private suspend fun waitForConnection() {
        // setMedia is a noop before this point
        playerRepository.connected.filter { it }.first()
    }

    private fun Playable.toMedia(): Media {
        val downloadUrl = downloadUrl ?: ""
        return Media(
            id = uuid,
            uri = downloadUrl,
            title = title,
            artist = episodeDescription
        )
    }

    companion object {
        private const val UPDATE_DELAY = 1000L
    }
}
