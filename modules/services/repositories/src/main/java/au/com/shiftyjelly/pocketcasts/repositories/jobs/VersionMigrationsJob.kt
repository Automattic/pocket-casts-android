package au.com.shiftyjelly.pocketcasts.repositories.jobs

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("SpecifyJobSchedulerIdRange")
class VersionMigrationsJob : JobService() {

    companion object {
        fun run(podcastManager: PodcastManager, settings: Settings, context: Context) {
            runSync(podcastManager, settings)
            runAsync(settings, context)
        }

        /**
         * Run short migrations straight away.
         */
        private fun runSync(podcastManager: PodcastManager, settings: Settings) {
            performUpdateIfRequired(updateKey = "run_v7_20", settings = settings) {
                // Upgrading to version 7.20.0 requires the folders from the servers to be added to the existing podcasts. In case the user doesn't have internet this is done as part of the regular sync process.
                if (settings.isLoggedIn()) {
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
                    .build()
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
        if (!settings.getUsedAccountManager()) {
            performAccountManagerMigration(application)
        }

        removeCustomEpisodes()
        removeOldTempPodcastDirectory()
        unscheduleEpisodeDetailsJob(applicationContext)
        unscheduleRefreshJob(applicationContext)

        if (previousVersionCode < 6362) {
            upgradeTrimSilenceMode()
        }
    }

    private fun removeOldTempPodcastDirectory() {
        try {
            val oldTempDirectory = fileStorage.oldTempPodcastDirectory
            FileUtil.deleteDirectoryContents(oldTempDirectory.absolutePath)
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Could not clear old podcast temp directory")
        }
    }

    private fun removeCustomEpisodes() {
        val customPodcastUuid = "customFolderPodcast"
        val podcast: Podcast = podcastManager.findPodcastByUuid(customPodcastUuid) ?: return
        val episodes: List<Episode> = episodeManager.findEpisodesByPodcastOrderedByPublishDate(podcast)
        episodes.forEach { episode ->
            playbackManager.removeEpisode(
                episodeToRemove = episode,
                source = AnalyticsSource.UNKNOWN,
                userInitiated = false
            )
            appDatabase.episodeDao().delete(episode)
        }
        appDatabase.podcastDao().deleteByUuid(customPodcastUuid)
    }

    private fun performV7Migration() {
        // We want v6 users to keep defaulting to download, new users should get the new stream default
        val currentStreamingPreference = if (settings.contains(Settings.PREFERENCE_GLOBAL_STREAMING_MODE)) settings.streamingMode() else false
        settings.setStreamingMode(currentStreamingPreference)
    }

    private fun addUpNextAutoDownload() {
        settings.setUpNextAutoDownloaded(!settings.streamingMode())
    }

    private fun deletePodcastImages() {
        try {
            val thumbnailsFolder = fileStorage.getOrCreateDirectory("podcast_thumbnails")
            if (thumbnailsFolder.exists()) {
                thumbnailsFolder.delete()
            }
            val imageFolder = fileStorage.getOrCreateDirectory("images")
            if (imageFolder.exists()) {
                imageFolder.delete()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun performAccountManagerMigration(context: Context) {
        val oldDetails = settings.getOldSyncDetails()
        val email = oldDetails.first
        val password = oldDetails.second

        if (email != null && password != null) {
            val am = AccountManager.get(context)
            val account = Account(email, AccountConstants.ACCOUNT_TYPE)
            am.addAccountExplicitly(account, password, null)

            val result = am.getAuthToken(account, AccountConstants.TOKEN_TYPE, Bundle(), false, null, null).result
            val token = result.getString(AccountManager.KEY_AUTHTOKEN) // Force a token refresh
            Timber.d("Migrated token $token")
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Migrated to account manager")
        } else {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "No account found to migrate")
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
}
