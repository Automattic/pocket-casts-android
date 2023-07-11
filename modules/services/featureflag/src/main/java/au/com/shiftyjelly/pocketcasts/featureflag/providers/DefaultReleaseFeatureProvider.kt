package au.com.shiftyjelly.pocketcasts.featureflag.providers

import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureProvider
import au.com.shiftyjelly.pocketcasts.featureflag.MIN_PRIORITY
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

    override fun isEnabled(feature: Feature) =
        when (feature) {
            Feature.END_OF_YEAR_ENABLED -> Feature.END_OF_YEAR_ENABLED.defaultValue
            Feature.SHOW_RATINGS_ENABLED -> Feature.SHOW_RATINGS_ENABLED.defaultValue
            Feature.ADD_PATRON_ENABLED -> Feature.ADD_PATRON_ENABLED.defaultValue
            Feature.BOOKMARKS_ENABLED -> Feature.BOOKMARKS_ENABLED.defaultValue
        }
}
