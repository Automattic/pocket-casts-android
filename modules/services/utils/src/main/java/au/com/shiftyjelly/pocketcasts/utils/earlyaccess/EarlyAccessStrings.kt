package au.com.shiftyjelly.pocketcasts.utils.earlyaccess

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.utils.featureflag.EarlyAccessState
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion.Companion.comparedToEarlyPatronAccess
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper

object EarlyAccessStrings {
    fun getAppropriateTextResource(
        @StringRes titleRes: Int,
        @StringRes joinBetaRes: Int? = null,
        earlyAccessFeature: Feature? = null,
        releaseVersion: ReleaseVersionWrapper = ReleaseVersionWrapper(),
    ): Int {
        if (earlyAccessFeature == null || joinBetaRes == null) {
            return titleRes
        } else {
            val patronExclusiveAccessRelease =
                (earlyAccessFeature.tier as? FeatureTier.Plus)?.patronExclusiveAccessRelease

            val isReleaseCandidate = releaseVersion.currentReleaseVersion.releaseCandidate != null
            val relativeToEarlyPatronAccess = patronExclusiveAccessRelease?.let {
                releaseVersion.currentReleaseVersion.comparedToEarlyPatronAccess(it)
            }

            val showJoinBeta = when (relativeToEarlyPatronAccess) {
                EarlyAccessState.Before,
                EarlyAccessState.During,
                -> isReleaseCandidate

                EarlyAccessState.After -> false
                null -> false
            }

            return if (showJoinBeta) {
                joinBetaRes
            } else {
                titleRes
            }
        }
    }
}
