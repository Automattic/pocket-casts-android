package au.com.shiftyjelly.pocketcasts.repositories.support

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.toPublisher
import androidx.work.WorkManager
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageOptions
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.SystemBatteryRestrictions
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jaredrummler.android.device.DeviceName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class Support @Inject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
    private val fileStorage: FileStorage,
    private val upNextQueue: UpNextQueue,
    private val subscriptionManager: SubscriptionManager,
    private val systemBatteryRestrictions: SystemBatteryRestrictions,
    private val syncManager: SyncManager,
    @ApplicationContext private val context: Context
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    val afterPlayingValues
        get() = context.resources.getStringArray(R.array.settings_auto_archive_played_values)
    val inactiveValues
        get() = context.resources.getStringArray(R.array.settings_auto_archive_inactive_values)

    @Suppress("DEPRECATION")
    suspend fun sendEmail(subject: String, intro: String, context: Context): Intent {
        val dialog = withContext(Dispatchers.Main) {
            android.app.ProgressDialog.show(context, context.getString(R.string.loading), context.getString(R.string.settings_support_please_wait), true)
        }
        val intent = Intent(Intent.ACTION_SEND)

        withContext(Dispatchers.IO) {
            intent.type = "text/html"
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@pocketcasts.com"))
            val isPlus = subscriptionManager.getCachedStatus() is SubscriptionStatus.Plus
            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "$subject v${settings.getVersion()} ${if (isPlus) " - Plus Account" else ""}"
            )

            // try to attach the debug information
            try {
                val emailFolder = File(context.filesDir, "email")
                emailFolder.mkdirs()
                val debugFile = File(emailFolder, "debug.txt")

                FileOutputStream(debugFile).use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { out ->
                        out.write(getUserDebug(false))
                        out.write("\n\n")
                        out.flush()
                        LogBuffer.output(outputStream)
                        outputStream.flush()
                        out.close()

                        val fileUri =
                            FileUtil.createUriWithReadPermissions(debugFile, intent, context)
                        intent.putExtra(Intent.EXTRA_STREAM, fileUri)
                        intent.putExtra(
                            Intent.EXTRA_TEXT,
                            HtmlCompat.fromHtml(
                                "$intro<br/><br/>",
                                HtmlCompat.FROM_HTML_MODE_COMPACT
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)

                val debugStr = StringBuilder()
                debugStr.append(intro)
                debugStr.append("<br/><br/><br/><br/><br/><br/><br/>")
                debugStr.append(getUserDebug(true))

                intent.putExtra(
                    Intent.EXTRA_TEXT,
                    HtmlCompat.fromHtml(debugStr.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT)
                )
            }
        }

        dialog.dismiss()

        return intent
    }

    @Suppress("DEPRECATION")
    private suspend fun getUserDebug(html: Boolean): String {
        val output = StringBuilder()
        try {
            val eol = if (html) "<br/>" else "\n"
            output.append("App version : ").append(settings.getVersion()).append(" (").append(settings.getVersionCode()).append(")").append(eol)
            output.append("Sync account: ").append(if (syncManager.isLoggedIn()) syncManager.getEmail() else "Not logged in").append(eol)
            if (syncManager.isLoggedIn()) {
                output.append("Last Sync: ").append(settings.getLastModified() ?: "Never").append(eol)
            }
            val now = Date()
            val localDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val utcDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            output.append("Time: ").append(localDateFormatter.format(now)).append(" Local").append(eol)
            output.append("      ").append(utcDateFormatter.format(now)).append(" Utc").append(eol)

            output.append(eol)

            output.append("Phone: ").append(Build.MANUFACTURER).append(" - ").append(getDeviceName()).append(" - ").append(Build.MODEL).append(" - ").append(Build.DEVICE).append(eol)
            output.append("Kernel: ").append(System.getProperty("os.version")).append(" - ").append(Build.HOST).append(eol)
            output.append("Android version: ").append(Build.VERSION.RELEASE).append(" SDK ").append(Build.VERSION.SDK_INT).append(eol)
            output.append(eol)

            output.append("Background refresh: ").append(settings.refreshPodcastsAutomatically()).append(eol)
            output.append("Battery restriction: ${systemBatteryRestrictions.status}")
            output.append(eol)

            if (Build.VERSION.SDK_INT >= 30) {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 0)
                if (exitReasons.isNotEmpty()) {
                    output.append("Exit reasons").append(eol)
                    exitReasons.forEach {
                        if (it.reason != ApplicationExitInfo.REASON_USER_REQUESTED && it.reason != ApplicationExitInfo.REASON_USER_STOPPED) {
                            output.append(it.toString()).append(eol)
                        }
                    }
                    output.append(eol)
                }
            }

            val podcastsOutput = StringBuilder()
            podcastsOutput.append("Podcasts").append(eol).append("--------").append(eol).append(eol)
            val autoDownloadOn = booleanArrayOf(false)
            val uuidToPodcast = HashMap<String, Podcast>()
            try {
                val podcasts = podcastManager.findSubscribed()
                for (podcast in podcasts) {
                    if (podcast.isAutoDownloadNewEpisodes) {
                        autoDownloadOn[0] = true
                    }
                    podcastsOutput.append(podcast.uuid).append(eol)
                    podcastsOutput.append(if (podcast.title.isEmpty()) "-" else podcast.title).append(eol)
                    podcastsOutput.append("Last episode: ").append(podcast.latestEpisodeUuid).append(eol)
                    val effects = podcast.playbackEffects
                    podcastsOutput.append("Audio effects: ").append(if (podcast.overrideGlobalEffects) "on" else "off").append(" Silence removed: ").append(effects.trimMode).append(" Speed: ").append(effects.playbackSpeed).append(" Boost: ")
                        .append(if (effects.isVolumeBoosted) "on" else "off").append(eol)
                    podcastsOutput.append("Auto download? ").append(podcast.isAutoDownloadNewEpisodes).append(eol)
                    podcastsOutput.append("Custom auto archive: ").append(podcast.overrideGlobalArchive.toString()).append(eol)
                    if (podcast.overrideGlobalArchive) {
                        podcastsOutput.append("Episode limit: ").append(podcast.autoArchiveEpisodeLimit).append(eol)
                        podcastsOutput.append("Archive after playing: ").append(afterPlayingValues[podcast.autoArchiveAfterPlaying]).append(eol)
                        podcastsOutput.append("Archive inactive: ").append(inactiveValues[podcast.autoArchiveInactive]).append(eol)
                    }
                    podcastsOutput.append("Auto add to up next: ").append(autoAddToUpNextToString(podcast.autoAddToUpNext)).append(eol)
                    podcastsOutput.append(eol)

                    uuidToPodcast[podcast.uuid] = podcast
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            val afterPlaying = context.resources.getStringArray(R.array.settings_auto_archive_played_values)
            val inactive = context.resources.getStringArray(R.array.settings_auto_archive_inactive_values)
            output.append(eol)
            output.append("Auto archive settings").append(eol)
            output.append("Auto archive played episodes after: " + afterPlaying[settings.getAutoArchiveAfterPlaying().toIndex()]).append(eol)
            output.append("Auto archive inactive episodes after: " + inactive[settings.getAutoArchiveInactive().toIndex()]).append(eol)
            output.append("Auto archive starred episodes: " + settings.getAutoArchiveIncludeStarred().toString()).append(eol)

            output.append(eol)
            output.append("Auto downloads").append(eol)
            output.append("  Any podcast? ").append(yesNoString(autoDownloadOn[0])).append(eol)
            output.append("  Up Next? ").append(yesNoString(settings.isUpNextAutoDownloaded())).append(eol)
            output.append("  Only on unmetered WiFi? ").append(yesNoString(settings.isPodcastAutoDownloadUnmeteredOnly())).append(eol)
            output.append("  Only when charging? ").append(yesNoString(settings.isPodcastAutoDownloadPowerOnly())).append(eol)
            output.append(eol)

            output.append("Current connection").append(eol)
            output.append("  Type: ").append(Network.getConnectedNetworkTypeName(context).lowercase()).append(eol)
            output.append("  Metered? ").append(if (Network.isActiveNetworkMetered(context)) "yes (costs money)" else "no (unlimited, free)").append(eol)
            output.append("  Restrict Background Status: ").append(Network.getRestrictBackgroundStatusString(context)).append(eol)
            output.append(eol)

            output.append("Warning when not on Wifi? ").append(yesNoString(settings.warnOnMeteredNetwork())).append(eol)
            output.append(eol)

            output.append("Work Manager Tasks").append(eol)
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosByTagLiveData(DownloadManager.WORK_MANAGER_DOWNLOAD_TAG)
                .toPublisher(ProcessLifecycleOwner.get())
                .awaitFirst()
            workInfos.forEach { workInfo ->
                output.append(workInfo.toString()).append(" Attempt=").append(workInfo.runAttemptCount).append(eol)
            }
            output.append(eol)

            output.append("Launcher: ")
            try {
                val mainIntent = Intent(Intent.ACTION_MAIN, null)
                mainIntent.addCategory(Intent.CATEGORY_HOME)
                val launcherApps = context.packageManager.queryIntentActivities(mainIntent, 0)
                for (launcherApp in launcherApps) {
                    output.append(launcherApp.loadLabel(context.packageManager)).append(", ")
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            output.append(eol)

            val metrics = DisplayMetrics()
            val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            if (display != null) {
                val width = display.width
                val height = display.height
                display.getMetrics(metrics)

                output.append("Screen: ").append(width).append("x").append(height).append(", dpi: ").append(metrics.densityDpi).append(", density: ").append(metrics.density).append(eol)
            }

            val storageFolder = fileStorage.storageDirectory.absolutePath
            output.append("Storage: ").append(if (settings.usingCustomFolderStorage()) "Custom Folder" else settings.getStorageChoiceName()).append(", ").append(storageFolder).append(eol)
            output.append("Storage options:").append(eol)
            val storageOptions = StorageOptions()
            for (folderLocation in storageOptions.getFolderLocations(context)) {
                if (storageFolder == folderLocation.filePath) {
                    continue
                }
                output.append(folderLocation.label).append(", ").append(folderLocation.filePath).append(eol)
            }
            output.append("Database: " + Util.formattedBytes(context.getDatabasePath("pocketcasts").length(), context = context)).append(eol)
            output.append("Temp directory: " + Util.formattedBytes(FileUtil.folderSize(fileStorage.tempPodcastDirectory), context = context)).append(eol)
            output.append("Podcast directory: " + Util.formattedBytes(FileUtil.folderSize(fileStorage.podcastDirectory), context = context)).append(eol)
            output.append("Network image directory: " + Util.formattedBytes(FileUtil.folderSize(fileStorage.networkImageDirectory), context = context)).append(eol)

            if (!html) {
                output.append(eol)

                output.append("Player").append(eol)
                val queue = upNextQueue.queueEpisodes
                if (queue.isEmpty()) {
                    output.append("Up Next queue is empty.").append(eol)
                } else {
                    val episode = queue.first()
                    output.append("Episode: ").append(episode.title).append(eol)
                    if (episode is PodcastEpisode) {
                        output.append("Podcast: ").append(episode.podcastUuid).append(eol)
                    } else {
                        output.append("Cloud File")
                    }
                }
                output.append(eol)

                output.append("Notifications").append(eol)
                output.append("Play over notifications? ").append(if (settings.canDuckAudioWithNotifications()) "yes" else "no").append(eol)
                output.append("Hide notification on pause? ").append(if (settings.hideNotificationOnPause()) "yes" else "no").append(eol)
                output.append(eol)

                val effects = settings.getGlobalPlaybackEffects()
                output.append("Effects").append(eol)
                output.append("Global Audio effects: ")
                    .append(" Playback speed: ").append(effects.playbackSpeed).append(eol)
                    .append(" Silence removed: ").append(effects.trimMode).append(eol)
                    .append(" Volume boost: ").append(if (effects.isVolumeBoosted) "on" else "off").append(eol).append(eol)

                output.append("Database").append(eol)
                    .append(" ").append(podcastManager.countPodcasts()).append(" Podcasts ").append(eol)
                    .append(" ").append(episodeManager.countEpisodes()).append(" Episodes ").append(eol)
                    .append(" ").append(playlistManager.findAll().size).append(" Playlists ").append(eol)
                    .append(" ").append(queue.size).append(" Up Next ").append(eol).append(eol)

                output.append(podcastsOutput.toString())

                output.append("Filters").append(eol).append("-------").append(eol).append(eol)

                try {
                    val playlists = playlistManager.findAll()
                    for (playlist in playlists) {
                        output.append(playlist.title).append(eol)
                        output.append("Auto Download? ").append(playlist.autoDownload).append(" Unmetered only? ").append(playlist.autoDownloadUnmeteredOnly).append(" Power only? ").append(playlist.autoDownloadPowerOnly).append(eol)
                        output.append(eol)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }

                output.append("Episode Issues").append(eol).append("--------------").append(eol).append(eol)

                try {
                    val episodes = episodeManager.findEpisodesWhere("downloaded_error_details IS NOT NULL AND LENGTH(downloaded_error_details) > 0 LIMIT 100")
                    for (episode in episodes) {
                        output.append("Title: ").append(episode.title).append(eol)
                        output.append("Id: ").append(episode.uuid).append(eol)
                        output.append("Error: ").append(if (episode.downloadErrorDetails == null) "-" else episode.downloadErrorDetails).append(eol)
                        output.append("Url: ").append(if (episode.downloadUrl == null) "-" else episode.downloadUrl).append(eol)
                        val podcast = uuidToPodcast[episode.podcastUuid]
                        output.append("Podcast: ").append(episode.podcastUuid).append(" ").append(podcast?.title ?: "-").append(eol)
                        output.append(eol)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        } catch (e: Exception) {
            output.append("Unable to report all user debug info due to an exception. ").append(e.message)
        }

        return output.toString()
    }

    private fun autoAddToUpNextToString(autoAddToUpNext: Podcast.AutoAddUpNext): String {
        return when (autoAddToUpNext) {
            Podcast.AutoAddUpNext.OFF -> "off"
            Podcast.AutoAddUpNext.PLAY_NEXT -> "to top (play next)"
            Podcast.AutoAddUpNext.PLAY_LAST -> "to bottom (play last)"
            else -> "unknown value $autoAddToUpNext"
        }
    }

    private fun yesNoString(value: Boolean): String {
        return if (value) "yes" else "no"
    }

    private fun getDeviceName(): String {
        try {
            return DeviceName.getDeviceName()
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve device name")
            return ""
        }
    }
}
