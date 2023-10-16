package au.com.shiftyjelly.pocketcasts.utils.featureflag

import javax.inject.Inject

class FeatureWrapper @Inject constructor() {
    fun isUserEntitled(feature: Feature, userTier: UserTier) = Feature.isUserEntitled(feature, userTier)
    val bookmarksFeature = Feature.BOOKMARKS_ENABLED
}
