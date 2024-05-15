package au.com.shiftyjelly.pocketcasts.utils

import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class SharingUrlTimestampParserTest {

    private val parser = SharingUrlTimestampParser()

    @Test
    fun parseTimestampInterval() {
        assertEquals(Pair(28.seconds, 60.seconds), parser.parseTimestamp("28,60"))
        assertEquals(Pair(0.seconds, 20.seconds), parser.parseTimestamp(",20"))
        assertEquals(Pair(10.seconds, 0.seconds), parser.parseTimestamp("10"))
    }
}
