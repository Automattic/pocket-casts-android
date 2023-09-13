package au.com.shiftyjelly.pocketcasts.featureflag

import javax.inject.Inject

class FeatureFlagWrapper @Inject constructor() {
    fun isEnabled(feature: Feature) = FeatureFlag.isEnabled(feature)
}
