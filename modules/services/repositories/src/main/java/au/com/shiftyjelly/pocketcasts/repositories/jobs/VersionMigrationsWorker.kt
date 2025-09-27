package au.com.shiftyjelly.pocketcasts.repositories.jobs

import android.app.job.JobScheduler
import android.content.Context
import androidx.core.text.isDigitsOnly
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.Membership
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.squareup.moshi.Moshi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

@HiltWorker
class VersionMigrationsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appDatabase: AppDatabase,
    private val episodeManager: EpisodeManager,
    private val fileStorage: FileStorage,
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val VERSION_MIGRATIONS_WORKER_NAME = "version_migrations_worker"
        fun performMigrations(
            context: Context,
            settings: Settings,
            moshi: Moshi,
        ) {
            performMigrationsSync(settings, moshi)
            enqueueAsyncMigrations(settings, context)
        }

        /**
         * Perform short migrations straight away.
         */
        private fun performMigrationsSync(settings: Settings, moshi: Moshi) {
            migrateSubscriptionStatusToSubscription(settings)
            migrateSubscriptionToMembership(settings, moshi)
        }

        private fun migrateSubscriptionStatusToSubscription(settings: Settings) {
            if (settings.contains("accountstatus", isPrivate = true)) {
                val rawStatus = settings.getStringForKey("accountstatus", isPrivate = true) ?: return
                runCatching { mapToSubscriptionOrThrow(rawStatus) }
                    .onFailure { error -> LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, error, "Failed to migrate subscription") }
                    .onSuccess { subscription ->
                        val membership = Membership(
                            subscription = subscription,
                            createdAt = null,
                            features = emptyList(),
                        )
                        settings.cachedMembership.set(membership, updateModifiedAt = false)
                    }
                settings.deleteKey("accountstatus", isPrivate = true)
            }
        }

        private fun mapToSubscriptionOrThrow(rawStatus: String): Subscription? {
            val jsonStatus = JSONObject(rawStatus)
            return Subscription(
                tier = when (val tier = jsonStatus.getString("tier")) {
                    "PATRON" -> SubscriptionTier.Patron
                    "PLUS" -> SubscriptionTier.Plus
                    "FREE" -> return null
                    else -> error("Unknown tier: $tier")
                },
                billingCycle = when (jsonStatus.getString("frequency")) {
                    "MONTHLY" -> BillingCycle.Monthly
                    "YEARLY" -> BillingCycle.Yearly
                    "NONE" -> null
                    else -> return null
                },
                platform = when (jsonStatus.getString("platform")) {
                    "IOS" -> SubscriptionPlatform.iOS
                    "ANDROID" -> SubscriptionPlatform.Android
                    "WEB" -> SubscriptionPlatform.Web
                    "GIFT" -> SubscriptionPlatform.Gift
                    else -> SubscriptionPlatform.Unknown
                },
                expiryDate = Instant.parse(jsonStatus.getString("expiry")),
                isAutoRenewing = jsonStatus
                    .getJSONArray("subscriptions")
                    .getJSONObject(jsonStatus.getInt("index"))
                    .getBoolean("autoRenewing"),
                giftDays = jsonStatus.getInt("giftDays"),
            )
        }

        private fun migrateSubscriptionToMembership(settings: Settings, moshi: Moshi) {
            if (settings.contains("user_subscription", isPrivate = true)) {
                val rawSubscription = settings.getStringForKey("user_subscription", isPrivate = true) ?: return
                val moshi = moshi.adapter(Subscription::class.java)
                runCatching { requireNotNull(moshi.fromJson(rawSubscription)) { "Failed to decode subscription" } }
                    .onFailure { error -> LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, error, "Failed to migrate subscription") }
                    .onSuccess { subscription ->
                        val membership = Membership(
                            subscription = subscription,
                            createdAt = null,
                            features = emptyList(),
                        )
                        settings.cachedMembership.set(membership, updateModifiedAt = false)
                    }
                settings.deleteKey("user_subscription", isPrivate = true)
            }
        }

        /**
         * Enqueue longer migrations as a background task.
         */
        private fun enqueueAsyncMigrations(settings: Settings, context: Context) {
            val previousVersionCode = settings.getMigratedVersionCode()
            val versionCode = settings.getVersionCode()

            if (previousVersionCode >= versionCode) {
                Timber.i("No version migration needed.")
                return
            }
            Timber.i("Migrating from version $previousVersionCode to $versionCode")

            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Running VersionMigrationsWorker")
            val workRequest = OneTimeWorkRequestBuilder<VersionMigrationsWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(VERSION_MIGRATIONS_WORKER_NAME, ExistingWorkPolicy.KEEP, workRequest)
        }

        /**
         * Convert the shared preference multi_select_items from resource ids to strings.
         * From: <string name="multi_select_items">2131362844,2131362845,2131362852,2131362850,2131362831,2131362829,2131362836</string>
         * To: <string name="multi_select_items">star,play_last,play_next,download,archive,share,mark_as_played</string>
         */
        fun upgradeMultiSelectItems(settings: Settings) {
            val items = settings.getMultiSelectItems()
            if (items.isEmpty() || !items.first().isDigitsOnly()) {
                return
            }

            val resourceToSetting = hashMapOf(
                // 7.70
                "2131362859" to "star",
                "2131362851" to "play_last",
                "2131362852" to "play_next",
                "2131362838" to "download",
                "2131362836" to "archive",
                "2131362857" to "share",
                "2131362843" to "mark_as_played",
                // 7.71-rc-1 and 7.71-rc-2
                "2131362834" to "star",
                "2131362826" to "play_last",
                "2131362827" to "play_next",
                "2131362813" to "download",
                "2131362811" to "archive",
                "2131362832" to "share",
                "2131362818" to "mark_as_played",
            )

            val itemsUpdated = items.map { resourceToSetting[it] ?: it }
            settings.setMultiSelectItems(itemsUpdated)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "VersionMigrationsWorker - started")
            performMigrationsAsync()
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "VersionMigrationsWorker - finished")
            Result.success()
        } catch (t: Throwable) {
            Timber.e(t)
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, t, "VersionMigrationsWorker - failed.")
            Result.failure()
        }
    }

    private suspend fun performMigrationsAsync() {
        val previousVersionCode = settings.getMigratedVersionCode()
        val versionCode = settings.getVersionCode()

        settings.setMigratedVersionCode(versionCode)

        // don't migrate when first installing
        if (previousVersionCode == 0) {
            settings.setWhatsNewVersionCode(Settings.WHATS_NEW_VERSION_CODE)
            configurePlaylistForNewUsersOnboarding()
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

        if (previousVersionCode < 6362) {
            upgradeTrimSilenceMode()
        }

        if (previousVersionCode < 9209) {
            consolidateEmbeddedArtworkSettings(applicationContext)
        }

        // Exclude 7.63-rc-3 (9227) from the migration as it was already migrated.
        if (previousVersionCode < 9230 && previousVersionCode != 9227) {
            migrateToGranularEpisodeArtworkSettings(applicationContext)
        }

        if (previousVersionCode < 9235) {
            enableDynamicColors()
        }

        upgradeMultiSelectItems(settings)
    }

    private fun removeOldTempPodcastDirectory() {
        try {
            fileStorage.getOrCreateEpisodesOldTempDir()?.absolutePath?.let(FileUtil::deleteDirContents)
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Could not clear old podcast temp directory")
        }
    }

    private suspend fun removeCustomEpisodes() {
        val customPodcastUuid = "customFolderPodcast"
        val podcast: Podcast = podcastManager.findPodcastByUuid(customPodcastUuid) ?: return
        val episodes: List<PodcastEpisode> = episodeManager.findEpisodesByPodcastOrderedByPublishDateBlocking(podcast)
        episodes.forEach { episode ->
            playbackManager.removeEpisode(
                episodeToRemove = episode,
                source = SourceView.UNKNOWN,
                userInitiated = false,
            )
            appDatabase.episodeDao().deleteBlocking(episode)
        }
        appDatabase.podcastDao().deleteByUuidBlocking(customPodcastUuid)
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

    private suspend fun upgradeTrimSilenceMode() {
        try {
            val podcasts = podcastManager.findSubscribedBlocking()
            podcasts.forEach { podcast ->
                if (podcast.isSilenceRemoved && podcast.trimMode == TrimMode.OFF) {
                    podcastManager.updateTrimModeBlocking(podcast, TrimMode.LOW)
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
            settings.setBooleanForKey("useEpisodeArtwork", useEpisodeArtwork || useFileArtwork)
        }
    }

    private fun migrateToGranularEpisodeArtworkSettings(context: Context) {
        if (!Util.isWearOs(context) && !Util.isAutomotive(context)) {
            val useEpisodeArtwork = settings.getBooleanForKey("useEpisodeArtwork", false)
            settings.artworkConfiguration.set(ArtworkConfiguration((useEpisodeArtwork)), updateModifiedAt = true)
        }
    }

    private fun enableDynamicColors() {
        settings.useDynamicColorsForWidget.set(true, updateModifiedAt = true)
    }

    private fun configurePlaylistForNewUsersOnboarding() {
        if (FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)) {
            settings.showPlaylistsOnboarding.set(false, updateModifiedAt = false)
        }
    }
}
