package au.com.shiftyjelly.pocketcasts.utils.earlyaccess

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EarlyAccessStringsTest {

    private val betaEarlyAccessRelease = ReleaseVersion(7, 50, null, 1)
    private val productionEarlyAccessRelease = ReleaseVersion(7, 50)
    private val betaFullAccessRelease = ReleaseVersion(7, 51, null, 1)
    private val productionFullAccessRelease = ReleaseVersion(7, 51)

    private val titleRes = 1
    private val joinBetaRes = 2

    @Test
    fun `given join beta resource null, then default resource is shown`() {
        val resource =
            EarlyAccessStrings.getAppropriateTextResource(titleRes = titleRes, joinBetaRes = null)
        assertEquals(titleRes, resource)
    }

    @Test
    fun `given feature null, then default resource is shown`() {
        val resource = EarlyAccessStrings.getAppropriateTextResource(
            titleRes = titleRes,
            earlyAccessFeature = null,
        )
        assertEquals(titleRes, resource)
    }

    @Test
    fun `when beta early access release, then join beta testing title shown`() {
        val resource = getEarlyAccessString(
            titleRes = titleRes,
            joinBetaRes = joinBetaRes,
            currentRelease = betaEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        assertEquals(joinBetaRes, resource)
    }

    @Test
    fun `when prod early access release, then default resource is shown`() {
        val resource = getEarlyAccessString(
            titleRes = titleRes,
            joinBetaRes = joinBetaRes,
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        assertEquals(titleRes, resource)
    }

    @Test
    fun `when beta full access release, then default resource is shown`() {
        val resource = getEarlyAccessString(
            titleRes = titleRes,
            joinBetaRes = joinBetaRes,
            currentRelease = betaFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        assertEquals(titleRes, resource)
    }

    @Test
    fun `when prod full access release, then default resource is shown`() {
        val resource = getEarlyAccessString(
            titleRes = titleRes,
            joinBetaRes = joinBetaRes,
            currentRelease = productionFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        assertEquals(titleRes, resource)
    }
    private fun getEarlyAccessString(
        titleRes: Int,
        joinBetaRes: Int? = null,
        currentRelease: ReleaseVersion,
        patronExclusiveAccessRelease: ReleaseVersion?,
    ): Int {
        val releaseVersion = mock<ReleaseVersionWrapper>().apply {
            doReturn(currentRelease).whenever(this).currentReleaseVersion
        }
        val feature = mock<Feature>().apply {
            doReturn(FeatureTier.Plus(patronExclusiveAccessRelease)).whenever(this).tier
            doReturn(true).whenever(this).defaultValue
        }

        return EarlyAccessStrings.getAppropriateTextResource(titleRes, joinBetaRes, feature, releaseVersion)
    }
}
