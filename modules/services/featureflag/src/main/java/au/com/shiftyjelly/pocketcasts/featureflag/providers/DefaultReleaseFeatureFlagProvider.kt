package au.com.shiftyjelly.pocketcasts.featureflag.providers

import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlagProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Used to set feature flag values in release builds.
 * Any changes to the feature flags through FeatureFlagManager are not applied to feature flags
 * provided through this provider.
 */
@Singleton
class DefaultReleaseFeatureFlagProvider @Inject constructor() : FeatureFlagProvider {
    override fun isEnabled(feature: Feature) =
        when (feature) {
            Feature.END_OF_YEAR_ENABLED,
            Feature.SHOW_RATINGS_ENABLED,
            Feature.ADD_PATRON_ENABLED,
            -> false
        }
}
