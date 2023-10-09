package au.com.shiftyjelly.pocketcasts.utils.featureflag

import javax.inject.Inject

class FeatureWrapper @Inject constructor() {
    fun isAvailable(feature: Feature, userTier: UserTier) = Feature.isAvailable(feature, userTier)
}
