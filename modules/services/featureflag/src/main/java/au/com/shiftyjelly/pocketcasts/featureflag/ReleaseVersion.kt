package au.com.shiftyjelly.pocketcasts.featureflag

import androidx.annotation.VisibleForTesting
import timber.log.Timber

data class ReleaseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int? = null,
    val releaseCandidate: Int? = null,
) : Comparable<ReleaseVersion> {

    override fun compareTo(other: ReleaseVersion): Int {
        return when {
            this.major != other.major -> this.major - other.major
            this.minor != other.minor -> this.minor - other.minor
            (this.patch ?: 0) != (other.patch ?: 0) -> (this.patch ?: 0) - (other.patch ?: 0)
            (this.releaseCandidate ?: Int.MAX_VALUE) != (other.releaseCandidate ?: Int.MAX_VALUE) ->
                (this.releaseCandidate ?: Int.MAX_VALUE) - (other.releaseCandidate ?: Int.MAX_VALUE)
            else -> 0
        }
    }

    companion object {

        private val currentReleaseVersion by lazy {
            fromString(BuildConfig.VERSION_NAME) ?: error("Invalid version name: ${BuildConfig.VERSION_NAME}")
        }

        fun ReleaseVersion?.matchesCurrentReleaseForEarlyPatronAccess() =
            this.matchesCurrentReleaseForEarlyPatronAccess(currentReleaseVersion)

        // If major and minor versions match, and this is not a release candidate, this is the
        // early access release. We don't want to include release candidates because we want to
        // allow broader testing of the feature. Patch versions are ignored.
        @VisibleForTesting
        internal fun ReleaseVersion?.matchesCurrentReleaseForEarlyPatronAccess(
            currentReleaseVersion: ReleaseVersion, // this parameter is just to allow testing
        ) = when {
            this == null -> {
                Timber.e("ReleaseVersion.matchesCurrentReleaseForEarlyPatronAccess called on null")
                false
            }
            this.major != currentReleaseVersion.major -> false
            minor != currentReleaseVersion.minor -> false
            releaseCandidate != null -> false
            else -> true
        }

        fun fromString(versionName: String): ReleaseVersion? {
            val regex = Regex("""(\d+)\.(\d+)(?:\.(\d+))?(?:-rc-(\d+))?""")
            val match = regex.find(versionName) ?: return null

            val (majorStr, minorStr, patchStr, rcStr) = match.destructured
            val major = majorStr.toInt()
            val minor = minorStr.toInt()
            val patch = if (patchStr.isNotBlank()) patchStr.toInt() else null
            val rc = if (rcStr.isNotBlank()) rcStr.toInt() else null

            return ReleaseVersion(major, minor, patch, rc)
        }
    }
}
