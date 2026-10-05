package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EpisodeChatAccessTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `plus user gets chat without beta`() {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT_FREE_BETA, true)

        assertEquals(EpisodeChatAccess.Chat(isBeta = false), EpisodeChatAccess.from(isPlusUser = true, isSignedIn = true))
    }

    @Test
    fun `signed in free user gets beta chat when free beta is on`() {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT_FREE_BETA, true)

        assertEquals(EpisodeChatAccess.Chat(isBeta = true), EpisodeChatAccess.from(isPlusUser = false, isSignedIn = true))
    }

    @Test
    fun `signed in free user gets paywall when free beta is off`() {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT_FREE_BETA, false)

        assertEquals(EpisodeChatAccess.Paywall, EpisodeChatAccess.from(isPlusUser = false, isSignedIn = true))
    }

    @Test
    fun `signed out user gets paywall even when free beta is on`() {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT_FREE_BETA, true)

        assertEquals(EpisodeChatAccess.Paywall, EpisodeChatAccess.from(isPlusUser = false, isSignedIn = false))
    }
}
