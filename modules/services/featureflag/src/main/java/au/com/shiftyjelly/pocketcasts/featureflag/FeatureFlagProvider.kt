package au.com.shiftyjelly.pocketcasts.featureflag

/**
 * Every provider has an explicit priority so they can override each other (e.g. "Firebase Remote" > Store).
 *
 * Not every provider has to provide a flag value for every feature.
 * E.g. Unless you want the feature flag to be remote, feature should not be provided by the remote feature flag provider and hasFeature should return false for that feature
 * This is to avoid implicitly relying on built-in defaults.
 */
interface FeatureFlagProvider {
    val priority: Int
    fun isFeatureEnabled(feature: Feature): Boolean
    fun hasFeature(feature: Feature): Boolean
}

interface ModifiableFeatureFlagProvider : FeatureFlagProvider {
    fun setFeatureEnabled(feature: Feature, enabled: Boolean)
}

interface RemoteFeatureFlagProvider : FeatureFlagProvider {
    fun refreshFeatureFlags()
}

const val MAX_PRIORITY = 1
const val MIN_PRIORITY = 2
