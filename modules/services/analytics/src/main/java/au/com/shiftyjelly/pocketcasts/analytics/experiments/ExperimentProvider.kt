package au.com.shiftyjelly.pocketcasts.analytics.experiments

import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.experimentation.Experiment
import com.automattic.android.experimentation.VariationsRepository
import com.automattic.android.experimentation.domain.Variation.Control
import com.automattic.android.experimentation.domain.Variation.Treatment
import java.util.UUID
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment as ExperimentModel

class ExperimentProvider @Inject constructor(
    private val accountStatusInfo: AccountStatusInfo,
    private val repository: VariationsRepository,
) {
    companion object {
        const val TAG = "ExperimentsProvider"
        const val PLATFORM = "pocketcasts"
    }

    fun initialize() {
        if (FeatureFlag.isEnabled(Feature.EXPLAT_EXPERIMENT)) {
            val uuid = accountStatusInfo.getUuid() ?: UUID.randomUUID().toString().replace("-", "")

            LogBuffer.i(TAG, "Initializing experiments with uuid: $uuid")

            repository.initialize(anonymousId = uuid, oAuthToken = null)
        }
    }

    fun clear() {
        if (FeatureFlag.isEnabled(Feature.EXPLAT_EXPERIMENT)) {
            LogBuffer.i(TAG, "Clearing experiments")
            repository.clear()
        }
    }

    fun getVariation(experiment: ExperimentModel): Variation? {
        if (!FeatureFlag.isEnabled(Feature.EXPLAT_EXPERIMENT)) return null

        return when (val variation = repository.getVariation(Experiment(experiment.identifier))) {
            is Control -> Variation.Control
            is Treatment -> Variation.Treatment(variation.name)
            else -> null
        }
    }
}
