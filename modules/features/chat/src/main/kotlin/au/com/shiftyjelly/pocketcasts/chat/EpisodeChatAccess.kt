package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

sealed interface EpisodeChatAccess {
    data class Chat(val isBeta: Boolean) : EpisodeChatAccess

    data object Paywall : EpisodeChatAccess

    companion object {
        fun from(
            isPlusUser: Boolean,
            isSignedIn: Boolean,
        ): EpisodeChatAccess = when {
            isPlusUser -> Chat(isBeta = false)
            isSignedIn && FeatureFlag.isEnabled(Feature.EPISODE_CHAT_FREE_BETA) -> Chat(isBeta = true)
            else -> Paywall
        }
    }
}
