package au.com.shiftyjelly.pocketcasts.utils.earlyaccess

import au.com.shiftyjelly.pocketcasts.utils.featureflag.EarlyAccessState
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion.Companion.comparedToEarlyPatronAccess
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper

object EarlyAccessHelper {
    fun getAvailableForFeatureTier(
        feature: Feature,
        releaseVersion: ReleaseVersionWrapper = ReleaseVersionWrapper(),
    ): FeatureTier {
        val patronExclusiveAccessRelease =
            (feature.tier as? FeatureTier.Plus)?.patronExclusiveAccessRelease

        val isReleaseCandidate = releaseVersion.currentReleaseVersion.releaseCandidate != null
        val relativeToEarlyPatronAccess = patronExclusiveAccessRelease?.let {
            releaseVersion.currentReleaseVersion.comparedToEarlyPatronAccess(it)
        }
        return when (relativeToEarlyPatronAccess) {
            null -> feature.tier
            EarlyAccessState.Before,
            EarlyAccessState.During,
            -> if (isReleaseCandidate) feature.tier else FeatureTier.Patron

            EarlyAccessState.After -> feature.tier
        }
    }
}
