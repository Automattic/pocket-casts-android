package au.com.shiftyjelly.pocketcasts.sharedtest

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ModifiableFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion

internal class InMemoryFeatureProvider : ModifiableFeatureProvider {
    private val features = mutableMapOf<Feature, Boolean>()

    override var currentReleaseVersion = ReleaseVersion(Int.MAX_VALUE, Int.MAX_VALUE)

    override val priority = 0

    override fun setEnabled(feature: Feature, enabled: Boolean) {
        features[feature] = enabled
    }

    override fun hasFeature(feature: Feature) = true

    override fun isEnabled(feature: Feature) = features[feature] ?: feature.defaultValue
}
