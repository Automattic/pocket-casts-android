package au.com.shiftyjelly.pocketcasts.utils.featureflag

import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion.Companion.comparedToEarlyPatronAccess
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull

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
    private val featureFlows = ConcurrentHashMap<Feature, MutableStateFlow<Boolean>>()
    private val immutableValues = ConcurrentHashMap<Feature, Boolean>()

    fun initialize(
        providers: List<FeatureProvider>,
    ) {
        this.providers.addAll(providers)
        updateFeatureFlowValues()
    }

    /**
     * Returns whether the [feature] is enabled. If [immutable] property is set to [true]
     * then the value is snapshotted for any future calls with the [immutable] flag present.
     */
    fun isEnabled(
        feature: Feature,
        immutable: Boolean = false,
    ): Boolean {
        return if (immutable) {
            immutableValues.computeIfAbsent(feature) {
                isEnabled(feature, immutable = false)
            }
        } else {
            findProviderForFeature<FeatureProvider>(feature)
                ?.isEnabled(feature)
                ?: feature.defaultValue
        }
    }

    /**
     * Suspends until the remote config has been fetched, then returns whether the [feature] is enabled.
     * This ensures you get the latest remote value rather than a cached or default value.
     *
     * @param feature The feature to check
     * @param timeout Maximum time to wait for remote config. Defaults to 5 seconds.
     *                If timeout is reached, returns the current value (which may be cached or default)
     * @return true if the feature is enabled, false otherwise
     */
    suspend fun isEnabledWithRemote(
        feature: Feature,
        timeout: Duration = 5.seconds,
    ): Boolean {
        withTimeoutOrNull(timeout) {
            coroutineScope {
                providers.map { async { it.awaitInitialization() } }.awaitAll()
            }
        }
        updateFeatureFlowValues()

        return isEnabled(feature)
    }

    fun isEnabledFlow(
        feature: Feature,
    ): StateFlow<Boolean> {
        return featureFlows.computeIfAbsent(feature) { MutableStateFlow(isEnabled(feature)) }
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
        updateFeatureFlowValues()
    }

    fun clearProviders() {
        providers.clear()
        updateFeatureFlowValues()
    }

    fun updateFeatureFlowValues() {
        for ((feature, flow) in featureFlows) {
            flow.value = isEnabled(feature)
        }
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
