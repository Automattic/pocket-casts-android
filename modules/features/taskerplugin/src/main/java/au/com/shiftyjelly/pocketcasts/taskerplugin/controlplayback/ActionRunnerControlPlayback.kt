package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playbackManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.nullIfEmpty
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

private const val ERROR_NO_COMMAND_PROVIDED = 1
private const val ERROR_INVALIUD_COMMAND_PROVIDED = 2
private const val ERROR_INVALID_CHAPTER_TO_SKIP_TO_PROVIDED = 3
private const val ERROR_INVALID_TIME_TO_SKIP_TO_PROVIDED = 4
private const val ERROR_INVALID_TIME_TO_SKIP_PROVIDED = 5

class ActionRunnerControlPlayback : TaskerPluginRunnerActionNoOutput<InputControlPlayback>() {

    override fun run(context: Context, input: TaskerInput<InputControlPlayback>): TaskerPluginResult<Unit> {
        val command = input.regular.command.nullIfEmpty ?: return TaskerPluginResultError(ERROR_NO_COMMAND_PROVIDED, context.getString(au.com.shiftyjelly.pocketcasts.localization.R.string.must_provide_command_name))

        val playbackManager = context.playbackManager
        val commandEnum = input.regular.commandEnum ?: return TaskerPluginResultError(ERROR_INVALIUD_COMMAND_PROVIDED, context.getString(au.com.shiftyjelly.pocketcasts.localization.R.string.command_x_not_valid, command))

        when (commandEnum) {
            InputControlPlayback.PlaybackCommand.SkipToNextChapter -> playbackManager.skipToNextChapter()
            InputControlPlayback.PlaybackCommand.SkipToChapter -> {
                val chapterToSkipTo = input.regular.chapterToSkipTo
                playbackManager.skipToChapter(chapterToSkipTo?.toIntOrNull() ?: return TaskerPluginResultError(ERROR_INVALID_CHAPTER_TO_SKIP_TO_PROVIDED, context.getString(R.string.chapter_to_skip_to_not_valid, input.regular.chapterToSkipTo)))
            }
            InputControlPlayback.PlaybackCommand.SkipToPrevious -> playbackManager.skipToPreviousChapter()
            InputControlPlayback.PlaybackCommand.SkipToTime -> playbackManager.seekToTimeMs(input.regular.timeToSkipToSeconds?.toIntOrNull()?.let { it * 1000 } ?: return TaskerPluginResultError(ERROR_INVALID_TIME_TO_SKIP_TO_PROVIDED, context.getString(R.string.time_to_skip_to_not_valid, input.regular.timeToSkipToSeconds)))
            InputControlPlayback.PlaybackCommand.SkipForward, InputControlPlayback.PlaybackCommand.SkipBack -> {
                val jumpAmountSeconds = input.regular.timeToSkipSeconds?.toIntOrNull() ?: return TaskerPluginResultError(ERROR_INVALID_TIME_TO_SKIP_PROVIDED, context.getString(R.string.time_to_skip_to_not_valid, input.regular.timeToSkipToSeconds))
                if (commandEnum == InputControlPlayback.PlaybackCommand.SkipBack) {
                    playbackManager.skipBackward(jumpAmountSeconds = jumpAmountSeconds, playbackSource = PlaybackManager.PlaybackSource.TASKER)
                }else{
                    playbackManager.skipForward(jumpAmountSeconds = jumpAmountSeconds, playbackSource = PlaybackManager.PlaybackSource.TASKER)
                }
            }
            InputControlPlayback.PlaybackCommand.PlayNextInQueue -> playbackManager.playNextInQueue(PlaybackManager.PlaybackSource.TASKER)
        }

        return TaskerPluginResultSucess()
    }
}
