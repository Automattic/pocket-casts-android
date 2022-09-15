package au.com.shiftyjelly.pocketcasts.wear.ui.player

import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.ui.state.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel
@Inject constructor(
    playerRepository: PlayerRepositoryImpl,
) : PlayerViewModel(playerRepository)
