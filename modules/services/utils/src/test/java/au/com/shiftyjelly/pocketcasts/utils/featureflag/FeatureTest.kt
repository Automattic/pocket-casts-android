package au.com.shiftyjelly.pocketcasts.utils.featureflag

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(MockitoJUnitRunner::class)
class FeatureTest {

    private val betaBeforeEarlyAccessRelease = ReleaseVersion(7, 49, null, 1)
    private val productionBeforeEarlyAccessRelease = ReleaseVersion(7, 49)
    private val betaEarlyAccessRelease = ReleaseVersion(7, 50, null, 1)
    private val productionEarlyAccessRelease = ReleaseVersion(7, 50)
    private val betaAfterEarlyAccessRelease = ReleaseVersion(7, 51, null, 1)
    private val productionAfterEarlyAccessRelease = ReleaseVersion(7, 51)

    /*
     * isAvailable: patron users
     */

    @Test
    fun `patron can use features`() {
        listOf(
            FeatureTier.Free,
            FeatureTier.Plus(null),
            FeatureTier.Patron,
        ).forEach { featureTier ->

            val feature = mock<Feature> {
                on { tier } doReturn featureTier
            }
            assertTrue("$featureTier", Feature.isUserEntitled(feature, UserTier.Patron))
        }
    }

    /*
     * isAvailable: plus user
     */
    @Test
    fun `plus can use plus features if before early access (beta)`() {
        val releaseVersionWrapper = mock<ReleaseVersionWrapper>() {
            on { currentReleaseVersion } doReturn betaBeforeEarlyAccessRelease
        }
        val plusTier = mock<FeatureTier.Plus> {
            on { patronExclusiveAccessRelease } doReturn productionEarlyAccessRelease
        }
        val feature = mock<Feature> {
            on { tier } doReturn plusTier
        }
        assertTrue(Feature.isUserEntitled(feature, UserTier.Plus, releaseVersionWrapper))
    }

    @Test
    fun `plus cannot use plus features if before early access (production)`() {
        val releaseVersionWrapper = mock<ReleaseVersionWrapper> {
            on { currentReleaseVersion } doReturn productionBeforeEarlyAccessRelease
        }
        val plusTier = mock<FeatureTier.Plus> {
            on { patronExclusiveAccessRelease } doReturn productionEarlyAccessRelease
        }
        val feature = mock<Feature> {
            on { tier } doReturn plusTier
        }
        assertFalse(Feature.isUserEntitled(feature, UserTier.Plus, releaseVersionWrapper))
    }

    @Test
    fun `plus can use plus features if in early access (beta)`() {
        val releaseVersionWrapper = mock<ReleaseVersionWrapper>() {
            on { currentReleaseVersion } doReturn betaEarlyAccessRelease
        }
        val plusTier = mock<FeatureTier.Plus> {
            on { patronExclusiveAccessRelease } doReturn productionEarlyAccessRelease
        }
        val feature = mock<Feature> {
            on { tier } doReturn plusTier
        }
        assertTrue(Feature.isUserEntitled(feature, UserTier.Plus, releaseVersionWrapper))
    }

    @Test
    fun `plus cannot use plus features if in early access (production)`() {
        val releaseVersionWrapper = mock<ReleaseVersionWrapper> {
            on { currentReleaseVersion } doReturn productionEarlyAccessRelease
        }
        val plusTier = mock<FeatureTier.Plus> {
            on { patronExclusiveAccessRelease } doReturn productionEarlyAccessRelease
        }
        val feature = mock<Feature> {
            on { tier } doReturn plusTier
        }
        assertFalse(Feature.isUserEntitled(feature, UserTier.Plus, releaseVersionWrapper))
    }

    @Test
    fun `plus can use plus features and are after early access (beta)`() {
        val releaseVersionWrapper = mock<ReleaseVersionWrapper> {
            on { currentReleaseVersion } doReturn betaAfterEarlyAccessRelease
        }
        val plusTier = mock<FeatureTier.Plus> {
            on { patronExclusiveAccessRelease } doReturn productionEarlyAccessRelease
        }
        val feature = mock<Feature> {
            on { tier } doReturn plusTier
        }
        assertTrue(Feature.isUserEntitled(feature, UserTier.Plus, releaseVersionWrapper))
    }

    @Test
    fun `plus can use plus features and are after early access (production)`() {
        val releaseVersionWrapper = mock<ReleaseVersionWrapper> {
            on { currentReleaseVersion } doReturn productionAfterEarlyAccessRelease
        }
        val plusTier = mock<FeatureTier.Plus> {
            on { patronExclusiveAccessRelease } doReturn productionEarlyAccessRelease
        }
        val feature = mock<Feature> {
            on { tier } doReturn plusTier
        }
        assertTrue(Feature.isUserEntitled(feature, UserTier.Plus, releaseVersionWrapper))
    }

    /*
     * isAvailable: free user
     */

    @Test
    fun `free user can use free features`() {
        val feature = mock<Feature> {
            on { tier } doReturn FeatureTier.Free
        }
        assertTrue(Feature.isUserEntitled(feature, UserTier.Free))
    }

    @Test
    fun `free user cannot use Plus features`() {
        val feature = mock<Feature> {
            on { tier } doReturn FeatureTier.Plus(null)
        }
        assertFalse(Feature.isUserEntitled(feature, UserTier.Free))
    }

    @Test
    fun `free user cannot use Patron features`() {
        val feature = mock<Feature> {
            on { tier } doReturn FeatureTier.Patron
        }
        assertFalse(Feature.isUserEntitled(feature, UserTier.Free))
    }
}
