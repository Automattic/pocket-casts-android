package au.com.shiftyjelly.pocketcasts.analytics.experiments

import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
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
    private val repository: VariationsRepository,
    private val accountStatusInfo: AccountStatusInfo,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        const val TAG = "ExperimentsProvider"
        const val PLATFORM = "pocketcasts"
    }

    fun initialize() {
        initialize(accountStatusInfo.getUserIds().id)
    }

    fun initialize(uuid: String) {
        if (FeatureFlag.isEnabled(Feature.EXPLAT_EXPERIMENT)) {
            LogBuffer.i(TAG, "Initializing experiments with uuid: $uuid")
            repository.initialize(anonymousId = uuid, oAuthToken = null)
        }
    }

    suspend fun refreshExperiments(uuid: String? = null) = withContext(ioDispatcher) {
        clear()
        // This will update the repository with the current user ID
        initialize(uuid ?: accountStatusInfo.getUserIds().id)
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
