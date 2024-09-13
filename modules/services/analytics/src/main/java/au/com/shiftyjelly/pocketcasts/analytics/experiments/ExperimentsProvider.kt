package au.com.shiftyjelly.pocketcasts.analytics.experiments

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment.PaywallAATest
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.experimentation.Experiment
import com.automattic.android.experimentation.VariationsRepository
import com.automattic.android.experimentation.domain.Variation.Control
import com.automattic.android.experimentation.domain.Variation.Treatment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment as ExperimentModel

@Singleton
class ExperimentsProvider @Inject constructor(
    @ApplicationContext context: Context,
    private val accountStatusInfo: AccountStatusInfo,
    private val logger: ExperimentLogger,
) {

    companion object {
        const val TAG = "ExperimentsProvider"
        const val PLATFORM = "pocketcasts"
    }

    private val experiments = setOf(
        Experiment(PaywallAATest.identifier),
    )

    private val cacheDir: File by lazy {
        File(context.cacheDir, "experiments_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    private val repository: VariationsRepository by lazy {
        VariationsRepository.create(
            platform = PLATFORM,
            experiments = experiments,
            logger = logger,
            failFast = true,
            cacheDir = cacheDir,
            coroutineScope = CoroutineScope(Dispatchers.IO + Job()),
        )
    }

    fun initialize() {
        val uuid = accountStatusInfo.getUuid() ?: UUID.randomUUID().toString().replace("-", "")

        LogBuffer.i(TAG, "Initializing experiments with uuid: $uuid")

        repository.initialize(anonymousId = uuid)
    }

    fun clear() {
        LogBuffer.i(TAG, "Clearing experiments")
        repository.clear()
    }

    fun getVariation(experiment: ExperimentModel): Variation? {
        return when (val variation = repository.getVariation(Experiment(experiment.identifier))) {
            is Control -> {
                Variation.Control
            }

            is Treatment -> {
                Variation.Treatment(variation.name)
            }

            else -> {
                null
            }
        }
    }
}
