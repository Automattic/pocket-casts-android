package au.com.shiftyjelly.pocketcasts.models.to

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoArchiveAfterPlayingTest {
    @Test
    fun `use correct server ID`() {
        val expected = listOf(0, 1, 2, 3, 4)

        val serverIds = AutoArchiveAfterPlaying.All.map { it.serverId }

        assertEquals(expected, serverIds)
    }

    @Test
    fun `create from server ID`() {
        val values = listOf(0, 1, 2, 3, 4).map { AutoArchiveAfterPlaying.fromServerId(it) }

        assertEquals(AutoArchiveAfterPlaying.All, values)
    }

    @Test
    fun `create null from unknown server ID`() {
        val value = AutoArchiveAfterPlaying.fromServerId(Int.MIN_VALUE)

        assertNull(value)
    }

    @Test
    fun `use correct time durations`() {
        val expected = mapOf(
            AutoArchiveAfterPlaying.Never to (-1).seconds,
            AutoArchiveAfterPlaying.AfterPlaying to 0.seconds,
            AutoArchiveAfterPlaying.Hours24 to 1.days,
            AutoArchiveAfterPlaying.Days2 to 2.days,
            AutoArchiveAfterPlaying.Weeks1 to 7.days,
        )

        val durations = AutoArchiveAfterPlaying.All.associateWith { it.timeSeconds.toDuration(DurationUnit.SECONDS) }

        assertEquals(expected, durations)
    }
}
