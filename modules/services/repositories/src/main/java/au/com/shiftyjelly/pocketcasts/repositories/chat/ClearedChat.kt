package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage

interface ClearedChat

internal class StoredClearedChat(val messages: List<EpisodeChatMessage>) : ClearedChat
