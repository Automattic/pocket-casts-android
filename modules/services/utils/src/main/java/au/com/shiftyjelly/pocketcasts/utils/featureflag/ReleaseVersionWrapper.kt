package au.com.shiftyjelly.pocketcasts.utils.featureflag

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReleaseVersionWrapper @Inject constructor() {
    val currentReleaseVersion = ReleaseVersion.currentReleaseVersion
}
