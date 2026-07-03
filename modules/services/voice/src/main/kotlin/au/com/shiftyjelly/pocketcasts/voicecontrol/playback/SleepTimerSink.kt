package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import au.com.shiftyjelly.pocketcasts.repositories.playback.SleepTimer
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconId
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class SleepTimerSink @Inject constructor(
    private val sleepTimer: SleepTimer,
) : VoiceSleepSink {
    override fun set(minutes: Int): VoiceResponse {
        sleepTimer.sleepAfter(minutes.minutes)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun endOfEpisode(): VoiceResponse {
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterEpisodes = 1)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun endOfChapter(): VoiceResponse {
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterChapters = 1)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun addTime(minutes: Int): VoiceResponse {
        sleepTimer.addExtraTime(minutes.minutes)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun cancel(): VoiceResponse {
        sleepTimer.cancelTimer()
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun query(): VoiceResponse.Spoken {
        val state = sleepTimer.state
        return when {
            !state.isSleepTimerRunning -> VoiceResponse.Spoken("No sleep timer active")

            state.isSleepEndOfEpisodeRunning -> VoiceResponse.Spoken("${state.numberOfEpisodesLeft} episodes remaining")

            state.isSleepEndOfChapterRunning -> VoiceResponse.Spoken("${state.numberOfChaptersLeft} chapters remaining")

            else -> {
                val secs = state.timeLeft.inWholeSeconds
                val mins = secs / 60
                VoiceResponse.Spoken("$mins minutes remaining")
            }
        }
    }
}
