package au.com.shiftyjelly.pocketcasts.utils.featureflag

/**
 * Every provider has an explicit priority so they can override each other (e.g. "Firebase Remote" > "Default Release").
 *
 * Not every provider has to provide a flag value for every feature.
 * E.g. Unless you want the feature flag to be remote, feature should not be provided by the remote feature flag provider and hasFeature should return false for that feature
 * This is to avoid implicitly relying on built-in defaults.
 */
interface FeatureProvider {
    val priority: Int
    fun hasFeature(feature: Feature): Boolean
    fun isEnabled(feature: Feature): Boolean
}

interface ModifiableFeatureProvider : FeatureProvider {
    fun setEnabled(feature: Feature, enabled: Boolean)
}

interface RemoteFeatureProvider : FeatureProvider {
    fun refresh()
}

const val MAX_PRIORITY = 1
const val MIN_PRIORITY = 2
