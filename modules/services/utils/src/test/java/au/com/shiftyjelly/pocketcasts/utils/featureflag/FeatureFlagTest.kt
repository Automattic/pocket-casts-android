package au.com.shiftyjelly.pocketcasts.utils.featureflag

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeatureFlagTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `read feature flag value`() {
        val isEnabled = FeatureFlag.isEnabled(Feature.TEST_FREE_FEATURE)

        assertTrue(isEnabled)
    }

    @Test
    fun `modify feature flag value`() {
        FeatureFlag.setEnabled(Feature.TEST_FREE_FEATURE, false)

        val isEnabled = FeatureFlag.isEnabled(Feature.TEST_FREE_FEATURE)

        assertFalse(isEnabled)
    }

    @Test
    fun `use default feature flag value when there are no providers installed`() {
        FeatureFlag.clearProviders()
        FeatureFlag.setEnabled(Feature.TEST_FREE_FEATURE, false)

        val isEnabled = FeatureFlag.isEnabled(Feature.TEST_FREE_FEATURE)

        assertTrue(isEnabled)
    }

    @Test
    fun `patron user can use free feature`() {
        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_FREE_FEATURE, SubscriptionTier.Patron)

        assertTrue(isEnabled)
    }

    @Test
    fun `patron user can use plus feature`() {
        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_FEATURE, SubscriptionTier.Patron)

        assertTrue(isEnabled)
    }

    @Test
    fun `patron user can use plus restricted feature during beta phase`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(0, 0, releaseCandidate = 1))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, SubscriptionTier.Patron)

        assertTrue(isEnabled)
    }

    @Test
    fun `patron user can use plus restricted feature before release`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(0, 0))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, SubscriptionTier.Patron)

        assertTrue(isEnabled)
    }

    @Test
    fun `patron user can use plus restricted feature during release`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(1, 0))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, SubscriptionTier.Patron)

        assertTrue(isEnabled)
    }

    @Test
    fun `patron user can use plus restricted feature after release`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(1, 1))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, SubscriptionTier.Patron)

        assertTrue(isEnabled)
    }

    @Test
    fun `patron user can use patron feature`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(0, 0))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, SubscriptionTier.Patron)

        assertTrue(isEnabled)
    }

    @Test
    fun `plus user can use free feature`() {
        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_FREE_FEATURE, SubscriptionTier.Plus)

        assertTrue(isEnabled)
    }

    @Test
    fun `plus user can use plus feature`() {
        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_FEATURE, SubscriptionTier.Plus)

        assertTrue(isEnabled)
    }

    @Test
    fun `plus user can use plus restricted feature during beta phase`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(0, 0, releaseCandidate = 1))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, SubscriptionTier.Plus)

        assertTrue(isEnabled)
    }

    @Test
    fun `plus user can't use plus restricted feature before release`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(0, 0))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, SubscriptionTier.Plus)

        assertFalse(isEnabled)
    }

    @Test
    fun `plus user can't use plus restricted feature during release`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(1, 0))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, SubscriptionTier.Plus)

        assertFalse(isEnabled)
    }

    @Test
    fun `plus user can use plus restricted feature after release`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(1, 1))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, SubscriptionTier.Plus)

        assertTrue(isEnabled)
    }

    @Test
    fun `plus user can't use patron feature`() {
        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PATRON_FEATURE, SubscriptionTier.Plus)

        assertFalse(isEnabled)
    }

    @Test
    fun `free user can use free feature`() {
        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_FREE_FEATURE, subscriptionTier = null)

        assertTrue(isEnabled)
    }

    @Test
    fun `free user can't use plus feature`() {
        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_FEATURE, subscriptionTier = null)

        assertFalse(isEnabled)
    }

    @Test
    fun `free user can't use plus restricted feature during beta phase`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(0, 0, releaseCandidate = 1))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, subscriptionTier = null)

        assertFalse(isEnabled)
    }

    @Test
    fun `free user can't use plus restricted feature before release`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(0, 0))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, subscriptionTier = null)

        assertFalse(isEnabled)
    }

    @Test
    fun `free user can't use plus restricted feature during release`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(1, 0))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, subscriptionTier = null)

        assertFalse(isEnabled)
    }

    @Test
    fun `free user can't use plus restricted feature after release`() {
        featureFlagRule.setReleaseVersion(ReleaseVersion(1, 1))

        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PLUS_RESTRICTED_FEATURE, subscriptionTier = null)

        assertFalse(isEnabled)
    }

    @Test
    fun `free user can't use patron feature`() {
        val isEnabled = FeatureFlag.isEnabledForUser(Feature.TEST_PATRON_FEATURE, subscriptionTier = null)

        assertFalse(isEnabled)
    }

    @Test
    fun `feature is not enabled for users when it itself is not enabled`() {
        FeatureFlag.setEnabled(Feature.TEST_FREE_FEATURE, false)

        assertFalse(FeatureFlag.isEnabledForUser(Feature.TEST_FREE_FEATURE, SubscriptionTier.Patron))
        assertFalse(FeatureFlag.isEnabledForUser(Feature.TEST_FREE_FEATURE, SubscriptionTier.Plus))
        assertFalse(FeatureFlag.isEnabledForUser(Feature.TEST_FREE_FEATURE, subscriptionTier = null))
    }

    @Test
    fun `feature flag values are updated`() = runTest {
        FeatureFlag.isEnabledFlow(Feature.TEST_FREE_FEATURE).test {
            assertTrue(awaitItem())

            FeatureFlag.setEnabled(Feature.TEST_FREE_FEATURE, false)
            assertFalse(awaitItem())

            FeatureFlag.setEnabled(Feature.TEST_FREE_FEATURE, true)
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `feature flag flow is cached`() {
        val flow1 = FeatureFlag.isEnabledFlow(Feature.TEST_FREE_FEATURE)
        val flow2 = FeatureFlag.isEnabledFlow(Feature.TEST_FREE_FEATURE)

        assertEquals(flow1, flow2)
    }

    @Test
    fun `immutable value doesn't change`() {
        FeatureFlag.setEnabled(Feature.TEST_FREE_FEATURE, false)
        assertFalse(FeatureFlag.isEnabled(Feature.TEST_FREE_FEATURE, immutable = true))

        FeatureFlag.setEnabled(Feature.TEST_FREE_FEATURE, true)
        assertFalse(FeatureFlag.isEnabled(Feature.TEST_FREE_FEATURE, immutable = true))
    }
}
