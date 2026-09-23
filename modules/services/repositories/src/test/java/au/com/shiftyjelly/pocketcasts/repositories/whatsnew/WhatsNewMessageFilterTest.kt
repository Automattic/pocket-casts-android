package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewAudience
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewContent
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessageType
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewPage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewTargeting
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewMessageFilterTest {
    private val now = Instant.parse("2026-09-22T00:00:00Z")
    private val filter = WhatsNewMessageFilter(WhatsNewAudience.Plus, ReleaseVersion(8, 22))

    @Test
    fun `a message aimed at the user's tier is shown`() {
        assertTrue(filter.includes(message(audiences = listOf("plus")), now))
    }

    @Test
    fun `a message aimed at another tier is hidden`() {
        assertFalse(filter.includes(message(audiences = listOf("patron")), now))
    }

    @Test
    fun `a message with no audiences is shown to everyone`() {
        assertTrue(filter.includes(message(audiences = emptyList()), now))
    }

    @Test
    fun `a message aimed only at a tier this version does not know is hidden`() {
        assertFalse(filter.includes(message(audiences = listOf("founder")), now))
    }

    @Test
    fun `a build newer than the minimum is shown the message`() {
        assertTrue(filter.includes(message(minimumAppVersion = "8.21"), now))
    }

    @Test
    fun `the build the minimum asks for is shown the message`() {
        assertTrue(filter.includes(message(minimumAppVersion = "8.22"), now))
    }

    @Test
    fun `an older build is not shown the message`() {
        assertFalse(filter.includes(message(minimumAppVersion = "8.23"), now))
    }

    @Test
    fun `version components are compared as numbers`() {
        assertTrue(filter.includes(message(minimumAppVersion = "8.9"), now))
    }

    @Test
    fun `a trailing zero does not make the minimum newer`() {
        assertTrue(filter.includes(message(minimumAppVersion = "8.22.0"), now))
    }

    @Test
    fun `a release candidate counts as the release it is a candidate for`() {
        val filter = WhatsNewMessageFilter(WhatsNewAudience.Plus, ReleaseVersion(8, 21, releaseCandidate = 6))

        assertTrue(filter.includes(message(minimumAppVersion = "8.21"), now))
        assertFalse(filter.includes(message(minimumAppVersion = "8.22"), now))
    }

    @Test
    fun `a message gated on a version is hidden when the app version is unknown`() {
        val filter = WhatsNewMessageFilter(WhatsNewAudience.Plus, appVersion = null)

        assertFalse(filter.includes(message(minimumAppVersion = "8.22"), now))
    }

    @Test
    fun `a message gated on something that is not a version is hidden`() {
        assertFalse(filter.includes(message(minimumAppVersion = "soon"), now))
    }

    @Test
    fun `a message that is not published yet is hidden`() {
        assertFalse(filter.includes(message(publishedAt = now.plusSeconds(60)), now))
    }

    @Test
    fun `an expired message is hidden`() {
        assertFalse(filter.includes(message(expiresAt = now.minusSeconds(60)), now))
    }

    @Test
    fun `a message that has not expired yet is shown`() {
        assertTrue(filter.includes(message(expiresAt = now.plusSeconds(60)), now))
    }

    @Test
    fun `a message with no expiry never expires`() {
        assertTrue(filter.includes(message(expiresAt = null), now))
    }

    @Test
    fun `the feed lists what the user can see, most recently published first`() {
        val messages = listOf(
            message(id = "old", publishedAt = now.minusSeconds(600)),
            message(id = "hidden", audiences = listOf("patron")),
            message(id = "new", publishedAt = now.minusSeconds(60)),
        )

        val feed = filter.feedMessages(messages, now)

        assertEquals(listOf("new", "old"), feed.map { it.id })
    }

    @Test
    fun `a free user is matched against the free audience`() {
        val filter = WhatsNewMessageFilter.of(tier = null, appVersion = ReleaseVersion(8, 22))

        assertEquals(WhatsNewAudience.Free, filter.audience)
    }

    @Test
    fun `a subscriber is matched against the audience for their tier`() {
        assertEquals(WhatsNewAudience.Plus, WhatsNewMessageFilter.of(SubscriptionTier.Plus, null).audience)
        assertEquals(WhatsNewAudience.Patron, WhatsNewMessageFilter.of(SubscriptionTier.Patron, null).audience)
    }

    private fun message(
        id: String = "m1",
        audiences: List<String> = emptyList(),
        minimumAppVersion: String? = null,
        publishedAt: Instant = now.minusSeconds(60),
        expiresAt: Instant? = null,
    ) = WhatsNewMessage(
        id = id,
        type = WhatsNewMessageType.Tip,
        publishedAt = publishedAt,
        expiresAt = expiresAt,
        targeting = WhatsNewTargeting(audiences = audiences, minimumAppVersion = minimumAppVersion),
        title = "Sort your Up Next",
        content = WhatsNewContent.Pages(listOf(WhatsNewPage(image = null, heading = "h", description = "d", action = null))),
    )
}
