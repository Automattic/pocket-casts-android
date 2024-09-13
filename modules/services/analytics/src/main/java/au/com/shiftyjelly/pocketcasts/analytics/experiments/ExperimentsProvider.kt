package au.com.shiftyjelly.pocketcasts.analytics.experiments

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.experimentation.Experiment
import com.automattic.android.experimentation.ExperimentLogger
import com.automattic.android.experimentation.VariationsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

@Singleton
class ExperimentsProvider @Inject constructor(
    @ApplicationContext context: Context,
    private val accountStatusInfo: AccountStatusInfo,
) {

    companion object {
        const val TAG = "Experiment"
        const val PLATFORM = "pocketcasts"
    }

    private val experiments = setOf(
        Experiment("pocketcasts_paywall_android_aa_test"),
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
            logger = PocketCastsExperimentLogger(),
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
}

private class PocketCastsExperimentLogger : ExperimentLogger {
    override fun d(message: String) {
        LogBuffer.i(ExperimentsProvider.TAG, message)
    }

    override fun e(message: String, throwable: Throwable?) {
        throwable?.let { LogBuffer.e(ExperimentsProvider.TAG, throwable, message) } ?: LogBuffer.e(ExperimentsProvider.TAG, message)
    }
}
