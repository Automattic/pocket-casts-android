package au.com.shiftyjelly.pocketcasts.wear.ui.player

import android.os.Vibrator
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.audio.ui.ExperimentalHorologistAudioUiApi
import com.google.android.horologist.audio.ui.VolumeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalHorologistAudioUiApi::class)
@HiltViewModel
class PCVolumeViewModel @Inject constructor(
    systemAudioRepository: SystemAudioRepository,
    vibrator: Vibrator
) : VolumeViewModel(
    volumeRepository = systemAudioRepository,
    audioOutputRepository = systemAudioRepository,
    vibrator = vibrator
)
