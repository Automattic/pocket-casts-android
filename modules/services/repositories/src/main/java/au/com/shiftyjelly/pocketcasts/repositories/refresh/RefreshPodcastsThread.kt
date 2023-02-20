package au.com.shiftyjelly.pocketcasts.repositories.refresh

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.text.TextUtils
import android.util.Pair
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.work.ListenableWorker
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.R
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.notification.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.NotificationBroadcastReceiver
import au.com.shiftyjelly.pocketcasts.repositories.sync.PodcastSyncProcess
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.RefreshResponse
import au.com.shiftyjelly.pocketcasts.servers.ServerCallback
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.old.SyncOldServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.old.UserNotLoggedInException
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class RefreshPodcastsThread(
    private val context: Context,
    private val runNow: Boolean
) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RefreshPodcastsThreadEntryPoint {
        fun serverManager(): ServerManager
        fun podcastManager(): PodcastManager
        fun playlistManager(): PlaylistManager
        fun statsManager(): StatsManager
        fun fileStorage(): FileStorage
        fun syncOldServerManager(): SyncOldServerManager
        fun syncServerManager(): SyncServerManager
        fun podcastCacheServerManager(): PodcastCacheServerManagerImpl
        fun userEpisodeManager(): UserEpisodeManager
        fun subscriptionManager(): SubscriptionManager
        fun folderManager(): FolderManager
        fun settings(): Settings
        fun playbackManager(): PlaybackManager
        fun episodeManager(): EpisodeManager
        fun downloadManager(): DownloadManager
        fun notificationHelper(): NotificationHelper
        fun userManager(): UserManager
    }

    @Volatile
    private var taskHasBeenCancelled = false

    fun isAllowedToRun(runNow: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        return now > lastRefreshAllowedTime + if (runNow) THROTTLE_RUN_NOW_MS else THROTTLE_PERIODIC_MS
    }

    fun getEntryPoint(): RefreshPodcastsThreadEntryPoint {
        return EntryPoints.get(context.applicationContext, RefreshPodcastsThreadEntryPoint::class.java)
    }

    fun run(): ListenableWorker.Result {
        val entryPoint = getEntryPoint()
        try {
            val settings = entryPoint.settings()

            settings.setRefreshState(RefreshState.Refreshing)

            if (taskHasBeenCancelled) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Not refreshing as task cancelled")
                refreshFailedOrCancelled("Not refreshing as task cancelled")
                return ListenableWorker.Result.success()
            }

            if (!Network.isConnected(context)) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Not refreshing as internet not connected")
                refreshFailedOrCancelled("Not refreshing as internet not connected")
                return ListenableWorker.Result.retry()
            }

            if (!isAllowedToRun(runNow)) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Not refreshing as too soon")
                try {
                    // sleep for half a second to give the user a feeling of "oh the app is refreshing"
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                }

                return ListenableWorker.Result.success()
            }
            lastRefreshAllowedTime = System.currentTimeMillis()

            refresh()

            return ListenableWorker.Result.success()
        } catch (e: Exception) {
            Timber.e(e)
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Refresh failed")

            if (e is SecurityException || e is UserNotLoggedInException) {
                val exceptionType = when (e) {
                    is SecurityException -> "SecurityException"
                    is UserNotLoggedInException -> "UserNotLoggedInException"
                    else -> "unexpected exception"
                }
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Signing out user because there was a $exceptionType")

                val userManager = entryPoint.userManager()
                val playbackManager = entryPoint.playbackManager()
                userManager.signOut(playbackManager, wasInitiatedByUser = false)
            } else {
                refreshFailedOrCancelled(e.message ?: "Unknown error")
            }

            return ListenableWorker.Result.failure()
        }
    }

    fun cancelExecution() {
        taskHasBeenCancelled = true
    }

    /** REFRESH  */
    private fun refresh() {
        val entryPoint = getEntryPoint()
        val podcastManager = entryPoint.podcastManager()
        val serverManager = entryPoint.serverManager()
        val podcasts = podcastManager.findSubscribed()
        val startTime = SystemClock.elapsedRealtime()
        serverManager.refreshPodcastsSync(
            podcasts,
            object : ServerCallback<RefreshResponse> {
                override fun dataReturned(result: RefreshResponse?) {
                    val elapsedTime = String.format("%d ms", SystemClock.elapsedRealtime() - startTime)
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - podcasts response - $elapsedTime")
                    processRefreshResponse(result)
                }

                override fun onFailed(
                    errorCode: Int,
                    userMessage: String?,
                    serverMessageId: String?,
                    serverMessage: String?,
                    throwable: Throwable?
                ) {
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Not refreshing as server call failed errorCode: $errorCode serverMessage: ${serverMessage ?: ""}")
                    if (throwable != null) {
                        LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "Server call failed")
                    }
                    refreshFailedOrCancelled("Not refreshing as server call failed errorCode: $errorCode serverMessage: ${serverMessage ?: ""}")
                }
            }
        )
    }

    private fun processRefreshResponse(result: RefreshResponse?) {
        if (taskHasBeenCancelled) {
            refreshFailedOrCancelled("Not refreshing as task cancelled (2)")
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Not refreshing as task cancelled (2)")
            return
        }

        val entryPoint = getEntryPoint()
        val podcastManager = entryPoint.podcastManager()
        val playbackManager = entryPoint.playbackManager()
        val episodeManager = entryPoint.episodeManager()
        val playlistManager = entryPoint.playlistManager()
        val downloadManager = entryPoint.downloadManager()
        val settings = entryPoint.settings()
        val notificationHelper = entryPoint.notificationHelper()

        val emptyResponse = result == null
        val notificationLastSeen = getNotificationLastSeen(entryPoint.settings())
        val episodeUuidsAdded = updatePodcasts(result)

        val syncRefreshState = sync()

        if (!emptyResponse) {
            var startTime = SystemClock.elapsedRealtime()
            episodeManager.checkForEpisodesToAutoArchive(playbackManager, podcastManager)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - checkForEpisodesToAutoArchive - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
            startTime = SystemClock.elapsedRealtime()
            podcastManager.checkForUnusedPodcasts(playbackManager)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - checkForUnusedPodcasts - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
            startTime = SystemClock.elapsedRealtime()
            playlistManager.checkForEpisodesToDownload(episodeManager, playbackManager)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - playlist checkForEpisodesToDownload - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
            startTime = SystemClock.elapsedRealtime()
            podcastManager.checkForEpisodesToDownload(episodeUuidsAdded, downloadManager)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - podcast checkForEpisodesToDownload - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
            startTime = SystemClock.elapsedRealtime()
            updateNotifications(notificationLastSeen, settings, podcastManager, episodeManager, notificationHelper, context)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - updateNotifications - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
        }

        if (syncRefreshState is RefreshState.Failed) {
            settings.setRefreshState(syncRefreshState)
        } else {
            settings.setRefreshState(RefreshState.Success(Date(System.currentTimeMillis())))
        }
    }

    private fun sync(): RefreshState {
        val entryPoint = getEntryPoint()
        val userManager = entryPoint.userManager()
        val playbackManager = entryPoint.playbackManager()

        val sync = PodcastSyncProcess(
            context = context,
            settings = entryPoint.settings(),
            episodeManager = entryPoint.episodeManager(),
            podcastManager = entryPoint.podcastManager(),
            playlistManager = entryPoint.playlistManager(),
            statsManager = entryPoint.statsManager(),
            fileStorage = entryPoint.fileStorage(),
            playbackManager = playbackManager,
            syncOldServerManager = entryPoint.syncOldServerManager(),
            syncServerManager = entryPoint.syncServerManager(),
            podcastCacheServerManager = entryPoint.podcastCacheServerManager(),
            userEpisodeManager = entryPoint.userEpisodeManager(),
            subscriptionManager = entryPoint.subscriptionManager(),
            folderManager = entryPoint.folderManager()
        )
        val startTime = SystemClock.elapsedRealtime()
        val syncCompletable = sync.run()
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - sync complete - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
        val throwable = syncCompletable.blockingGet()
        if (throwable != null) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "SyncProcess: Sync failed")

            if (throwable is UserNotLoggedInException) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Signing out user because server post failed to log in")
                userManager.signOut(playbackManager, wasInitiatedByUser = false)
            } else {
                return RefreshState.Failed("Sync threw an error: ${throwable.message}")
            }
        }
        return RefreshState.Success(Date(System.currentTimeMillis()))
    }

    private fun refreshFailedOrCancelled(message: String) {
        getEntryPoint().settings().setRefreshState(RefreshState.Failed(message))
    }

    private fun updatePodcasts(result: RefreshResponse?): List<String> {
        if (result == null) {
            return emptyList()
        }

        val entryPoint = getEntryPoint()
        val podcastManager = entryPoint.podcastManager()
        val playbackManager = entryPoint.playbackManager()
        val episodeManager = entryPoint.episodeManager()
        val settings = entryPoint.settings()

        var newEpisodeCount = 0

        val episodesToAddToUpNext: MutableList<Pair<AddToUpNext, Episode>> = mutableListOf()
        val episodeUuidsAdded = ArrayList<String>()

        for (podcastUuid in result.getPodcastsWithUpdates()) {
            val podcast = podcastManager.findPodcastByUuid(podcastUuid) ?: continue
            var episodes = result.getUpdatesForPodcast(podcastUuid)
            if (episodes == null || episodes.isEmpty()) {
                continue // no updates
            }

            // only download the meta data for this episode for the first 10 episodes, after that we'd overwhelm the users phone
            val downloadMetaData = episodes.size + newEpisodeCount < 10
            val addedDate = Date()
            for (episode in episodes) {
                episode.addedDate = addedDate
            }
            episodes = episodeManager.add(episodes, podcast.uuid, downloadMetaData)

            // we now have some new episodes, update the latest episode uuid on the podcast row
            if (episodes.isNotEmpty()) {
                podcastManager.updateLatestEpisode(podcast, episodes[0])
                for ((uuid) in episodes) {
                    episodeUuidsAdded.add(uuid)
                }

                // auto add to up next
                if (!podcast.isAutoAddToUpNextOff) {
                    if (podcast.isAutoAddToUpNextPlayLast) {
                        episodesToAddToUpNext.addAll(episodes.map { Pair(AddToUpNext.Last as AddToUpNext, it) })
                    } else if (podcast.isAutoAddToUpNextPlayNext) {
                        episodesToAddToUpNext.addAll(episodes.map { Pair(AddToUpNext.Next as AddToUpNext, it) })
                    }
                }
            }

            newEpisodeCount += episodes.size
        }

        // Because we may not have refreshed for a while, we collect all the auto add to up next episodes
        // and run through them one by one sorted by their publish date. They are added to up next as if the action
        // was run right as they were published magically
        runBlocking {
            val upNextLimit = settings.getAutoAddUpNextLimit()
            episodesToAddToUpNext.sortBy { it.second.publishedDate }
            episodesToAddToUpNext.forEach {
                if (playbackManager.upNextQueue.queueEpisodes.size < upNextLimit) {
                    when (it.first) {
                        AddToUpNext.Next -> playbackManager.playNext(it.second, source = AnalyticsSource.UNKNOWN, userInitiated = false)
                        AddToUpNext.Last -> playbackManager.playLast(it.second, source = AnalyticsSource.UNKNOWN, userInitiated = false)
                    }
                } else if (playbackManager.upNextQueue.queueEpisodes.size >= upNextLimit &&
                    settings.getAutoAddUpNextLimitBehaviour() == Settings.AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP &&
                    it.first == AddToUpNext.Next
                ) {
                    playbackManager.playNext(it.second, source = AnalyticsSource.UNKNOWN, userInitiated = false)
                    playbackManager.upNextQueue.queueEpisodes.lastOrNull()?.let { lastEpisode ->
                        playbackManager.removeEpisode(lastEpisode, source = AnalyticsSource.UNKNOWN, userInitiated = false)
                    }
                }
            }
        }

        return episodeUuidsAdded
    }

    private fun getNotificationLastSeen(settings: Settings): Date {
        var lastSeen = settings.getNotificationLastSeen()
        if (lastSeen != null) {
            return lastSeen
        }
        // if no last seen date set to now so we don't get all the notifications
        lastSeen = Date()
        settings.setNotificationLastSeen(lastSeen)

        return lastSeen
    }

    companion object {

        private const val GROUP_NEW_EPISODES = "group_new_episodes"

        private val THROTTLE_RUN_NOW_MS: Long = if (BuildConfig.DEBUG) 0 else 15000 // 15 seconds
        private val THROTTLE_PERIODIC_MS: Long = if (BuildConfig.DEBUG) 0 else 5L * 60L * 1000L // 5 minutes
        private var lastRefreshAllowedTime: Long = -10000

        fun clearLastRefreshTime() {
            lastRefreshAllowedTime = -10000
        }

        fun updateNotifications(lastSeen: Date?, settings: Settings, podcastManager: PodcastManager, episodeManager: EpisodeManager, notificationHelper: NotificationHelper, context: Context) {
            if (lastSeen == null) {
                return
            }

            settings.setNotificationLastSeenToNow()

            val podcastsShowingNotifications = podcastManager.countNotificationsOn()
            if (podcastsShowingNotifications == 0) {
                return
            }

            val intentId = 675578

            try {
                val notificationsEpisodeAndPodcast = ArrayList<Pair<Episode, Podcast>>()

                val episodes = episodeManager.findNotificationEpisodes(lastSeen)
                for (episode in episodes) {
                    val podcast = podcastManager.findPodcastByUuid(episode.podcastUuid) ?: continue
                    notificationsEpisodeAndPodcast.add(Pair(episode, podcast))
                }

                if (notificationsEpisodeAndPodcast.isEmpty()) {
                    return
                }

                // order by published date
                notificationsEpisodeAndPodcast.sortWith { episodePodcastOne, episodePodcastTwo ->
                    val (_, _, publishedDate) = episodePodcastOne.first
                    val (_, _, publishedDate1) = episodePodcastTwo.first
                    publishedDate1.compareTo(publishedDate)
                }

                val isGroup = notificationsEpisodeAndPodcast.size > 1

                if (isGroup) {
                    var firstPodcastUuid: String? = null
                    val notificationLines = ArrayList<CharSequence>()

                    for (episodePodcast in notificationsEpisodeAndPodcast) {
                        val (uuid, _, _, title) = episodePodcast.second
                        val (_, _, _, title1) = episodePodcast.first
                        // create the summary notification text lines
                        notificationLines.add(formatNotificationLine(title, title1, context))
                        // remember the first and last podcast for the artwork on phone and wearshowSummaryNotification
                        if (firstPodcastUuid == null) {
                            firstPodcastUuid = uuid
                        }
                    }

                    // phone summary notifications display the first podcast icon but wear summary shows the last podcast notification
                    showSummaryNotification(notificationLines, notificationsEpisodeAndPodcast.size, firstPodcastUuid, intentId, settings, podcastManager, notificationHelper, context)
                }

                for (i in notificationsEpisodeAndPodcast.indices) {
                    val episodePodcast = notificationsEpisodeAndPodcast[i]
                    showEpisodeNotification(episodePodcast.second, episodePodcast.first, i, intentId, isGroup, podcastManager, notificationHelper, settings, context)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        @Suppress("NAME_SHADOWING", "DEPRECATION")
        private fun showEpisodeNotification(
            podcast: Podcast,
            episode: Episode,
            episodeIndex: Int,
            intentId: Int,
            isGroupNotification: Boolean,
            podcastManager: PodcastManager,
            notificationHelper: NotificationHelper,
            settings: Settings,
            context: Context
        ) {

            // order by published date on Google Wear devices
            val sortKey = String.format("%04d", episodeIndex)
            var intentId = intentId
            val manager = NotificationManagerCompat.from(context)

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = (System.currentTimeMillis() + intentId).toString()
                putExtra(Settings.INTENT_OPEN_APP_EPISODE_UUID, episode.uuid)
            }
            val pendingIntent = PendingIntent.getActivity(context, intentId, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
            intentId += 1

            val notificationTag = if (isGroupNotification) {
                NotificationBroadcastReceiver.NOTIFICATION_TAG_NEW_EPISODES_PREFIX + episode.uuid
            } else {
                NotificationBroadcastReceiver.NOTIFICATION_TAG_NEW_EPISODES_PRIMARY
            }

            val userActions = NewEpisodeNotificationAction.loadFromSettings(settings)

            val phoneActions = mutableListOf<NotificationCompat.Action>()
            val wearActions = mutableListOf<NotificationCompat.Action>()

            for (action in NewEpisodeNotificationAction.values()) {
                if (userActions.contains(action)) {
                    intentId++
                    val label = context.resources.getString(action.labelId)
                    val playIntent = buildNotificationIntent(intentId, action.notificationAction, episode, notificationTag, context)
                    phoneActions.add(NotificationCompat.Action(action.drawableId, label, playIntent))
                    wearActions.add(NotificationCompat.Action(action.largeDrawableId, label, playIntent))
                }
            }

            val color = ContextCompat.getColor(context, R.color.notification_color)

            var builder = notificationHelper.episodeNotificationChannelBuilder()
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(podcast.title)
                .setContentText(episode.title)
                .setSmallIcon(IR.drawable.notification)
                .setAutoCancel(true)
                .setColor(color)
                .setOnlyAlertOnce(true)
                .setSortKey(sortKey)
                .setContentIntent(pendingIntent)

            if (phoneActions.size > 0) {
                builder = builder.addAction(phoneActions[0])
            }
            if (phoneActions.size > 1) {
                builder = builder.addAction(phoneActions[1])
            }

            // Don't include three action on old devices because they don't fit
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                if (phoneActions.size > 2) {
                    builder = builder.addAction(phoneActions[2])
                }
            }

            if (isGroupNotification) {
                builder.setGroup(GROUP_NEW_EPISODES)
            } else {
                // don't set the delete intent if grouping the notifications as it won't fire the group intent
                val deletePendingIntent = getDeletePendingIntent(context)
                builder.setDeleteIntent(deletePendingIntent)
            }

            var wearableExtender = NotificationCompat.WearableExtender().clearActions()
            for (action in wearActions) {
                wearableExtender = wearableExtender.addAction(action)
            }
            builder.extend(wearableExtender)

            val bitmap = getPodcastNotificationBitmap(podcast.uuid, podcastManager, context)
            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
            }

            val notification = builder.build()

            // Add sound and vibrations
            if (!isGroupNotification) {
                val sound = settings.getNotificationSound()
                if (sound != null) {
                    notification.sound = sound
                }
                if (settings.isNotificationVibrateOn()) {
                    notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE
                }
            }

            manager.notify(notificationTag, NotificationBroadcastReceiver.NOTIFICATION_ID, notification)
        }

        private fun buildNotificationIntent(intentId: Int, intentName: String, episode: Episode, notificationTag: String, context: Context): PendingIntent {
            val intent = Intent(context, NotificationBroadcastReceiver::class.java)
            intent.action = (System.currentTimeMillis() + intentId).toString()
            intent.putExtra(NotificationBroadcastReceiver.INTENT_EXTRA_ACTION, intentName)
            intent.putExtra(NotificationBroadcastReceiver.INTENT_EXTRA_EPISODE_UUID, episode.uuid)
            intent.putExtra(NotificationBroadcastReceiver.INTENT_EXTRA_NOTIFICATION_TAG, notificationTag)
            return PendingIntent.getBroadcast(context, intentId, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
        }

        private fun getDeletePendingIntent(context: Context): PendingIntent {
            val deleteIntent = Intent(context, NotificationBroadcastReceiver::class.java)
            deleteIntent.action = NotificationBroadcastReceiver.INTENT_ACTION_NOTIFICATION_DELETED
            return PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
        }

        @Suppress("DEPRECATION")
        private fun showSummaryNotification(
            notificationLines: List<CharSequence>,
            episodeCounts: Int,
            firstPodcastUuid: String?,
            intentId: Int,
            settings: Settings,
            podcastManager: PodcastManager,
            notificationHelper: NotificationHelper,
            context: Context
        ) {
            var intentIndex = intentId

            var artworkIcon: Bitmap? = null
            var wearBackground: Bitmap? = null

            if (firstPodcastUuid != null) {
                artworkIcon = getPodcastNotificationBitmap(firstPodcastUuid, podcastManager, context)
                wearBackground = getPodcastNotificationWearBitmap(firstPodcastUuid, podcastManager, context)
            }

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = Settings.INTENT_OPEN_APP_NEW_EPISODES
            }
            val pendingIntent = PendingIntent.getActivity(context, intentIndex, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
            intentIndex += 1

            val inboxStyle = NotificationCompat.InboxStyle()
            for (line in notificationLines) {
                inboxStyle.addLine(line)
            }
            inboxStyle.setBigContentTitle("$episodeCounts new episode" + (if (episodeCounts == 1) "" else "s"))

            val deletePendingIntent = getDeletePendingIntent(context)

            val color = ContextCompat.getColor(context, R.color.notification_color)

            val summaryBuilder = notificationHelper.episodeNotificationChannelBuilder()
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle("$episodeCounts new episode" + (if (episodeCounts == 1) "" else "s"))
                .setContentText(notificationLines[0])
                .setSmallIcon(IR.drawable.notification)
                .setStyle(inboxStyle)
                .setColor(color)
                .setDeleteIntent(deletePendingIntent)
                .setGroup(GROUP_NEW_EPISODES)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)

            if (artworkIcon != null) {
                summaryBuilder.setLargeIcon(artworkIcon)
            }
            if (wearBackground != null) {
                summaryBuilder.extend(NotificationCompat.WearableExtender().setBackground(wearBackground))
            }

            val summaryNotification = summaryBuilder.build()

            // Add sound and vibrations
            val sound = settings.getNotificationSound()
            if (sound != null) {
                summaryNotification.sound = sound
            }
            if (settings.isNotificationVibrateOn()) {
                summaryNotification.defaults = summaryNotification.defaults or Notification.DEFAULT_VIBRATE
            }

            val manager = NotificationManagerCompat.from(context)
            manager.notify(NotificationBroadcastReceiver.NOTIFICATION_TAG_NEW_EPISODES_PRIMARY, NotificationBroadcastReceiver.NOTIFICATION_ID, summaryNotification)
        }

        private fun getPodcastNotificationWearBitmap(uuid: String?, podcastManager: PodcastManager, context: Context): Bitmap? {
            if (uuid == null) {
                return null
            }
            val podcast = podcastManager.findPodcastByUuid(uuid) ?: return null

            return PodcastImageLoader(context = context, isDarkTheme = true, transformations = emptyList()).getBitmap(podcast, 400)
        }

        private fun getPodcastNotificationBitmap(uuid: String?, podcastManager: PodcastManager, context: Context): Bitmap? {
            if (uuid == null) {
                return null
            }
            val podcast = podcastManager.findPodcastByUuid(uuid) ?: return null

            val resources = context.resources
            val width = resources.getDimension(android.R.dimen.notification_large_icon_width).toInt()

            return PodcastImageLoader(context = context, isDarkTheme = true, transformations = emptyList()).getBitmap(podcast, width)
        }

        private fun formatNotificationLine(podcastName: String?, episodeName: String?, context: Context): CharSequence {
            return HtmlCompat.fromHtml(
                context.resources.getString(
                    LR.string.podcast_notification_new_episode,
                    if (podcastName == null) "" else TextUtils.htmlEncode(podcastName),
                    if (episodeName == null) "" else TextUtils.htmlEncode(episodeName)
                ),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        }
    }
}

private sealed class AddToUpNext {
    object Next : AddToUpNext()
    object Last : AddToUpNext()
}
