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

    fun isFeatureEnabled(feature: Feature) =
        providers.filter { it.hasFeature(feature) }
            .sortedBy(FeatureFlagProvider::priority)
            .firstOrNull()
            ?.isFeatureEnabled(feature)
            ?: feature.defaultValue

    fun setFeatureEnabled(feature: Feature, enabled: Boolean) =
        providers.filterIsInstance<ModifiableFeatureFlagProvider>()
            .firstOrNull()
            ?.setFeatureEnabled(feature, enabled)
            ?.let { true }
            ?: false

    fun refreshFeatureFlags() {
        providers.filterIsInstance<RemoteFeatureFlagProvider>()
            .forEach { it.refreshFeatureFlags() }
    }

    private fun addProvider(provider: FeatureFlagProvider) = providers.add(provider)

    fun clearFeatureFlagProviders() = providers.clear()
}
