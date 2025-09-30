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
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast.Companion.AUTO_DOWNLOAD_NEW_EPISODES
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageOptions
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.SystemBatteryRestrictions
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jaredrummler.android.device.DeviceName
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class Support @Inject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
    private val fileStorage: FileStorage,
    private val upNextQueue: UpNextQueue,
    private val systemBatteryRestrictions: SystemBatteryRestrictions,
    private val syncManager: SyncManager,
    @ApplicationContext private val context: Context,
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    @Suppress("DEPRECATION")
    suspend fun shareLogs(subject: String, intro: String, emailSupport: Boolean, context: Context): Intent {
        val dialog = withContext(Dispatchers.Main) {
            android.app.ProgressDialog.show(context, context.getString(R.string.loading), context.getString(R.string.settings_support_please_wait), true)
        }
        val intent = Intent(Intent.ACTION_SEND)

        withContext(Dispatchers.IO) {
            intent.type = "message/rfc822"
            if (emailSupport) {
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@pocketcasts.com"))
            }
            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "$subject v${settings.getVersion()} ${getAccountType()}",
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
                            FileUtil.createUriWithReadPermissions(context, debugFile, intent)
                        intent.putExtra(Intent.EXTRA_STREAM, fileUri)
                        intent.putExtra(
                            Intent.EXTRA_TEXT,
                            HtmlCompat.fromHtml(
                                "$intro<br/><br/>",
                                HtmlCompat.FROM_HTML_MODE_COMPACT,
                            ),
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
                    HtmlCompat.fromHtml(debugStr.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT),
                )
            }
        }

        dialog.dismiss()

        return intent
    }

    suspend fun emailWearLogsToSupportIntent(logBytes: ByteArray, context: Context): Intent {
        val subject = "Android wear support"
        val intro = "Hi there, just needed help with something..."
        val intent = Intent(Intent.ACTION_SEND)

        withContext(Dispatchers.IO) {
            intent.type = "text/html"
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@pocketcasts.com"))
            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "$subject v${settings.getVersion()} ${getAccountType()}",
            )

            try {
                val emailFolder = File(context.filesDir, "email")
                emailFolder.mkdirs()
                val debugFile = File(emailFolder, "debug_wear.txt")

                debugFile.writeBytes(logBytes)
                val fileUri =
                    FileUtil.createUriWithReadPermissions(context, debugFile, intent)
                intent.putExtra(Intent.EXTRA_STREAM, fileUri)
                intent.putExtra(
                    Intent.EXTRA_TEXT,
                    HtmlCompat.fromHtml(
                        "$intro<br/><br/>",
                        HtmlCompat.FROM_HTML_MODE_COMPACT,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e)

                val debugStr = buildString {
                    append(intro)
                    append("<br/><br/><br/><br/><br/><br/><br/>")
                    append(String(logBytes))
                }

                intent.putExtra(Intent.EXTRA_TEXT, debugStr)
            }
        }

        return intent
    }

    private fun getAccountType() = when (settings.cachedSubscription.value?.tier) {
        SubscriptionTier.Patron -> "Patron Account"
        SubscriptionTier.Plus -> "Plus Account"
        null -> ""
    }

    suspend fun getLogs(): String = withContext(Dispatchers.IO) {
        buildString {
            append(getUserDebug(false))
            val outputStream = ByteArrayOutputStream()
            LogBuffer.output(outputStream)
            append(outputStream.toString())
        }
    }

    @Suppress("DEPRECATION")
    suspend fun getUserDebug(html: Boolean): String {
        val output = StringBuilder()
        try {
            val eol = if (html) "<br/>" else "\n"
            output.append("Platform: ").append(Util.getAppPlatform(context)).append(eol)
            output.append("App version: ").append(settings.getVersion()).append(" (").append(settings.getVersionCode()).append(")").append(eol)
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

            output.append("Background refresh: ").append(settings.backgroundRefreshPodcasts.value).append(eol)
            output.append("Battery restriction: ${systemBatteryRestrictions.status}").append(eol)
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

            val features = Feature.entries.map { "${it.key}: ${FeatureFlag.isEnabled(it)}" }
            output.append("Feature flags").append(eol)
            features.forEach { output.append("  ").append(it).append(eol) }

            val podcastsOutput = StringBuilder()
            podcastsOutput.append("Podcasts").append(eol).append("--------").append(eol).append(eol)
            val uuidToPodcast = HashMap<String, Podcast>()
            val hasAnyPodcastWithAddUpNext = try {
                val podcasts = podcastManager.findSubscribedBlocking()
                val upNextSettings = Array(podcasts.size) { false }
                podcasts.forEachIndexed { index, podcast ->
                    podcastsOutput.append(podcast.uuid).append(eol)
                    podcastsOutput.append(if (podcast.title.isEmpty()) "-" else podcast.title).append(eol)
                    podcastsOutput.append("Last episode: ").append(podcast.latestEpisodeUuid).append(eol)
                    val effects = podcast.playbackEffects
                    podcastsOutput.append("Audio effects: ").append(if (podcast.overrideGlobalEffects) "on" else "off")
                        .append(" Silence removed: ").append(effects.trimMode.toString().lowercase())
                        .append(" Speed: ").append(effects.playbackSpeed)
                        .append(" Boost: ").append(if (effects.isVolumeBoosted) "on" else "off")
                        .append(eol)
                    podcastsOutput.append("Auto download? ").append(podcast.isAutoDownloadNewEpisodes).append(eol)
                    podcastsOutput.append("Custom auto archive: ").append(podcast.overrideGlobalArchive.toString()).append(eol)
                    if (podcast.overrideGlobalArchive) {
                        podcastsOutput.append("Episode limit: ").append(podcast.autoArchiveEpisodeLimit).append(eol)
                        podcastsOutput.append("Archive after playing: ").append(podcast.autoArchiveAfterPlaying?.analyticsValue).append(eol)
                        podcastsOutput.append("Archive inactive: ").append(podcast.autoArchiveInactive?.analyticsValue).append(eol)
                    }
                    podcastsOutput.append("Auto add to up next: ").append(autoAddToUpNextToString(podcast.autoAddToUpNext)).append(eol)
                    podcastsOutput.append(eol)

                    uuidToPodcast[podcast.uuid] = podcast
                    upNextSettings[index] = podcast.autoAddToUpNext != Podcast.AutoAddUpNext.OFF
                }
                upNextSettings.any { it }
            } catch (e: Exception) {
                Timber.e(e)
                false
            }
            val isAutoDownloadEnabled = podcastManager.hasEpisodesWithAutoDownloadStatus(AUTO_DOWNLOAD_NEW_EPISODES)

            output.append(eol)
            output.append("Auto archive settings").append(eol)
            output.append("  Auto archive played episodes after: ${settings.autoArchiveAfterPlaying.value.analyticsValue}").append(eol)
            output.append("  Auto archive inactive episodes after: ${settings.autoArchiveInactive.value.analyticsValue}").append(eol)
            output.append("  Auto archive starred episodes: ${settings.autoArchiveIncludesStarred.value}").append(eol)

            output.append(eol)
            output.append("Auto downloads").append(eol)
            output.append("  Any podcast? ").append(yesNoString(isAutoDownloadEnabled)).append(eol)
            output.append("  New episodes? ").append(yesNoString(settings.autoDownloadNewEpisodes.value == AUTO_DOWNLOAD_NEW_EPISODES)).append(eol)
            output.append("  On follow? ").append(yesNoString(settings.autoDownloadOnFollowPodcast.value)).append(eol)
            output.append("  Limit downloads: ").append(settings.autoDownloadLimit.value).append(eol)
            output.append("  Any podcast new episode up next? ").append(yesNoString(hasAnyPodcastWithAddUpNext)).append(eol)
            output.append("  Up Next? ").append(yesNoString(settings.autoDownloadUpNext.value)).append(eol)
            output.append("  Only on unmetered WiFi? ").append(yesNoString(settings.autoDownloadUnmeteredOnly.value)).append(eol)
            output.append("  Only when charging? ").append(yesNoString(settings.autoDownloadOnlyWhenCharging.value)).append(eol)

            output.append(eol)
            output.append("Auto-play settings").append(eol)
            output.append("  Is enabled? ${settings.autoPlayNextEpisodeOnEmpty.value}").append(eol)
            output.append("  Latest source: ${settings.lastAutoPlaySource.value.prettyPrint(podcastManager, playlistManager)}").append(eol)

            output.append(eol)
            output.append("Current connection").append(eol)
            output.append("  Type: ").append(Network.getConnectedNetworkTypeName(context).lowercase()).append(eol)
            output.append("  Metered? ").append(if (Network.isActiveNetworkMetered(context)) "yes (costs money)" else "no (unlimited, free)").append(eol)
            output.append("  Restrict Background Status: ").append(Network.getRestrictBackgroundStatusString(context)).append(eol)

            output.append(eol)
            output.append("Warning when not on Wifi? ").append(yesNoString(settings.warnOnMeteredNetwork.value)).append(eol)

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

            try {
                val storageFolder = fileStorage.getOrCreateStorageDir()?.absolutePath
                output.append("Storage: ").append(if (settings.usingCustomFolderStorage()) "Custom Folder" else settings.getStorageChoiceName()).append(", ").append(storageFolder).append(eol)
                output.append("Storage options:").append(eol)
                val storageOptions = StorageOptions()
                for (folderLocation in storageOptions.getFolderLocations(context)) {
                    if (storageFolder == folderLocation.filePath) {
                        continue
                    }
                    output.append(folderLocation.label).append(", ").append(folderLocation.filePath).append(eol)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
            output.append("Database: " + Util.formattedBytes(context.getDatabasePath("pocketcasts").length(), context = context)).append(eol)
            try {
                output.append("Temp directory: " + Util.formattedBytes(FileUtil.dirSize(fileStorage.getOrCreateEpisodesTempDir()), context)).append(eol)
                output.append("Podcast directory: " + Util.formattedBytes(fileStorage.getOrCreateEpisodesDir()?.let(FileUtil::dirSize) ?: 0, context)).append(eol)
                output.append("Network image directory: " + Util.formattedBytes(fileStorage.getOrCreateNetworkImagesDir()?.let(FileUtil::dirSize) ?: 0, context)).append(eol)
            } catch (e: Exception) {
                Timber.e(e)
            }

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
                output.append("Play over notifications? ").append(settings.playOverNotification.value.analyticsString).append(eol)
                output.append("Hide notification on pause? ").append(if (settings.hideNotificationOnPause.value) "yes" else "no").append(eol)
                output.append(eol)

                val effects = settings.globalPlaybackEffects.value
                output.append("Effects").append(eol)
                output.append("Global Audio effects: ")
                    .append(" Playback speed: ").append(effects.playbackSpeed).append(eol)
                    .append(" Silence removed: ").append(effects.trimMode.toString().lowercase()).append(eol)
                    .append(" Volume boost: ").append(if (effects.isVolumeBoosted) "on" else "off").append(eol).append(eol)

                output.append("Database").append(eol)
                    .append(" ").append(podcastManager.countPodcastsBlocking()).append(" Podcasts ").append(eol)
                    .append(" ").append(episodeManager.countEpisodes()).append(" Episodes ").append(eol)
                    .append(" ").append(runBlocking { playlistManager.playlistPreviewsFlow() }.first().size).append(" Playlists ").append(eol)
                    .append(" ").append(queue.size).append(" Up Next ").append(eol).append(eol)

                output.append(podcastsOutput.toString())

                output.append("Filters").append(eol).append("-------").append(eol).append(eol)

                try {
                    val playlists = runBlocking { playlistManager.playlistPreviewsFlow() }.first()
                    for (playlist in playlists) {
                        output.append(playlist.title).append(eol)
                        output.append("Auto Download? ").append(playlist.settings.isAutoDownloadEnabled).append(eol)
                        output.append(eol)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }

                output.append("Advance Settings").append(eol).append("-------------------").append(eol).append(eol)
                output.append("Prioritize seek accuracy? ").append(settings.prioritizeSeekAccuracy.value).append(eol)
                output.append(eol)

                output.append("Episode Issues").append(eol).append("--------------").append(eol).append(eol)

                try {
                    val episodes = episodeManager.findEpisodesWhereBlocking("downloaded_error_details IS NOT NULL AND LENGTH(downloaded_error_details) > 0 LIMIT 100")
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
            Timber.e(e)
            output.append("Unable to report all user debug info due to an exception. ").append(e.message)
        }

        return output.toString()
    }

    private fun autoAddToUpNextToString(autoAddToUpNext: Podcast.AutoAddUpNext): String {
        return when (autoAddToUpNext) {
            Podcast.AutoAddUpNext.OFF -> "off"
            Podcast.AutoAddUpNext.PLAY_NEXT -> "to top (play next)"
            Podcast.AutoAddUpNext.PLAY_LAST -> "to bottom (play last)"
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

private fun AutoPlaySource.prettyPrint(
    podcastManager: PodcastManager,
    playlistManager: PlaylistManager,
): String = when (this) {
    is AutoPlaySource.PodcastOrFilter -> {
        podcastManager.findPodcastByUuidBlocking(uuid)?.let { podcast ->
            return "Podcast / ${podcast.title} / ${podcast.uuid}"
        }
        runBlocking { playlistManager.playlistPreviewsFlow().first() }
            .find { it.uuid == uuid }
            ?.let { filter ->
                return "Filter / ${filter.title} / ${filter.uuid}"
            }
        "Podcast or filter: $uuid"
    }

    AutoPlaySource.Predefined.Downloads -> "Downloads"
    AutoPlaySource.Predefined.Files -> "Files"
    AutoPlaySource.Predefined.Starred -> "Starred"
    AutoPlaySource.Predefined.None -> "None"
}
