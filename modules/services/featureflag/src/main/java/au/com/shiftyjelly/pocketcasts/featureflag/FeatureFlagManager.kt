package au.com.shiftyjelly.pocketcasts.featureflag

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages feature flags in the application with a list of different flag providers.
 * It allows you to initialize, query, and modify flags. Each feature flag provider may have
 * a specific implementation based on the source of the flag (e.g. shared preferences, remote server) and
 * added based on build type (debug or release).
 *
 * Inspired from blog post: https://rb.gy/ea4eo
 */
object FeatureFlagManager {
    private val providers = CopyOnWriteArrayList<FeatureFlagProvider>()

    fun initialize(providers: List<FeatureFlagProvider>) {
        providers.forEach { addProvider(it) }
    }

    fun isFeatureEnabled(feature: Feature): Boolean {
        // TODO: Add support for (Firebase) Remote Feature Flags Provider
        if (providers.size > 1) {
            throw IllegalStateException("Multiple providers not yet supported for feature flags")
        }
        return providers.firstOrNull()
            ?.isEnabled(feature)
            ?: feature.defaultValue
    }

    fun setFeatureEnabled(feature: Feature, enabled: Boolean) =
        providers.filterIsInstance(ModifiableFeatureFlagProvider::class.java)
            .firstOrNull()
            ?.setEnabled(feature, enabled)
            ?.let { true }
            ?: false

    private fun addProvider(provider: FeatureFlagProvider) = providers.add(provider)

    fun clearFeatureFlagProviders() = providers.clear()
}
