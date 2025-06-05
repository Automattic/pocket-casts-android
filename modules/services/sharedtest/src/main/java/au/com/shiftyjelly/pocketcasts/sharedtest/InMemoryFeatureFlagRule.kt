package au.com.shiftyjelly.pocketcasts.sharedtest

import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ModifiableFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class InMemoryFeatureFlagRule() : TestWatcher() {
    private val _provider = InMemoryFeatureProvider()
    val provider: ModifiableFeatureProvider = InMemoryFeatureProvider()

    fun setReleaseVersion(releaseVersion: ReleaseVersion) {
        _provider.currentReleaseVersion = releaseVersion
    }

    override fun starting(description: Description) {
        FeatureFlag.initialize(listOf(_provider))
    }

    override fun finished(description: Description) {
        FeatureFlag.clearProviders()
    }
}
