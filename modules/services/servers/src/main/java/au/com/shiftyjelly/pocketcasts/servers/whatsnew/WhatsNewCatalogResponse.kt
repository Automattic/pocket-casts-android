package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import au.com.shiftyjelly.pocketcasts.servers.adapters.LossyList
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class WhatsNewCatalogResponse(
    val schemaVersion: Int = 0,
    val generatedAt: Date? = null,
    val platform: String? = null,
    val locale: String? = null,
    val messages: LossyList<WhatsNewMessageResponse> = LossyList(),
) {
    fun toCatalog() = WhatsNewCatalog(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt?.toInstant(),
        platform = platform,
        locale = locale,
        messages = messages.values.mapNotNull(WhatsNewMessageResponse::toMessage),
    )
}

@JsonClass(generateAdapter = true)
data class WhatsNewMessageResponse(
    val id: String? = null,
    val type: String? = null,
    val publishedAt: Date? = null,
    val expiresAt: Date? = null,
    val targeting: WhatsNewTargetingResponse? = null,
    val title: String? = null,
    val pages: List<WhatsNewPageResponse>? = null,
    val description: String? = null,
    val poll: WhatsNewPollResponse? = null,
) {
    fun toMessage(): WhatsNewMessage? {
        val id = id.nonBlank() ?: return null
        val type = WhatsNewMessageType.fromKey(type) ?: return null
        val publishedAt = publishedAt ?: return null
        val targeting = targeting ?: return null
        val title = title.nonBlank() ?: return null
        val content = toContent(type) ?: return null

        return WhatsNewMessage(
            id = id,
            type = type,
            publishedAt = publishedAt.toInstant(),
            expiresAt = expiresAt?.toInstant(),
            targeting = targeting.toTargeting(),
            title = title,
            content = content,
        )
    }

    private fun toContent(type: WhatsNewMessageType) = when (type) {
        WhatsNewMessageType.Research -> poll?.toPoll()?.let { poll ->
            WhatsNewContent.Research(WhatsNewResearch(description = description.nonBlank(), poll = poll))
        }

        else -> pages?.toPages()?.let(WhatsNewContent::Pages)
    }

    private fun List<WhatsNewPageResponse>.toPages(): List<WhatsNewPage>? {
        val pages = map { page -> page.toPage() ?: return null }
        return pages.ifEmpty { null }
    }
}

@JsonClass(generateAdapter = true)
data class WhatsNewTargetingResponse(
    val audiences: LossyList<String> = LossyList(),
    val minimumAppVersion: String? = null,
) {
    fun toTargeting() = WhatsNewTargeting(
        audiences = audiences.values,
        minimumAppVersion = minimumAppVersion.nonBlank(),
        hasUnreadableAudiences = audiences.droppedCount > 0,
    )
}

@JsonClass(generateAdapter = true)
data class WhatsNewPageResponse(
    val image: WhatsNewImageResponse? = null,
    val heading: String? = null,
    val description: String? = null,
    val action: WhatsNewActionResponse? = null,
) {
    fun toPage(): WhatsNewPage? {
        val heading = heading.nonBlank() ?: return null
        val description = description.nonBlank() ?: return null
        val image = image?.let { it.toImage() ?: return null }
        val action = action?.let { it.toAction() ?: return null }

        return WhatsNewPage(image = image, heading = heading, description = description, action = action)
    }
}

@JsonClass(generateAdapter = true)
data class WhatsNewImageResponse(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val alt: String? = null,
) {
    fun toImage(): WhatsNewImage? {
        val url = url.nonBlank() ?: return null
        return WhatsNewImage(url = url, width = width, height = height, alt = alt.nonBlank())
    }
}

@JsonClass(generateAdapter = true)
data class WhatsNewActionResponse(
    val event: String? = null,
    val label: String? = null,
) {
    fun toAction(): WhatsNewAction? {
        val event = event.nonBlank() ?: return null
        val label = label.nonBlank() ?: return null
        return WhatsNewAction(event = event, label = label)
    }
}

@JsonClass(generateAdapter = true)
data class WhatsNewPollResponse(
    val pollId: String? = null,
    val pollKey: String? = null,
    val question: String? = null,
    val options: List<WhatsNewPollOptionResponse> = emptyList(),
) {
    fun toPoll(): WhatsNewPoll? {
        val pollId = pollId.nonBlank() ?: return null
        val pollKey = pollKey.nonBlank() ?: return null
        val question = question.nonBlank() ?: return null
        val options = options.map { option -> option.toOption() ?: return null }.ifEmpty { return null }

        return WhatsNewPoll(pollId = pollId, pollKey = pollKey, question = question, options = options)
    }
}

@JsonClass(generateAdapter = true)
data class WhatsNewPollOptionResponse(
    val id: String? = null,
    val pollOptionKey: String? = null,
    val label: String? = null,
) {
    fun toOption(): WhatsNewPoll.Option? {
        val id = id.nonBlank() ?: return null
        val pollOptionKey = pollOptionKey.nonBlank() ?: return null
        val label = label.nonBlank() ?: return null
        return WhatsNewPoll.Option(id = id, pollOptionKey = pollOptionKey, label = label)
    }
}

private fun String?.nonBlank() = this?.trim()?.takeIf(String::isNotEmpty)
