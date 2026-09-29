package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewAudience
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import java.time.Instant

data class WhatsNewMessageFilter(
    val audience: WhatsNewAudience,
    val appVersion: ReleaseVersion?,
) {
    fun includes(message: WhatsNewMessage, now: Instant = Instant.now()): Boolean {
        return message.targeting.targets(audience) && isSupported(message) && isLive(message, now)
    }

    fun feedMessages(messages: List<WhatsNewMessage>, now: Instant = Instant.now()): List<WhatsNewMessage> {
        return messages.filter { message -> includes(message, now) }.sortedByDescending(WhatsNewMessage::publishedAt)
    }

    private fun isSupported(message: WhatsNewMessage): Boolean {
        val minimum = message.targeting.minimumAppVersion ?: return true
        val required = ReleaseVersion.fromString(minimum) ?: return false
        val version = appVersion?.copy(releaseCandidate = null) ?: return false
        return version >= required
    }

    private fun isLive(message: WhatsNewMessage, now: Instant): Boolean {
        if (message.publishedAt.isAfter(now)) return false
        val expiresAt = message.expiresAt ?: return true
        return expiresAt.isAfter(now)
    }

    companion object {
        fun of(tier: SubscriptionTier?, appVersion: ReleaseVersion?) = WhatsNewMessageFilter(
            audience = when (tier) {
                SubscriptionTier.Plus -> WhatsNewAudience.Plus
                SubscriptionTier.Patron -> WhatsNewAudience.Patron
                null -> WhatsNewAudience.Free
            },
            appVersion = appVersion,
        )
    }
}
