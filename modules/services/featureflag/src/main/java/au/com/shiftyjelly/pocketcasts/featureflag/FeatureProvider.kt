package au.com.shiftyjelly.pocketcasts.featureflag

interface FeatureProvider {
    fun isEnabled(feature: Feature): Boolean
}

interface ModifiableFeatureProvider : FeatureProvider {
    fun setEnabled(feature: Feature, enabled: Boolean)
}
