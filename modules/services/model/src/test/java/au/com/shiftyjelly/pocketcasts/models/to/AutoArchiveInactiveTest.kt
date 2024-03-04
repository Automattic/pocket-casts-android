package au.com.shiftyjelly.pocketcasts.models.to

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoArchiveInactiveTest {
    @Test
    fun `use Never as a default value`() {
        assertEquals(AutoArchiveInactive.Default, AutoArchiveInactive.Never)
    }

    @Test
    fun `use correct server ID`() {
        val expected = listOf(0, 1, 2, 3, 4, 5, 6)

        val serverIds = AutoArchiveInactive.All.map { it.serverId }

        assertEquals(expected, serverIds)
    }

    @Test
    fun `create from server ID`() {
        val values = listOf(0, 1, 2, 3, 4, 5, 6).map { AutoArchiveInactive.fromServerId(it) }

        assertEquals(AutoArchiveInactive.All, values)
    }

    @Test
    fun `create null from unknown server ID`() {
        val value = AutoArchiveInactive.fromServerId(Int.MIN_VALUE)

        assertNull(value)
    }

    @Test
    fun `use correct time durations`() {
        val expected = mapOf(
            AutoArchiveInactive.Never to (-1).seconds,
            AutoArchiveInactive.Hours24 to 24.hours,
            AutoArchiveInactive.Days2 to 2.days,
            AutoArchiveInactive.Weeks1 to 7.days,
            AutoArchiveInactive.Weeks2 to 14.days,
            AutoArchiveInactive.Days30 to 30.days,
            AutoArchiveInactive.Days90 to 90.days,
        )

        val durations = AutoArchiveInactive.All.associateWith { it.timeSeconds.toDuration(DurationUnit.SECONDS) }

        assertEquals(expected, durations)
    }
}
