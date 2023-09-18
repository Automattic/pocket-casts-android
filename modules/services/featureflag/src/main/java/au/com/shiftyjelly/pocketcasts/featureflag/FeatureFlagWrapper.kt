package au.com.shiftyjelly.pocketcasts.featureflag

import javax.inject.Inject

open class FeatureFlagWrapper @Inject constructor() {
    open fun isEnabled(feature: Feature) = FeatureFlag.isEnabled(feature)
}
