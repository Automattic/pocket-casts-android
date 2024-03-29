package au.com.shiftyjelly.pocketcasts.utils.featureflag.providers

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.MIN_PRIORITY
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Used to set feature flag values in release builds.
 * Any changes to the feature flags through FeatureFlag are not applied to feature flags
 * provided through this provider.
 */
@Singleton
class DefaultReleaseFeatureProvider @Inject constructor() : FeatureProvider {
    override val priority = MIN_PRIORITY

    override fun hasFeature(feature: Feature): Boolean = true

    override fun isEnabled(feature: Feature) = feature.defaultValue
}
