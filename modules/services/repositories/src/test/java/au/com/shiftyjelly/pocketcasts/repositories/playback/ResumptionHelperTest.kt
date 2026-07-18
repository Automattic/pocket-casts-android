package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.hours
import au.com.shiftyjelly.pocketcasts.utils.minutes
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ResumptionHelperTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.INTERRUPTION_REWIND, true)
    }

    private fun episode(playedUpToMs: Int = PLAYED_UP_TO_MS, uuid: String = EPISODE_UUID) = PodcastEpisode(
        uuid = uuid,
        podcastUuid = "podcast-uuid",
        publishedDate = Date(),
    ).apply {
        this.playedUpToMs = playedUpToMs
    }

    private fun settingsMock(
        lastPausedUuid: String? = EPISODE_UUID,
        lastPausedAtMs: Int? = PLAYED_UP_TO_MS,
        wasInterruption: Boolean = false,
        lastPauseTime: Date? = Date(),
        isIntelligentResumptionEnabled: Boolean = true,
        rewindSeconds: Int = 5,
    ): Settings {
        val intelligentResumptionSetting = mock<UserSetting<Boolean>> {
            on { value } doReturn isIntelligentResumptionEnabled
        }
        val interruptionRewindSetting = mock<UserSetting<Int>> {
            on { value } doReturn rewindSeconds
        }
        return mock {
            on { getLastPausedUUID() } doReturn lastPausedUuid
            on { getLastPausedAt() } doReturn lastPausedAtMs
            on { getLastPauseWasInterruption() } doReturn wasInterruption
            on { getLastPauseTime() } doReturn lastPauseTime
            on { intelligentPlaybackResumption } doReturn intelligentResumptionSetting
            on { interruptionRewindSeconds } doReturn interruptionRewindSetting
        }
    }

    @Test
    fun `interruption rewind applies when the last pause was an interruption`() {
        val settings = settingsMock(wasInterruption = true, rewindSeconds = 30)
        val helper = ResumptionHelper(settings)

        assertEquals(PLAYED_UP_TO_MS - 30_000, helper.adjustedStartTimeMsFor(episode()))
    }

    @Test
    fun `interruption rewind does not apply to a regular pause`() {
        val settings = settingsMock(wasInterruption = false, rewindSeconds = 30)
        val helper = ResumptionHelper(settings)

        assertEquals(PLAYED_UP_TO_MS, helper.adjustedStartTimeMsFor(episode()))
    }

    @Test
    fun `interruption rewind does not apply when turned off`() {
        val settings = settingsMock(wasInterruption = true, rewindSeconds = 0)
        val helper = ResumptionHelper(settings)

        assertEquals(PLAYED_UP_TO_MS, helper.adjustedStartTimeMsFor(episode()))
    }

    @Test
    fun `interruption rewind does not apply when the feature flag is disabled`() {
        FeatureFlag.setEnabled(Feature.INTERRUPTION_REWIND, false)
        val settings = settingsMock(wasInterruption = true, rewindSeconds = 30)
        val helper = ResumptionHelper(settings)

        assertEquals(PLAYED_UP_TO_MS, helper.adjustedStartTimeMsFor(episode()))
    }

    @Test
    fun `interruption rewind applies when intelligent playback resumption is off`() {
        val settings = settingsMock(
            wasInterruption = true,
            rewindSeconds = 30,
            isIntelligentResumptionEnabled = false,
        )
        val helper = ResumptionHelper(settings)

        assertEquals(PLAYED_UP_TO_MS - 30_000, helper.adjustedStartTimeMsFor(episode()))
    }

    @Test
    fun `interruption rewind does not rewind past the episode start`() {
        val settings = settingsMock(
            wasInterruption = true,
            rewindSeconds = 30,
            lastPausedAtMs = 3_000,
        )
        val helper = ResumptionHelper(settings)

        assertEquals(0, helper.adjustedStartTimeMsFor(episode(playedUpToMs = 3_000)))
    }

    @Test
    fun `no adjustment for a different episode than the last paused one`() {
        val settings = settingsMock(wasInterruption = true, rewindSeconds = 30)
        val helper = ResumptionHelper(settings)

        val other = episode(uuid = "another-episode-uuid")

        assertEquals(PLAYED_UP_TO_MS, helper.adjustedStartTimeMsFor(other))
    }

    @Test
    fun `no adjustment when the playback position moved since the last pause`() {
        val settings = settingsMock(wasInterruption = true, rewindSeconds = 30, lastPausedAtMs = 1_000)
        val helper = ResumptionHelper(settings)

        assertEquals(PLAYED_UP_TO_MS, helper.adjustedStartTimeMsFor(episode()))
    }

    @Test
    fun `longer pause length rewind wins over a shorter interruption rewind`() {
        val settings = settingsMock(
            wasInterruption = true,
            rewindSeconds = 5,
            lastPauseTime = Date(Date().time - 25.hours()),
        )
        val helper = ResumptionHelper(settings)

        // paused for more than 24 hours, the 30 second pause length rewind beats the 5 second interruption rewind
        assertEquals(PLAYED_UP_TO_MS - 30_000, helper.adjustedStartTimeMsFor(episode()))
    }

    @Test
    fun `longer interruption rewind wins over a shorter pause length rewind`() {
        val settings = settingsMock(
            wasInterruption = true,
            rewindSeconds = 60,
            lastPauseTime = Date(Date().time - 6.minutes()),
        )
        val helper = ResumptionHelper(settings)

        // paused for more than 5 minutes, the 60 second interruption rewind beats the 10 second pause length rewind
        assertEquals(PLAYED_UP_TO_MS - 60_000, helper.adjustedStartTimeMsFor(episode()))
    }

    @Test
    fun `pause length rewind still applies to regular pauses`() {
        val settings = settingsMock(
            wasInterruption = false,
            lastPauseTime = Date(Date().time - 6.minutes()),
        )
        val helper = ResumptionHelper(settings)

        assertEquals(PLAYED_UP_TO_MS - 10_000, helper.adjustedStartTimeMsFor(episode()))
    }

    @Test
    fun `no rewind for a short regular pause`() {
        val settings = settingsMock(wasInterruption = false)
        val helper = ResumptionHelper(settings)

        assertEquals(PLAYED_UP_TO_MS, helper.adjustedStartTimeMsFor(episode()))
    }

    @Test
    fun `paused persists whether the pause came from an interruption`() {
        val settings = settingsMock()
        val helper = ResumptionHelper(settings)

        helper.paused(episode(), atPlayedUpToMs = PLAYED_UP_TO_MS, dueToInterruption = true)

        verify(settings).setLastPauseWasInterruption(true)
    }

    @Test
    fun `paused defaults to not being an interruption`() {
        val settings = settingsMock()
        val helper = ResumptionHelper(settings)

        helper.paused(episode(), atPlayedUpToMs = PLAYED_UP_TO_MS)

        verify(settings).setLastPauseWasInterruption(false)
    }

    companion object {
        private const val EPISODE_UUID = "episode-uuid"
        private const val PLAYED_UP_TO_MS = 600_000
    }
}
