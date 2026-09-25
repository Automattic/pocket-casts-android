package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReferenceFingerprintTest {

    @Test
    fun `decode returns null for invalid JSON`() {
        val result = ReferenceFingerprint.decode("not json".toByteArray())
        assertNull(result)
    }

    @Test
    fun `decode returns null for empty data`() {
        val result = ReferenceFingerprint.decode(ByteArray(0))
        assertNull(result)
    }

    @Test
    fun `decode returns null for unsupported format`() {
        val json = """
            {
                "format": "unsupported-format",
                "total_duration": 100.0,
                "checkpoint_interval": 10,
                "checkpoint_duration": 8,
                "timestamp_quantum": 1,
                "checkpoints": []
            }
        """.trimIndent()
        val result = ReferenceFingerprint.decode(json.toByteArray())
        assertNull(result)
    }

    @Test
    fun `decode returns fingerprint for valid JSON`() {
        val json = """
            {
                "format": "fingerprint-compact-v2",
                "total_duration": 100.0,
                "checkpoint_interval": 10,
                "checkpoint_duration": 8,
                "timestamp_quantum": 1,
                "checkpoints": []
            }
        """.trimIndent()
        val result = ReferenceFingerprint.decode(json.toByteArray())
        assertNotNull(result)
        assertEquals("fingerprint-compact-v2", result!!.format)
        assertEquals(100.0, result.totalDuration, 0.001)
        assertEquals(10, result.checkpointInterval)
        assertEquals(8, result.checkpointDuration)
        assertEquals(1, result.timestampQuantum)
    }

    @Test
    fun `libraryCheckpoints returns empty for no checkpoints`() {
        val fp = ReferenceFingerprint(
            format = "fingerprint-compact-v2",
            totalDuration = 100.0,
            checkpointInterval = 10,
            checkpointDuration = 8,
            timestampQuantum = 1,
            checkpoints = emptyList(),
        )
        assertEquals(emptyList<ReferenceFingerprint.LibraryCheckpoint>(), fp.libraryCheckpoints())
    }

    @Test
    fun `libraryCheckpoints skips malformed entries`() {
        val fp = ReferenceFingerprint(
            format = "fingerprint-compact-v2",
            totalDuration = 100.0,
            checkpointInterval = 10,
            checkpointDuration = 8,
            timestampQuantum = 1,
            checkpoints = listOf(
                listOf<Any>(),
                listOf<Any>(1.0),
                listOf<Any>("not a number", "AQIDBA=="),
            ),
        )
        assertEquals(0, fp.libraryCheckpoints().size)
    }

    @Test
    fun `libraryCheckpoints accumulates timestamps correctly`() {
        val fp = ReferenceFingerprint(
            format = "fingerprint-compact-v2",
            totalDuration = 100.0,
            checkpointInterval = 10,
            checkpointDuration = 8,
            timestampQuantum = 2,
            checkpoints = listOf(
                listOf<Any>(5.0, "AQIDBA=="),
                listOf<Any>(3.0, "BQYHCA=="),
            ),
        )
        val checkpoints = fp.libraryCheckpoints()
        assertEquals(2, checkpoints.size)
        assertEquals(10.0f, checkpoints[0].timestampSeconds)
        assertEquals(16.0f, checkpoints[1].timestampSeconds)
    }

    @Test
    fun `checkpointDurationSeconds converts int to float`() {
        val fp = ReferenceFingerprint(
            format = "fingerprint-compact-v2",
            totalDuration = 100.0,
            checkpointInterval = 10,
            checkpointDuration = 8,
            timestampQuantum = 1,
            checkpoints = emptyList(),
        )
        assertEquals(8.0f, fp.checkpointDurationSeconds)
    }
}
