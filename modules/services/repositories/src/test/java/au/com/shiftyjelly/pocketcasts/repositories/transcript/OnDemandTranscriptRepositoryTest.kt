package au.com.shiftyjelly.pocketcasts.repositories.transcript

import org.junit.Assert.assertEquals
import org.junit.Test

class OnDemandTranscriptRepositoryTest {
    @Test
    fun `approved client errors are not eligible`() {
        listOf(400, 401, 403, 404).forEach { statusCode ->
            assertEquals(
                OnDemandTranscriptRepository.Outcome.NotEligible,
                statusCode.toOnDemandTranscriptOutcome(),
            )
        }
    }

    @Test
    fun `rate limiting is throttled`() {
        assertEquals(
            OnDemandTranscriptRepository.Outcome.Throttled,
            429.toOnDemandTranscriptOutcome(),
        )
    }

    @Test
    fun `server and proxy errors are transient`() {
        listOf(500, 502, 503, 504).forEach { statusCode ->
            assertEquals(
                OnDemandTranscriptRepository.Outcome.TransientFailure,
                statusCode.toOnDemandTranscriptOutcome(),
            )
        }
    }

    @Test
    fun `unexpected status is unknown`() {
        assertEquals(
            OnDemandTranscriptRepository.Outcome.Unknown,
            418.toOnDemandTranscriptOutcome(),
        )
    }
}
