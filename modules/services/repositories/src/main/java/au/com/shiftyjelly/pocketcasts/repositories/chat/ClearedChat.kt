package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage

interface ClearedChat {
    val messages: List<ChatMessage>
}

internal class StoredClearedChat(
    val entities: List<EpisodeChatMessage>,
    override val messages: List<ChatMessage>,
) : ClearedChat
