package au.com.shiftyjelly.pocketcasts.featureflag

import javax.inject.Inject

class ReleaseVersionWrapper @Inject constructor() {
    val currentReleaseVersion = ReleaseVersion.currentReleaseVersion
}
