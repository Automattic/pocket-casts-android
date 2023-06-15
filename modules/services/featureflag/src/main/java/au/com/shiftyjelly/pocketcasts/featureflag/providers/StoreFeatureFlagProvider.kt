package au.com.shiftyjelly.pocketcasts.featureflag.providers

import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlagProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Used to set feature flag values in release builds.
 * Any changes to the feature flags through FeatureFlagManager are not applied to feature flags
 * provided through this provider.
 */
@Singleton
class StoreFeatureFlagProvider @Inject constructor() : FeatureFlagProvider {
    override fun isFeatureEnabled(feature: Feature) =
        if (feature is FeatureFlag) {
            when (feature) {
                FeatureFlag.END_OF_YEAR_ENABLED,
                FeatureFlag.SHOW_RATINGS_ENABLED,
                FeatureFlag.ADD_PATRON_ENABLED,
                -> false
            }
        } else {
            false
        }
}
