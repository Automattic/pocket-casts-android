package au.com.shiftyjelly.pocketcasts.sharedtest

import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.InMemoryFeatureProvider
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class InMemoryFeatureFlagRule : TestWatcher() {
    override fun starting(description: Description) {
        FeatureFlag.initialize(
            listOf(InMemoryFeatureProvider()),
        )
    }

    override fun finished(description: Description) {
        FeatureFlag.clearProviders()
    }
}
