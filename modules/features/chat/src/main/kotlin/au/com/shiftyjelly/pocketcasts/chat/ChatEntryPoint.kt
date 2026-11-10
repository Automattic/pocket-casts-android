package au.com.shiftyjelly.pocketcasts.chat

import com.automattic.eventhorizon.EpisodeChatEntryPointType

enum class ChatEntryPoint(val analyticsValue: EpisodeChatEntryPointType) {
    PlayerShelf(EpisodeChatEntryPointType.PlayerShelf),
    MoreActions(EpisodeChatEntryPointType.MoreActions),
    History(EpisodeChatEntryPointType.History),
    SignIn(EpisodeChatEntryPointType.SignIn),
}
