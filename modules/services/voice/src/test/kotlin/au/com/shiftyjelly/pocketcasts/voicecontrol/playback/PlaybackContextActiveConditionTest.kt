@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PlaybackContextActiveConditionTest {
    @Test
    fun `paused playback with current episode has active playback context`() = runTest {
        val playbackState = MutableStateFlow(
            PlaybackState(
                state = PlaybackState.State.PAUSED,
                episodeUuid = "episode-id",
            ),
        )
        val playbackManager: PlaybackManager = mock {
            on { playbackStateFlow } doReturn playbackState as Flow<PlaybackState>
        }

        val monitor = PlaybackContextMonitor(playbackManager, backgroundScope)
        runCurrent()

        assertEquals(PlaybackContext.Active(currentEpisodeUuid = "episode-id", isPlaying = false), monitor.context.value)
    }

    @Test
    fun `current episode allows listening even when paused`() {
        val condition = PlaybackContextActiveCondition(MutableStateFlow(PlaybackContext.Active(currentEpisodeUuid = "episode-id", isPlaying = false)))

        assertEquals(VoiceControlRuleState.Allowed, condition.evaluate())
    }

    @Test
    fun `missing episode blocks listening`() {
        val condition = PlaybackContextActiveCondition(MutableStateFlow(PlaybackContext.Inactive))

        assertEquals(VoiceControlRuleState.Blocked("playback_context_inactive"), condition.evaluate())
    }
}
