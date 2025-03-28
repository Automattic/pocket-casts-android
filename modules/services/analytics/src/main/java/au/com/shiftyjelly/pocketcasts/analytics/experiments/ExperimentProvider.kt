package au.com.shiftyjelly.pocketcasts.analytics.experiments

import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.experimentation.Experiment
import com.automattic.android.experimentation.VariationsRepository
import com.automattic.android.experimentation.domain.Variation.Control
import com.automattic.android.experimentation.domain.Variation.Treatment
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExperimentProvider @Inject constructor(
    private val tracksAnalyticsTracker: TracksAnalyticsTracker,
    private val repository: VariationsRepository,
    private val accountStatusInfo: AccountStatusInfo,
    private val iODispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        const val TAG = "ExperimentsProvider"
        const val PLATFORM = "pocketcasts"
    }

    fun initialize(newUuid: String? = null) {
        if (FeatureFlag.isEnabled(Feature.EXPLAT_EXPERIMENT)) {
            // We need to attempt to retrieve the UUID from accountStatusInfo instead of tracksAnalyticsTracker due to a race condition
            // that delays UUID refresh after logging in.
            val uuid = newUuid ?: accountStatusInfo.getUuid() ?: tracksAnalyticsTracker.anonID ?: tracksAnalyticsTracker.generateNewAnonID()

            LogBuffer.i(TAG, "Initializing experiments with uuid: $uuid")

            repository.initialize(anonymousId = uuid, oAuthToken = null)
        }
    }

    suspend fun refreshExperiments(newUuid: String? = null) = withContext(iODispatcher) {
        clear()
        initialize(newUuid)
    }

    fun getVariation(experiment: ExperimentType): Variation? {
        if (!FeatureFlag.isEnabled(Feature.EXPLAT_EXPERIMENT)) return null

        return when (val variation = repository.getVariation(Experiment(experiment.identifier))) {
            is Control -> Variation.Control
            is Treatment -> Variation.Treatment(variation.name)
        }
    }

    private fun clear() {
        if (FeatureFlag.isEnabled(Feature.EXPLAT_EXPERIMENT)) {
            LogBuffer.i(TAG, "Clearing experiments")
            repository.clear()
        }
    }
}
