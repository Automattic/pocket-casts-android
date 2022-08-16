package au.com.shiftyjelly.pocketcasts.repositories.opml

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.refresh.ImportOpmlResponse
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServerManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltWorker
class OpmlImportTask @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted val parameters: WorkerParameters,
    var podcastManager: PodcastManager,
    var refreshServerManager: RefreshServerManager,
    var notificationHelper: NotificationHelper
) : CoroutineWorker(context, parameters) {

    companion object {
        const val INPUT_URI = "INPUT_URI"

        fun run(uri: Uri, context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(INPUT_URI to uri.toString())
            val task = OneTimeWorkRequestBuilder<OpmlImportTask>()
                .setInputData(data)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(task)

            Toast.makeText(context, context.getString(LR.string.settings_import_opml_toast), Toast.LENGTH_LONG).show()
        }
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        try {
            val uri = Uri.parse(inputData.getString(INPUT_URI)) ?: return Result.failure()
            applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                processFile(inputStream)
            }
            return Result.success()
        } catch (t: Throwable) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, t, "OPML import failed.")
            return Result.failure()
        }
    }

    private suspend fun processFile(inputStream: InputStream) {
        val urls = readOpmlFeedUrls(inputStream)

        val podcastCount = urls.size
        val initialDatabaseCount = podcastManager.countPodcasts()

        // keep the job running the in foreground with a notification
        setForeground(createForegroundInfo(0, podcastCount))

        var pollUuids = urls.chunked(100)
            .asFlow()
            // call the server with the feed urls to get the podcast uuids
            .mapNotNull { callServer(urls = it) }
            // use the podcast uuids to subscribe to the podcasts
            .subscribeToPodcasts(podcastManager)
            // update the notification progress
            .onEach { updateNotification(initialDatabaseCount, podcastCount) }
            .collectPollUuids()

        var pollCount = 0
        while (pollUuids.isNotEmpty() && pollCount <= 20) {
            pollUuids = pollUuids.chunked(100)
                .asFlow()
                // poll the server with the create uuids to get the podcast uuids
                .mapNotNull { pollServer(pollUuids = it) }
                // use the podcast uuids to subscribe to the podcasts
                .subscribeToPodcasts(podcastManager)
                // update the notification progress
                .onEach { updateNotification(initialDatabaseCount, podcastCount) }
                .collectPollUuids()

            pollCount++
            delay(pollCount * 1000L)
        }

        // keep the job running while still subscribing to the podcasts
        while (podcastManager.isSubscribingToPodcasts()) {
            updateNotification(initialDatabaseCount, podcastCount)
            delay(1000)
        }
    }

    /**
     * Extract the feed urls from an OPML file.
     */
    private fun readOpmlFeedUrls(inputStream: InputStream): List<String> {
        val parser = SAXParserFactory.newInstance().newSAXParser()
        val urls = mutableListOf<String>()
        parser.parse(
            inputStream,
            object : DefaultHandler() {
                override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                    if (localName.equals("outline", ignoreCase = true)) {
                        val url = attributes?.getValue("xmlUrl")
                        if (!url.isNullOrBlank()) {
                            urls.add(url)
                        }
                    }
                }
            }
        )
        return urls
    }

    /**
     * Keep the job in the foreground with a notification
     */
    private fun createForegroundInfo(progress: Int, total: Int): ForegroundInfo {
        return ForegroundInfo(NotificationHelper.NOTIFICATION_ID_OPML, buildNotification(progress, total))
    }

    private fun updateNotification(initialDatabaseCount: Int, podcastCount: Int) {
        val databaseCount = podcastManager.countPodcasts()
        val progress = (databaseCount - initialDatabaseCount).coerceIn(0, podcastCount)
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID_OPML, buildNotification(progress, podcastCount))
    }

    private fun buildNotification(progress: Int, total: Int): Notification {
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        return notificationHelper.podcastImportChannelBuilder()
            .setContentTitle(applicationContext.getString(LR.string.settings_import_opml_title))
            .setContentText(applicationContext.getString(LR.string.settings_import_opml_progress, progress, total))
            .setProgress(total, progress, false)
            .setSmallIcon(IR.drawable.notification_download)
            .setOngoing(true)
            .addAction(IR.drawable.ic_cancel, applicationContext.getString(LR.string.settings_import_opml_stop), cancelIntent)
            .build()
    }

    /**
     * Call the refresh server with feed urls.
     * The server will return the follow:
     * - uuids: found podcast uuids
     * - poll_uuids: the create podcast ids to call the server back with to check if they have been added to the database yet
     * - failed: the number of podcast creates that have failed
     */
    suspend fun callServer(urls: List<String>): ImportOpmlResponse? {
        val response = refreshServerManager.importOpml(urls)
        return response.body()?.result
    }

    suspend fun pollServer(pollUuids: List<String>): ImportOpmlResponse? {
        val response = refreshServerManager.pollImportOpml(pollUuids)
        return response.body()?.result
    }
}

private fun Flow<ImportOpmlResponse>.subscribeToPodcasts(podcastManager: PodcastManager): Flow<ImportOpmlResponse> {
    return onEach {
        // add podcast uuid to subscribe queue
        it.uuids.forEach { uuid -> podcastManager.subscribeToPodcast(uuid, true) }
    }
}

private suspend fun Flow<ImportOpmlResponse>.collectPollUuids(): List<String> {
    return map { it.pollUuids }
        .toList()
        .flatten()
}
