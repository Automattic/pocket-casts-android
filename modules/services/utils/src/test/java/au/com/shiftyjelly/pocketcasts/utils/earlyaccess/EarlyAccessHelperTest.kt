package au.com.shiftyjelly.pocketcasts.utils.earlyaccess

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EarlyAccessHelperTest {
    private val betaEarlyAccessRelease = ReleaseVersion(7, 50, null, 1)
    private val productionEarlyAccessRelease = ReleaseVersion(7, 50)
    private val betaFullAccessRelease = ReleaseVersion(7, 51, null, 1)
    private val productionFullAccessRelease = ReleaseVersion(7, 51)

    /* Early Access Availability */
    // Current Release                   | Beta         | Production
    // ----------------------------------|--------------|--------------
    // Beta Early Access Release         | Plus Users   | N/A
    // Production Early Access Release   | Plus Users   | Patron Users
    // Beta Full Access Release          | Plus Users   | Plus Users
    // Production Full Access Release    | Plus Users   | Plus Users
    @Test
    fun `given patron exclusive feature, when beta release, feature available to Plus`() {
        val featureTier = getFeatureTier(
            currentRelease = betaEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )
        assertTrue(featureTier is FeatureTier.Plus)
    }

    @Test
    fun `given patron exclusive feature, when production early access release, feature available to Patron`() {
        val featureTier = getFeatureTier(
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )
        assertTrue(featureTier is FeatureTier.Patron)
    }

    @Test
    fun `given not a patron exclusive feature, when production release, feature available to Plus`() {
        val featureTier = getFeatureTier(
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = null,
        )
        assertTrue(featureTier is FeatureTier.Plus)
    }

    @Test
    fun `given not a patron exclusive feature, when beta full access release, feature available to Plus`() {
        val featureTier = getFeatureTier(
            currentRelease = betaFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )
        assertTrue(featureTier is FeatureTier.Plus)
    }

    @Test
    fun `given not a patron exclusive feature, when production full access release, feature available to Plus`() {
        val featureTier = getFeatureTier(
            currentRelease = productionFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )
        assertTrue(featureTier is FeatureTier.Plus)
    }

    private fun getFeatureTier(
        currentRelease: ReleaseVersion,
        patronExclusiveAccessRelease: ReleaseVersion?,
    ): FeatureTier {
        val releaseVersion = mock<ReleaseVersionWrapper>().apply {
            Mockito.doReturn(currentRelease).whenever(this).currentReleaseVersion
        }
        val feature = mock<Feature>().apply {
            Mockito.doReturn(FeatureTier.Plus(patronExclusiveAccessRelease)).whenever(this).tier
            Mockito.doReturn(true).whenever(this).defaultValue
        }

        return EarlyAccessHelper.getAvailableForFeatureTier(feature, releaseVersion)
    }
}
