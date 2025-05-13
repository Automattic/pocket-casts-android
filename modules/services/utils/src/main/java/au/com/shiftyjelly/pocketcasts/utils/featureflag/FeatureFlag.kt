package au.com.shiftyjelly.pocketcasts.utils.featureflag

import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion.Companion.comparedToEarlyPatronAccess
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

    fun initialize(
        providers: List<FeatureProvider>,
    ) {
        this.providers.addAll(providers)
    }

    fun isEnabled(
        feature: Feature,
    ): Boolean {
        return findProviderForFeature<FeatureProvider>(feature)
            ?.isEnabled(feature)
            ?: feature.defaultValue
    }

    fun isEnabledForUser(
        feature: Feature,
        subscriptionTier: SubscriptionTier?,
    ): Boolean {
        return isEnabled(feature) && isFeatureAllowedForCurrentVersion(feature, subscriptionTier)
    }

    fun isExclusiveToPatron(
        feature: Feature,
    ): Boolean = isEnabledForUser(feature, SubscriptionTier.Patron) && !isEnabledForUser(feature, SubscriptionTier.Plus)

    fun setEnabled(
        feature: Feature,
        enabled: Boolean,
    ) {
        findProviderForFeature<ModifiableFeatureProvider>(feature)?.setEnabled(feature, enabled)
    }

    fun refresh() {
        providers.filterIsInstance<RemoteFeatureProvider>().forEach { it.refresh() }
    }

    fun clearProviders() {
        providers.clear()
    }

    private fun isFeatureAllowedForCurrentVersion(
        feature: Feature,
        subscriptionTier: SubscriptionTier?,
    ): Boolean {
        return when (subscriptionTier) {
            SubscriptionTier.Patron -> true

            SubscriptionTier.Plus -> when (feature.tier) {
                is FeatureTier.Free -> true
                is FeatureTier.Patron -> false
                is FeatureTier.Plus -> {
                    val provider = findProviderForFeature<FeatureProvider>(feature) ?: return true
                    val releaseVersion = provider.currentReleaseVersion
                    val isReleaseCandidate = releaseVersion.releaseCandidate != null
                    val earlyAccessState = feature.tier.patronExclusiveAccessRelease?.let { featureVersion ->
                        releaseVersion.comparedToEarlyPatronAccess(featureVersion)
                    }
                    when (earlyAccessState) {
                        EarlyAccessState.Before, EarlyAccessState.During -> isReleaseCandidate
                        EarlyAccessState.After, null -> true
                    }
                }
            }

            null -> when (feature.tier) {
                is FeatureTier.Free -> true
                is FeatureTier.Patron -> false
                is FeatureTier.Plus -> false
            }
        }
    }

    private inline fun <reified T : FeatureProvider> findProviderForFeature(feature: Feature): T? {
        return providers
            .filterIsInstance<T>()
            .filter { it.hasFeature(feature) }
            .sortedBy(FeatureProvider::priority)
            .firstOrNull()
    }
}
