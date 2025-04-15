package au.com.shiftyjelly.pocketcasts.repositories.opml

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.di.Downloads
import au.com.shiftyjelly.pocketcasts.servers.refresh.ImportOpmlResponse
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServiceManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Source
import okio.source
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager as OnboardingNotificationManager

@HiltWorker
class OpmlImportTask @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val podcastManager: PodcastManager,
    private val refreshServiceManager: RefreshServiceManager,
    @Downloads private val httpClient: OkHttpClient,
    private val notificationHelper: NotificationHelper,
    private val analyticsTracker: AnalyticsTracker,
    private val onboardingNotificationManager: OnboardingNotificationManager,
) : CoroutineWorker(context, parameters) {

    companion object {
        private const val INPUT_URI = "INPUT_URI"
        private const val INPUT_URL = "INPUT_URL"
        private const val WORKER_TAG = "OpmlImportTask.Tag"

        fun run(uri: Uri, context: Context) {
            val data = workDataOf(INPUT_URI to uri.toString())
            run(data, context)
        }

        fun run(url: HttpUrl, context: Context) {
            val data = workDataOf(INPUT_URL to url.toString())
            run(data, context)
        }

        fun workInfos(context: Context) = WorkManager.getInstance(context).getWorkInfosByTagFlow(WORKER_TAG)

        private fun run(data: Data, context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val task = OneTimeWorkRequestBuilder<OpmlImportTask>()
                .setInputData(data)
                .setConstraints(constraints)
                .addTag(WORKER_TAG)
                .build()

            WorkManager.getInstance(context).enqueue(task)

            Toast.makeText(context, context.getString(LR.string.settings_import_opml_toast), Toast.LENGTH_LONG).show()
        }
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        try {
            onboardingNotificationManager.updateUserFeatureInteraction(OnboardingNotificationType.Import)
            analyticsTracker.track(AnalyticsEvent.OPML_IMPORT_STARTED)
            val url = inputData.getString(INPUT_URL)?.toHttpUrlOrNull()
            val uri = inputData.getString(INPUT_URI)?.toUri()
            val source = when {
                url != null -> createUrlOpmlSource(url)
                uri != null -> createUriOpmlSource(uri)
                else -> {
                    trackFailure(reason = "no_input_found")
                    null
                }
            }
            if (source == null) {
                return Result.failure()
            }

            val urls = OpmlUrlReader().readUrls(source)
            processUrls(urls)
            trackProcessed(urls.size)

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(applicationContext, applicationContext.getString(LR.string.settings_import_opml_succeeded_message), Toast.LENGTH_LONG).show()
            }
            return Result.success()
        } catch (t: Throwable) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(applicationContext, applicationContext.getString(LR.string.settings_import_opml_import_failed_message), Toast.LENGTH_LONG).show()
            }
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, t, "OPML import failed.")
            trackFailure(reason = "unknown")
            return Result.failure()
        }
    }

    private fun createUrlOpmlSource(url: HttpUrl): Source? {
        val request = Request.Builder().url(url).build()
        return httpClient.newCall(request).execute().body?.source()
    }

    private fun createUriOpmlSource(uri: Uri): Source? {
        return applicationContext.contentResolver.openInputStream(uri)?.source()
    }

    private fun trackProcessed(numberParsed: Int) {
        analyticsTracker.track(
            AnalyticsEvent.OPML_IMPORT_FINISHED,
            mapOf("number" to numberParsed),
        )
    }

    private fun trackFailure(reason: String) {
        analyticsTracker.track(
            AnalyticsEvent.OPML_IMPORT_FAILED,
            mapOf("reason" to reason),
        )
    }

    private suspend fun processUrls(urls: List<String>) {
        val podcastCount = urls.size
        val initialDatabaseCount = podcastManager.countPodcastsBlocking()

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
     * Keep the job in the foreground with a notification
     */
    private fun createForegroundInfo(progress: Int, total: Int): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                Settings.NotificationId.OPML.value,
                buildNotification(progress, total),
                FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(Settings.NotificationId.OPML.value, buildNotification(progress, total))
        }
    }

    private fun updateNotification(initialDatabaseCount: Int, podcastCount: Int) {
        val databaseCount = podcastManager.countPodcastsBlocking()
        val progress = (databaseCount - initialDatabaseCount).coerceIn(0, podcastCount)
        notificationManager.notify(Settings.NotificationId.OPML.value, buildNotification(progress, podcastCount))
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
        val response = refreshServiceManager.importOpml(urls)
        return response.body()?.result
    }

    suspend fun pollServer(pollUuids: List<String>): ImportOpmlResponse? {
        val response = refreshServiceManager.pollImportOpml(pollUuids)
        return response.body()?.result
    }
}

private fun Flow<ImportOpmlResponse>.subscribeToPodcasts(podcastManager: PodcastManager): Flow<ImportOpmlResponse> {
    return onEach {
        // add podcast uuid to subscribe queue
        it.uuids.forEach { uuid -> podcastManager.subscribeToPodcast(uuid, sync = true, shouldAutoDownload = false) }
    }
}

private suspend fun Flow<ImportOpmlResponse>.collectPollUuids(): List<String> {
    return map { it.pollUuids }
        .toList()
        .flatten()
}
