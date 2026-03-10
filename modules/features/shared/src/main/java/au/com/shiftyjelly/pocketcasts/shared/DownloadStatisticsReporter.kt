package au.com.shiftyjelly.pocketcasts.shared

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.repositories.di.ProcessLifecycle
import com.automattic.eventhorizon.EpisodeDownloadsStaleEvent
import com.automattic.eventhorizon.EventHorizon
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class DownloadStatisticsReporter @Inject constructor(
    private val episodeDao: EpisodeDao,
    private val eventHorizon: EventHorizon,
    @ProcessLifecycle private val lifecycleOwner: LifecycleOwner,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) {
    private val isSetupUpForReporting = AtomicBoolean()

    fun setup() {
        if (isSetupUpForReporting.getAndSet(true)) {
            return
        }
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                lifecycleOwner.lifecycle.removeObserver(this)
                reportStatistics()
            }
        })
    }

    private fun reportStatistics() {
        coroutineScope.launch {
            val statistics = episodeDao.getFailedDownloadsStatistics()
            eventHorizon.track(
                EpisodeDownloadsStaleEvent(
                    failedDownloadCount = statistics.count,
                    newestFailedDownload = statistics.newestTimestamp?.toString(),
                    oldestFailedDownload = statistics.oldestTimestamp?.toString(),
                ),
            )
        }
    }
}
