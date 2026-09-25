package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlusUpgradeFeatureItemTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `given episode chat flag disabled, when checking feature visibility, then episode chat is hidden`() {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT, false)

        assertFalse(PlusUpgradeFeatureItem.EpisodeChat.isMonthlyFeature)
        assertFalse(PlusUpgradeFeatureItem.EpisodeChat.isYearlyFeature)
    }

    @Test
    fun `given episode chat flag enabled, when checking feature visibility, then episode chat is shown`() {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT, true)

        assertTrue(PlusUpgradeFeatureItem.EpisodeChat.isMonthlyFeature)
        assertTrue(PlusUpgradeFeatureItem.EpisodeChat.isYearlyFeature)
    }
}
