package au.com.shiftyjelly.pocketcasts.featureflag

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

        val currentReleaseVersion by lazy {
            fromString(BuildConfig.VERSION_NAME) ?: error("Invalid version name: ${BuildConfig.VERSION_NAME}")
        }

        fun ReleaseVersion.comparedToEarlyPatronAccess(patronExclusiveAccessRelease: ReleaseVersion): EarlyAccessState =
            when {
                this.major < patronExclusiveAccessRelease.major ||
                    (this.major == patronExclusiveAccessRelease.major && this.minor < patronExclusiveAccessRelease.minor)
                -> EarlyAccessState.Before

                this.major > patronExclusiveAccessRelease.major ||
                    (this.major == patronExclusiveAccessRelease.major && this.minor > patronExclusiveAccessRelease.minor)
                -> EarlyAccessState.After

                else -> EarlyAccessState.During
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
