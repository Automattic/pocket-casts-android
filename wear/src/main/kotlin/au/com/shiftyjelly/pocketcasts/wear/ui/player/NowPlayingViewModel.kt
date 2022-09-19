package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.lifecycle.viewModelScope
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.ui.state.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel
@Inject constructor(
    playerRepository: PlayerRepositoryImpl,
) : PlayerViewModel(playerRepository) {
    init {
        viewModelScope.launch {
            // update the track position while app is in foreground
            while (isActive) {
                delay(UPDATE_DELAY)
                playerRepository.updatePosition()
            }
        }
    }

    companion object {
        private const val UPDATE_DELAY = 1000L
    }
}
