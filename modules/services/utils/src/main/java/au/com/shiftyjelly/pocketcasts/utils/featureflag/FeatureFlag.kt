package au.com.shiftyjelly.pocketcasts.utils.featureflag

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages feature flags in the application with a list of different feature providers.
 * It allows you to initialize, query, and modify feature visibility. Each feature provider may have
 * a specific implementation based on the source (e.g. shared preferences, remote server) and
 * added based on build type (debug or release).
 *
 * Inspired from blog post: https://rb.gy/ea4eo
 */
object FeatureFlag {
    private val providers = CopyOnWriteArrayList<FeatureProvider>()

    fun initialize(providers: List<FeatureProvider>) {
        providers.forEach { addProvider(it) }
    }

    fun isEnabled(feature: Feature) =
        providers.filter { it.hasFeature(feature) }
            .sortedBy(FeatureProvider::priority)
            .firstOrNull()
            ?.isEnabled(feature)
            ?: feature.defaultValue

    fun setEnabled(feature: Feature, enabled: Boolean) =
        providers.filter { it.hasFeature(feature) }
            .filterIsInstance<ModifiableFeatureProvider>()
            .firstOrNull()
            ?.setEnabled(feature, enabled)
            ?.let { true }
            ?: false

    fun refresh() {
        providers.filterIsInstance<RemoteFeatureProvider>()
            .forEach { it.refresh() }
    }

    private fun addProvider(provider: FeatureProvider) = providers.add(provider)

    fun clearProviders() = providers.clear()
}
