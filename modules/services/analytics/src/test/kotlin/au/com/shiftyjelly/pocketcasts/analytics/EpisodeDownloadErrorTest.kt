package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.CONTENT_TYPE_KEY
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.EPISODE_UUID_KEY
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.EXPECTED_CONTENT_LENGTH_KEY
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.HTTP_STATUS_CODE_KEY
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.IS_CELLULAR_KEY
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.IS_PROXY_KEY
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.PODCAST_UUID_KEY
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.REASON_KEY
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.RESPONSE_BODY_BYTES_RECEIVED_KEY
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.TASK_DURATION_KEY
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError.Companion.TLS_CIPHER_SUITE_KEY
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeDownloadErrorTest {
    @Test
    fun `convert to properties`() {
        val error = EpisodeDownloadError(
            reason = EpisodeDownloadError.Reason.Unknown,
            episodeUuid = "episode uuid",
            podcastUuid = "podcast uuid",
            taskDuration = 10,
            httpStatusCode = 20,
            contentType = "content type",
            expectedContentLength = 30,
            responseBodyBytesReceived = 40,
            tlsCipherSuite = "tls cipher suite",
            isCellular = true,
            isProxy = false,
        )

        val expected = mapOf(
            REASON_KEY to EpisodeDownloadError.Reason.Unknown.analyticsValue,
            EPISODE_UUID_KEY to "episode uuid",
            PODCAST_UUID_KEY to "podcast uuid",
            TASK_DURATION_KEY to 10L,
            HTTP_STATUS_CODE_KEY to 20,
            CONTENT_TYPE_KEY to "content type",
            EXPECTED_CONTENT_LENGTH_KEY to 30L,
            RESPONSE_BODY_BYTES_RECEIVED_KEY to 40L,
            TLS_CIPHER_SUITE_KEY to "tls cipher suite",
            IS_CELLULAR_KEY to true,
            IS_PROXY_KEY to false,
        )

        assertEquals(expected, error.toProperties())
    }

    @Test
    fun `create from properties`() {
        val properties = mapOf(
            REASON_KEY to EpisodeDownloadError.Reason.Unknown.analyticsValue,
            EPISODE_UUID_KEY to "episode uuid",
            PODCAST_UUID_KEY to "podcast uuid",
            TASK_DURATION_KEY to 10L,
            HTTP_STATUS_CODE_KEY to 20,
            CONTENT_TYPE_KEY to "content type",
            EXPECTED_CONTENT_LENGTH_KEY to 30L,
            RESPONSE_BODY_BYTES_RECEIVED_KEY to 40L,
            TLS_CIPHER_SUITE_KEY to "tls cipher suite",
            IS_CELLULAR_KEY to true,
            IS_PROXY_KEY to false,
        )

        val expected = EpisodeDownloadError(
            reason = EpisodeDownloadError.Reason.Unknown,
            episodeUuid = "episode uuid",
            podcastUuid = "podcast uuid",
            taskDuration = 10,
            httpStatusCode = 20,
            contentType = "content type",
            expectedContentLength = 30,
            responseBodyBytesReceived = 40,
            tlsCipherSuite = "tls cipher suite",
            isCellular = true,
            isProxy = false,
        )

        assertEquals(expected, EpisodeDownloadError.fromProperties(properties))
    }

    @Test
    fun `handle all reasons in properties`() {
        val errors = EpisodeDownloadError.Reason.entries.map { EpisodeDownloadError(reason = it).toProperties() }

        val reasons = errors.map { EpisodeDownloadError.fromProperties(it).reason }

        assertEquals(EpisodeDownloadError.Reason.entries, reasons)
    }
}
