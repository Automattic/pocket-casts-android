package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import androidx.lifecycle.MutableLiveData
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class SleepEpisodeTimer {
    companion object {
        const val MAX_EPISODES = 100
        private var sleepForEpisodes: Boolean = false
        private var canReduceCount = false
        var episodesUntilSleep: Int = 2
        var initEpisodeCount: Int = 0
        var untilSleepText: MutableLiveData<String>? = null
        var countdownSleepText: MutableLiveData<String>? = null

        fun setEpisodeOptionText(sleepOptionText: MutableLiveData<String>) {
            untilSleepText = sleepOptionText
        }

        fun setEpisodeCountdownText(sleepCountdownText: MutableLiveData<String>) {
            countdownSleepText = sleepCountdownText
        }

        fun initCustomIncrement(labelText: MutableLiveData<String>, context: Context) {
            initEpisodeCount = episodesUntilSleep
            labelText.postValue(
                if (initEpisodeCount == 1) context.resources.getString(LR.string.player_sleep_add_1_episode)
                else context.resources.getString(LR.string.player_sleep_add_n_episodes, initEpisodeCount)
            )
        }

        fun resetEpisodeCount(context: Context) {
            episodesUntilSleep = 2
            untilSleepText?.postValue(untilSleepMessage(context))
        }

        fun stop(pbManager: PlaybackManager, context: Context) {
            pbManager.updateSleepTimerStatus(false)
            resetEpisodeCount(context)
            sleepForEpisodes = false
        }

        fun activateTimer() {
            sleepForEpisodes = true
        }

        fun timerIsActive(): Boolean {
            return sleepForEpisodes
        }

        fun timerShouldStop(): Boolean {
            if (sleepForEpisodes && canReduceCount) {
                canReduceCount = false
                if (episodesUntilSleep > 1) {
                    episodesUntilSleep--
                } else {
                    return true
                }
            }
            return false
        }

        fun reduceCountIfActive() {
            canReduceCount = true
        }

        fun untilSleepMessage(context: Context): String {
            return when (episodesUntilSleep) {
                1 -> context.resources.getString(LR.string.player_sleep_end_of_episode)
                2 -> context.resources.getString(LR.string.player_sleep_end_of_next_episode)
                else -> context.resources.getString(LR.string.player_sleep_after_n_episodes, episodesUntilSleep)
            }
        }

        fun leftUntilSleepMessage(context: Context): String {
            return when (episodesUntilSleep) {
                1 -> context.resources.getString(LR.string.player_sleep_1_episode_left)
                2 -> context.resources.getString(LR.string.player_sleep_2_episodes_left)
                else -> context.resources.getString(LR.string.player_sleep_n_episodes_left, episodesUntilSleep)
            }
        }

        fun incEpisodeTimer(context: Context) {
            if (episodesUntilSleep < MAX_EPISODES) {
                episodesUntilSleep++
                untilSleepText?.postValue(untilSleepMessage(context))
            }
        }

        fun decEpisodeTimer(context: Context) {
            if (episodesUntilSleep > 1) {
                episodesUntilSleep--
                untilSleepText?.postValue(untilSleepMessage(context))
            }
        }

        fun extendTimer(episodes: Int?, context: Context) {
            val eps = episodes ?: initEpisodeCount
            episodesUntilSleep += eps
            if (episodesUntilSleep > MAX_EPISODES) {
                episodesUntilSleep = MAX_EPISODES
            }
            countdownSleepText?.postValue(leftUntilSleepMessage(context))
        }
    }
}
