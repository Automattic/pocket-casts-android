package au.com.shiftyjelly.pocketcasts.featureflag

interface FeatureFlagProvider {
    fun isEnabled(feature: Feature): Boolean
}

interface ModifiableFeatureFlagProvider : FeatureFlagProvider {
    fun setEnabled(feature: Feature, enabled: Boolean)
}
