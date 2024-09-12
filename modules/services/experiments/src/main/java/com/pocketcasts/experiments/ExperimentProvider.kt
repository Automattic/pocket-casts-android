package com.pocketcasts.experiments

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.experimentation.Experiment
import com.automattic.android.experimentation.ExperimentLogger
import com.automattic.android.experimentation.VariationsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Singleton
class ExperimentProvider @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope coroutineScope: CoroutineScope,
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
            coroutineScope = coroutineScope,
        )
    }

    fun initialize(anonymousId: String) {
        repository.initialize(anonymousId = anonymousId)
    }
}

private class PocketCastsExperimentLogger : ExperimentLogger {
    override fun d(message: String) {
        LogBuffer.i(ExperimentProvider.TAG, message)
    }

    override fun e(message: String, throwable: Throwable?) {
        throwable?.let { LogBuffer.e(ExperimentProvider.TAG, throwable, message) } ?: LogBuffer.e(ExperimentProvider.TAG, message)
    }
}
