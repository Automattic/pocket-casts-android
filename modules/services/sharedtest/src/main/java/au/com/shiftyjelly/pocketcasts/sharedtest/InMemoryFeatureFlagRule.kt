package au.com.shiftyjelly.pocketcasts.sharedtest

import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class InMemoryFeatureFlagRule : TestWatcher() {
    private val provider = InMemoryFeatureProvider()

    fun setReleaseVersion(releaseVersion: ReleaseVersion) {
        provider.currentReleaseVersion = releaseVersion
    }

    override fun starting(description: Description) {
        FeatureFlag.initialize(listOf(provider))
    }

    override fun finished(description: Description) {
        FeatureFlag.clearProviders()
    }
}
