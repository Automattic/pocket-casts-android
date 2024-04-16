package au.com.shiftyjelly.pocketcasts.repositories.jobs

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
@SuppressLint("SpecifyJobSchedulerIdRange")
class VersionMigrationsJob : JobService() {

    companion object {
        fun run(podcastManager: PodcastManager, settings: Settings, syncManager: SyncManager, context: Context) {
            runSync(podcastManager, settings, syncManager)
            runAsync(settings, context)
        }

        /**
         * Run short migrations straight away.
         */
        private fun runSync(podcastManager: PodcastManager, settings: Settings, syncManager: SyncManager) {
            performUpdateIfRequired(updateKey = "run_v7_20", settings = settings) {
                // Upgrading to version 7.20.0 requires the folders from the servers to be added to the existing podcasts. In case the user doesn't have internet this is done as part of the regular sync process.
                if (syncManager.isLoggedIn()) {
                    podcastManager.reloadFoldersFromServer()
                }
            }
        }

        private fun performUpdateIfRequired(updateKey: String, settings: Settings, update: () -> Unit) {
            if (settings.getBooleanForKey(key = updateKey, defaultValue = false)) {
                // already performed this update
                return
            }

            update()
            Timber.i("Successfully completed update $updateKey")

            settings.setBooleanForKey(key = updateKey, value = true)
        }

        /**
         * Run longer migrations in the background.
         */
        private fun runAsync(settings: Settings, context: Context) {
            val previousVersionCode = settings.getMigratedVersionCode()
            val versionCode = settings.getVersionCode()

            if (previousVersionCode >= versionCode) {
                Timber.i("No version migration needed.")
                return
            }
            Timber.i("Migrating from version $previousVersionCode to $versionCode")

            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Running VersionMigrationsTask")
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JobIds.VERSION_MIGRATION_JOB_ID)
            jobScheduler.schedule(
                JobInfo.Builder(JobIds.VERSION_MIGRATION_JOB_ID, ComponentName(context, VersionMigrationsJob::class.java))
                    .setOverrideDeadline(500) // don't let Android wait for more than 500ms before kicking this off
                    .build(),
            )
        }
    }

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var episodeManager: EpisodeManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var fileStorage: FileStorage

    @Inject lateinit var appDatabase: AppDatabase

    @Inject lateinit var playbackManager: PlaybackManager

    @Volatile private var shouldKeepRunning = true

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "VersionMigrationsTask onStartJob")

        Thread {
            var shouldReschedule = false
            try {
                if (!shouldKeepRunning) {
                    shouldReschedule = true
                } else {
                    performMigration()
                }
            } catch (t: Throwable) {
                Timber.e(t)
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, t, "VersionMigrationsTask jobFinished failed.")
            } finally {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "VersionMigrationsTask jobFinished shouldReschedule? $shouldReschedule")
                jobFinished(jobParameters, shouldReschedule)
            }
        }.start()

        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "VersionMigrationsTask onStopJob")
        shouldKeepRunning = false

        return false
    }

    private fun performMigration() {
        val previousVersionCode = settings.getMigratedVersionCode()
        val versionCode = settings.getVersionCode()

        settings.setMigratedVersionCode(versionCode)

        // don't migrate when first installing
        if (previousVersionCode == 0) {
            settings.setWhatsNewVersionCode(Settings.WHATS_NEW_VERSION_CODE)
            settings.setSeenPlayerTour(true)
            settings.setSeenUpNextTour(true)
            return
        }

        if (previousVersionCode < 377) {
            deletePodcastImages()
        }
        if (previousVersionCode < 422) {
            addUpNextAutoDownload()
        }
        if (previousVersionCode < 1257) {
            performV7Migration()
        }

        removeCustomEpisodes()
        removeOldTempPodcastDirectory()
        unscheduleEpisodeDetailsJob(applicationContext)
        unscheduleRefreshJob(applicationContext)

        if (previousVersionCode < 6362) {
            upgradeTrimSilenceMode()
        }

        if (previousVersionCode < 9209) {
            consolidateEmbeddedArtworkSettings(applicationContext)
        }
    }

    private fun removeOldTempPodcastDirectory() {
        try {
            fileStorage.getOrCreateEpisodesOldTempDir()?.absolutePath?.let(FileUtil::deleteDirContents)
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Could not clear old podcast temp directory")
        }
    }

    private fun removeCustomEpisodes() {
        val customPodcastUuid = "customFolderPodcast"
        val podcast: Podcast = podcastManager.findPodcastByUuid(customPodcastUuid) ?: return
        val episodes: List<PodcastEpisode> = episodeManager.findEpisodesByPodcastOrderedByPublishDate(podcast)
        episodes.forEach { episode ->
            playbackManager.removeEpisode(
                episodeToRemove = episode,
                source = SourceView.UNKNOWN,
                userInitiated = false,
            )
            appDatabase.episodeDao().delete(episode)
        }
        appDatabase.podcastDao().deleteByUuid(customPodcastUuid)
    }

    private fun performV7Migration() {
        // We want v6 users to keep defaulting to download, new users should get the new stream default
        val currentStreamingPreference = if (settings.contains(Settings.PREFERENCE_GLOBAL_STREAMING_MODE)) settings.streamingMode.value else false
        settings.streamingMode.set(currentStreamingPreference, updateModifiedAt = false)
    }

    private fun addUpNextAutoDownload() {
        settings.autoDownloadUpNext.set(!settings.streamingMode.value, updateModifiedAt = false)
    }

    private fun deletePodcastImages() {
        try {
            val thumbnailsFolder = fileStorage.getOrCreateDir("podcast_thumbnails")
            if (thumbnailsFolder != null && thumbnailsFolder.exists()) {
                thumbnailsFolder.delete()
            }
            val imageFolder = fileStorage.getOrCreateDir("images")
            if (imageFolder != null && imageFolder.exists()) {
                imageFolder.delete()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun unscheduleEpisodeDetailsJob(context: Context) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        jobScheduler.allPendingJobs.forEach { jobInfo ->
            if (jobInfo.service.className.contains("au.com.shiftyjelly.pocketcasts.repositories.download.UpdateEpisodeDetailsJob")) {
                jobScheduler.cancel(jobInfo.id)
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Unscheduling UpdateEpisodeDetailsJob ${jobInfo.id}")
            }
        }
    }

    private fun unscheduleRefreshJob(context: Context) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        jobScheduler.cancel(JobIds.REFRESH_PODCASTS_JOB_ID)
    }

    private fun upgradeTrimSilenceMode() {
        try {
            val podcasts = podcastManager.findSubscribed()
            podcasts.forEach { podcast ->
                if (podcast.isSilenceRemoved && podcast.trimMode == TrimMode.OFF) {
                    podcastManager.updateTrimMode(podcast, TrimMode.LOW)
                }
            }
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Could not migrate trimsilence mode on podcasts")
        }
    }

    private fun consolidateEmbeddedArtworkSettings(context: Context) {
        if (!Util.isWearOs(context) && !Util.isAutomotive(context)) {
            val useEpisodeArtwork = settings.getBooleanForKey("useEpisodeArtwork", false)
            val useFileArtwork = settings.getBooleanForKey("useEmbeddedArtwork", false)
            settings.useEpisodeArtwork.set(useEpisodeArtwork || useFileArtwork, updateModifiedAt = true)
        }
    }
}
