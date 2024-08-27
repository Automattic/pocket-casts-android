package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.nova.ExternalDataManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.branch.engage.conduit.source.BranchDynamicData
import io.branch.engage.conduit.source.CatalogSubmission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class NovaLauncherQueueSync(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val manager: ExternalDataManager,
) {
    private val catalogFactory = CatalogFactory(context)

    private var syncJob: Job? = null

    fun keepQueueInSync() {
        if (syncJob != null) {
            return
        }
        syncJob = manager.observeUpNextQueue(limit = 25)
            .distinctUntilChanged()
            .onEach { episodes ->
                try {
                    val queuedEpisodes = catalogFactory.queuedEpisodes(episodes)
                    val catalogSubmission = CatalogSubmission().setQueue(queuedEpisodes)

                    val isDataSubmitted = BranchDynamicData.getOrInit(context).submit(catalogSubmission).isSuccess
                    val submissionResult = SubmissionResult("Up Next episodes", queuedEpisodes.size)

                    logInfo("Nova Launcher queue sync complete. ${if (isDataSubmitted) "Success" else "Failure"}: $submissionResult")
                } catch (e: Throwable) {
                    logError("Nova Launcher queue sync failed", e)
                }
            }
            .launchIn(coroutineScope)
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }

    private fun logInfo(message: String) = LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, message)

    private fun logError(message: String, throwable: Throwable) = LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, message)
}
