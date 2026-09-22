package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import java.time.Instant

data class WhatsNewCatalog(
    val schemaVersion: Int,
    val generatedAt: Instant?,
    val platform: String?,
    val locale: String?,
    val messages: List<WhatsNewMessage>,
)

data class WhatsNewMessage(
    val id: String,
    val type: WhatsNewMessageType,
    val publishedAt: Instant,
    val expiresAt: Instant?,
    val targeting: WhatsNewTargeting,
    val title: String,
    val content: WhatsNewContent,
)

enum class WhatsNewMessageType(val key: String) {
    NewFeature("new_feature"),
    Tip("tip"),
    Announcement("announcement"),
    KnownIssue("known_issue"),
    Research("research"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

sealed interface WhatsNewContent {
    data class Pages(val pages: List<WhatsNewPage>) : WhatsNewContent

    data class Research(val research: WhatsNewResearch) : WhatsNewContent
}

data class WhatsNewTargeting(
    val audiences: List<String>,
    val minimumAppVersion: String?,
    val hasUnreadableAudiences: Boolean = false,
) {
    fun targets(audience: WhatsNewAudience) = if (audiences.isEmpty() && !hasUnreadableAudiences) {
        true
    } else {
        audience.key in audiences
    }
}

enum class WhatsNewAudience(val key: String) {
    Free("free"),
    Plus("plus"),
    Patron("patron"),
}

data class WhatsNewPage(
    val image: WhatsNewImage?,
    val heading: String,
    val description: String,
    val action: WhatsNewAction?,
)

data class WhatsNewImage(
    val url: String,
    val width: Int?,
    val height: Int?,
    val alt: String?,
) {
    val aspectRatio = if (width != null && height != null && width > 0 && height > 0) {
        width.toFloat() / height.toFloat()
    } else {
        null
    }
}

data class WhatsNewAction(
    val event: String,
    val label: String,
)

data class WhatsNewResearch(
    val description: String?,
    val poll: WhatsNewPoll,
)

data class WhatsNewPoll(
    val pollId: String,
    val pollKey: String,
    val question: String,
    val options: List<Option>,
) {
    data class Option(
        val id: String,
        val pollOptionKey: String,
        val label: String,
    )
}
