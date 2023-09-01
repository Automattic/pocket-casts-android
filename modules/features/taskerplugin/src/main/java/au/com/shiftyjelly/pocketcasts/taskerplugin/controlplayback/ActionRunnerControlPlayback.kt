package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playbackManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.podcastManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.settings
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.nullIfEmpty
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlin.math.round

private const val ERROR_NO_COMMAND_PROVIDED = 1
private const val ERROR_INVALIUD_COMMAND_PROVIDED = 2
private const val ERROR_INVALID_CHAPTER_TO_SKIP_TO_PROVIDED = 3
private const val ERROR_INVALID_TIME_TO_SKIP_TO_PROVIDED = 4
private const val ERROR_INVALID_TIME_TO_SKIP_PROVIDED = 5
private const val ERROR_INVALID_PLAYBACK_SPEED_PROVIDED = 6
private const val ERROR_INVALID_TRIM_SILENCE_MODE_PROVIDED = 7
private const val ERROR_INVALID_VOLUME_BOOST_VALUE_PROVIDED = 8

class ActionRunnerControlPlayback : TaskerPluginRunnerActionNoOutput<InputControlPlayback>() {

    override fun run(context: Context, input: TaskerInput<InputControlPlayback>): TaskerPluginResult<Unit> {
        val command = input.regular.command.nullIfEmpty ?: return TaskerPluginResultError(ERROR_NO_COMMAND_PROVIDED, context.getString(R.string.must_provide_command_name))

        val playbackManager = context.playbackManager
        val commandEnum = input.regular.commandEnum ?: return TaskerPluginResultError(ERROR_INVALIUD_COMMAND_PROVIDED, context.getString(R.string.command_x_not_valid, command))

        when (commandEnum) {
            InputControlPlayback.PlaybackCommand.SkipToNextChapter -> playbackManager.skipToNextChapter()
            InputControlPlayback.PlaybackCommand.SkipToChapter -> {
                val chapterToSkipTo = input.regular.chapterToSkipTo?.toIntOrNull() ?: return TaskerPluginResultError(ERROR_INVALID_CHAPTER_TO_SKIP_TO_PROVIDED, context.getString(R.string.chapter_to_skip_to_not_valid, input.regular.chapterToSkipTo))

                playbackManager.skipToChapter(chapterToSkipTo)
            }
            InputControlPlayback.PlaybackCommand.SkipToPreviousChapter -> playbackManager.skipToPreviousChapter()
            InputControlPlayback.PlaybackCommand.SkipToTime -> playbackManager.seekToTimeMs(input.regular.skipToSeconds?.toIntOrNull()?.let { it * 1000 } ?: return TaskerPluginResultError(ERROR_INVALID_TIME_TO_SKIP_TO_PROVIDED, context.getString(R.string.time_to_skip_to_not_valid, input.regular.skipToSeconds)))
            InputControlPlayback.PlaybackCommand.SkipForward, InputControlPlayback.PlaybackCommand.SkipBack -> {
                val jumpAmountSeconds = input.regular.skipSeconds?.toIntOrNull() ?: return TaskerPluginResultError(ERROR_INVALID_TIME_TO_SKIP_PROVIDED, context.getString(R.string.time_to_skip_not_valid, input.regular.skipSeconds))

                if (commandEnum == InputControlPlayback.PlaybackCommand.SkipBack) {
                    playbackManager.skipBackward(jumpAmountSeconds = jumpAmountSeconds, sourceView = SourceView.TASKER)
                } else {
                    playbackManager.skipForward(jumpAmountSeconds = jumpAmountSeconds, sourceView = SourceView.TASKER)
                }
            }
            InputControlPlayback.PlaybackCommand.PlayNextInQueue -> playbackManager.playNextInQueue(
                SourceView.TASKER
            )
            InputControlPlayback.PlaybackCommand.SetPlaybackSpeed -> {
                val speed = input.regular.playbackSpeed?.toDoubleOrNull() ?: return TaskerPluginResultError(ERROR_INVALID_PLAYBACK_SPEED_PROVIDED, context.getString(R.string.playback_speed_not_valid, input.regular.playbackSpeed))

                val clippedToRangeSpeed = speed.coerceIn(0.5, 3.0)
                val roundedSpeed = round(clippedToRangeSpeed * 10.0) / 10.0
                context.updateEffects { playbackSpeed = roundedSpeed }
            }
            InputControlPlayback.PlaybackCommand.SetTrimSilenceMode -> {
                val trimSilenceMode = input.regular.trimSilenceModeEnum ?: return TaskerPluginResultError(ERROR_INVALID_TRIM_SILENCE_MODE_PROVIDED, context.getString(R.string.trim_silence_mode_not_valid, input.regular.trimSilenceMode))

                context.updateEffects { trimMode = trimSilenceMode }
            }
            InputControlPlayback.PlaybackCommand.SetVolumeBoost -> {
                val volumeBoostEnabled = input.regular.volumeBoostEnabled?.toBooleanStrictOrNull() ?: return TaskerPluginResultError(ERROR_INVALID_VOLUME_BOOST_VALUE_PROVIDED, context.getString(R.string.volume_boost_enabled_not_valid, input.regular.volumeBoostEnabled))

                context.updateEffects { isVolumeBoosted = volumeBoostEnabled }
            }
        }

        return TaskerPluginResultSucess()
    }

    private fun Context.updateEffects(updater: PlaybackEffects.() -> Unit) {
        val currentPodcast = playbackManager.getCurrentPodcast() ?: return

        val overrideGlobalEffects = currentPodcast.overrideGlobalEffects
        val playbackEffects: PlaybackEffects = if (overrideGlobalEffects) {
            currentPodcast.playbackEffects
        } else {
            settings.globalPlaybackEffects.value
        }
        playbackEffects.updater()
        if (overrideGlobalEffects) {
            podcastManager.updateEffects(currentPodcast, playbackEffects)
        } else {
            settings.globalPlaybackEffects.set(playbackEffects)
        }
        playbackManager.updatePlayerEffects(playbackEffects)
    }
}
