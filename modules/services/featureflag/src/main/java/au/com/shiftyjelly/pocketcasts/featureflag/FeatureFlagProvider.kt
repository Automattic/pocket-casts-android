package au.com.shiftyjelly.pocketcasts.featureflag

interface FeatureFlagProvider {
    fun isEnabled(featureFlag: FeatureFlag): Boolean
}

interface ModifiableFeatureFlagProvider : FeatureFlagProvider {
    fun setEnabled(featureFlag: FeatureFlag, enabled: Boolean)
}
