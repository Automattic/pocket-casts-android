package au.com.shiftyjelly.pocketcasts.featureflag

import au.com.shiftyjelly.pocketcasts.featureflag.ReleaseVersion.Companion.matchesCurrentReleaseForEarlyPatronAccess
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(MockitoJUnitRunner::class)
class FeatureTest {

    /*
     * isAvailable: all users
     */

    @Test
    fun `no users can use free features that default to false`() {
        listOf(
            UserTier.Free,
            UserTier.Plus,
            UserTier.Patron,
        ).forEach { userTier ->

            listOf(
                FeatureTier.Free,
                FeatureTier.Plus(null),
                FeatureTier.Patron,
            ).forEach { featureTier ->

                val feature = mock<Feature> {
                    on { defaultValue } doReturn false
                    on { tier } doReturn featureTier
                }
                assertFalse("$userTier", Feature.isAvailable(feature, userTier))
            }
        }
    }

    /*
     * isAvailable: patron users
     */

    @Test
    fun `patron can use features that default to true`() {
        val feature = mock<Feature> {
            on { defaultValue } doReturn true
        }
        assertTrue(Feature.isAvailable(feature, UserTier.Patron))
    }

    @Test
    fun `patron cannot use features that default to false`() {
        val feature = mock<Feature> {
            on { defaultValue } doReturn false
        }
        assertFalse(Feature.isAvailable(feature, UserTier.Patron))
    }

    /*
     * isAvailble: plus user
     */

    @Test
    @Ignore("Test fails because it can't mock companion method")
    fun `plus can use plus features that default to true and are not in early access`() {
        val exclusiveAccessRelease = mock<ReleaseVersion> {
            on { matchesCurrentReleaseForEarlyPatronAccess() } doReturn false
        }
        val plusTier = mock<FeatureTier.Plus> {
            on { exclusiveAccessRelease } doReturn exclusiveAccessRelease
        }
        val feature = mock<Feature> {
            on { defaultValue } doReturn true
            on { tier } doReturn plusTier
        }
        assertTrue(Feature.isAvailable(feature, UserTier.Plus))
    }

    @Test
    @Ignore("Test fails because it can't mock companion method")
    fun `plus cannot use plus features that default to false even if not in early access`() {
        val exclusiveAccessRelease = mock<ReleaseVersion> {
            on { matchesCurrentReleaseForEarlyPatronAccess() } doReturn false
        }
        val plusTier = mock<FeatureTier.Plus> {
            on { exclusiveAccessRelease } doReturn exclusiveAccessRelease
        }
        val feature = mock<Feature> {
            on { defaultValue } doReturn false
            on { tier } doReturn plusTier
        }
        assertFalse(Feature.isAvailable(feature, UserTier.Plus))
    }

    @Test
    @Ignore("Test fails because it can't mock companion method")
    fun `plus cannot use plus features that default to true if in early access`() {
        val exclusiveAccessRelease = mock<ReleaseVersion> {
            on { matchesCurrentReleaseForEarlyPatronAccess() } doReturn true
        }
        val plusTier = mock<FeatureTier.Plus> {
            on { exclusiveAccessRelease } doReturn exclusiveAccessRelease
        }
        val feature = mock<Feature> {
            on { defaultValue } doReturn true
            on { tier } doReturn plusTier
        }
        assertFalse(Feature.isAvailable(feature, UserTier.Plus))
    }

    /*
     * isAvailable: free user
     */

    @Test
    fun `free user can use free features that default to true`() {
        val feature = mock<Feature> {
            on { defaultValue } doReturn true
            on { tier } doReturn FeatureTier.Free
        }
        assertTrue(Feature.isAvailable(feature, UserTier.Free))
    }

    @Test
    fun `free user cannot use free features that default to false`() {
        val feature = mock<Feature> {
            on { defaultValue } doReturn false
            on { tier } doReturn FeatureTier.Free
        }
        assertFalse(Feature.isAvailable(feature, UserTier.Free))
    }

    @Test
    fun `free user cannot use Plus features`() {
        val feature = mock<Feature> {
            on { tier } doReturn FeatureTier.Plus(null)
        }
        assertFalse(Feature.isAvailable(feature, UserTier.Free))
    }

    @Test
    fun `free user cannot use Patron features`() {
        val feature = mock<Feature> {
            on { tier } doReturn FeatureTier.Patron
        }
        assertFalse(Feature.isAvailable(feature, UserTier.Free))
    }
}
