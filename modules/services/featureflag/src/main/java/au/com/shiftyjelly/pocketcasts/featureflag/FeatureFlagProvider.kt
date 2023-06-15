package au.com.shiftyjelly.pocketcasts.featureflag

interface FeatureFlagProvider {
    fun isFeatureEnabled(feature: Feature): Boolean
}

interface ModifiableFeatureFlagProvider : FeatureFlagProvider {
    fun setFeatureEnabled(feature: Feature, enabled: Boolean)
}
