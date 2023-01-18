package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback

import android.content.Context
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.tryOrNull
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class InputControlPlayback @JvmOverloads constructor(
    @field:TaskerInputField("command") var command: String? = null,
    @field:TaskerInputField("skipToChapter") var chapterToSkipTo: String? = null,
    @field:TaskerInputField("skipToSeconds") var skipToSeconds: String? = null,
    @field:TaskerInputField("skipSeconds") var skipSeconds: String? = null,
    @field:TaskerInputField("playbackSpeed") var playbackSpeed: String? = null,
    @field:TaskerInputField("trimSilenceMode") var trimSilenceMode: String? = null,
    @field:TaskerInputField("volumeBoostEnabled") var volumeBoostEnabled: String? = null
) {

    val commandEnum get() = tryOrNull { command?.let { PlaybackCommand.valueOf(it) } }
    val trimSilenceModeEnum get() = tryOrNull { trimSilenceMode?.let { TrimMode.valueOf(it) } }

    enum class PlaybackCommand(@StringRes val descriptionResId: Int) {
        PlayNextInQueue(R.string.play_next_in_queue),
        SkipForward(R.string.skip_forward),
        SkipBack(R.string.skip_back),
        SkipToNextChapter(R.string.skip_to_next_chapter),
        SkipToPreviousChapter(R.string.skip_to_previous_chapter),
        SkipToChapter(R.string.skip_to_chapter),
        SkipToTime(R.string.skip_to_time),
        SetPlaybackSpeed(R.string.set_playback_speed),
        SetTrimSilenceMode(R.string.set_trim_silence_mode),
        SetVolumeBoost(R.string.set_volume_boost);

        fun getDescription(context: Context) = context.getString(descriptionResId)
    }
}
