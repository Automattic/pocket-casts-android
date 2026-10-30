package au.com.shiftyjelly.pocketcasts.repositories.podcast

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GeneratedChaptersAvailabilityTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.GENERATED_CHAPTERS, true)
    }

    @Test
    fun `not allowed for free users`() = runTest {
        val fixture = GeneratedChaptersFixture(initialSubscription = null)

        assertFalse(fixture.availability.isAvailable())
        assertFalse(fixture.availability.isAllowed())
        fixture.availability.isAvailableFlow.test { assertFalse(awaitItem()) }
        fixture.availability.isAllowedFlow.test { assertFalse(awaitItem()) }
    }

    @Test
    fun `allowed for Plus users`() = runTest {
        val fixture = GeneratedChaptersFixture(initialSubscription = testSubscription(SubscriptionTier.Plus))

        assertTrue(fixture.availability.isAllowed())
        fixture.availability.isAvailableFlow.test { assertTrue(awaitItem()) }
        fixture.availability.isAllowedFlow.test { assertTrue(awaitItem()) }
    }

    @Test
    fun `allowed for Patron users`() = runTest {
        val fixture = GeneratedChaptersFixture(initialSubscription = testSubscription(SubscriptionTier.Patron))

        assertTrue(fixture.availability.isAllowed())
        fixture.availability.isAvailableFlow.test { assertTrue(awaitItem()) }
        fixture.availability.isAllowedFlow.test { assertTrue(awaitItem()) }
    }

    @Test
    fun `not allowed when the feature is off`() = runTest {
        FeatureFlag.setEnabled(Feature.GENERATED_CHAPTERS, false)
        val fixture = GeneratedChaptersFixture()

        assertFalse(fixture.availability.isAllowed())
        fixture.availability.isAvailableFlow.test { assertFalse(awaitItem()) }
        fixture.availability.isAllowedFlow.test { assertFalse(awaitItem()) }
    }

    @Test
    fun `available but not allowed when the setting is off`() = runTest {
        val fixture = GeneratedChaptersFixture(initialShowGeneratedChapters = false)

        assertTrue(fixture.availability.isAvailable())
        assertFalse(fixture.availability.isAllowed())
        fixture.availability.isAvailableFlow.test { assertTrue(awaitItem()) }
        fixture.availability.isAllowedFlow.test { assertFalse(awaitItem()) }
    }

    @Test
    fun `stops being allowed when Plus lapses`() = runTest {
        val fixture = GeneratedChaptersFixture(initialSubscription = testSubscription(SubscriptionTier.Plus))

        fixture.availability.isAllowedFlow.test {
            assertTrue(awaitItem())

            fixture.subscriptionFlow.value = null
            assertFalse(awaitItem())
            assertFalse(fixture.availability.isAllowed())
        }
    }

    @Test
    fun `reacts to the feature flag changing`() = runTest {
        val fixture = GeneratedChaptersFixture()

        fixture.availability.isAvailableFlow.test {
            assertTrue(awaitItem())

            FeatureFlag.setEnabled(Feature.GENERATED_CHAPTERS, false)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `visibility moves straight from shown to unavailable when Plus lapses`() = runTest {
        val fixture = GeneratedChaptersFixture()

        fixture.availability.visibilityFlow.test {
            assertEquals(GeneratedChaptersAvailability.Visibility.Shown, awaitItem())

            fixture.showGeneratedChaptersFlow.value = false
            assertEquals(GeneratedChaptersAvailability.Visibility.Hidden, awaitItem())

            fixture.showGeneratedChaptersFlow.value = true
            assertEquals(GeneratedChaptersAvailability.Visibility.Shown, awaitItem())

            fixture.subscriptionFlow.value = null
            assertEquals(GeneratedChaptersAvailability.Visibility.Unavailable, awaitItem())
            expectNoEvents()
        }
    }
}
