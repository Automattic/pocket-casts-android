package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.app.Activity
import android.os.Bundle
import androidx.core.app.NotificationCompat

interface NotificationHelper {

    fun hasNotificationsPermission(): Boolean
    fun openNotificationSettings(activity: Activity?)
    fun setupNotificationChannels()

    fun downloadChannelBuilder(): NotificationCompat.Builder
    fun playbackChannelBuilder(): NotificationCompat.Builder
    fun episodeNotificationChannelBuilder(): NotificationCompat.Builder
    fun playbackErrorChannelBuilder(): NotificationCompat.Builder
    fun podcastImportChannelBuilder(): NotificationCompat.Builder
    fun bookmarkChannelBuilder(): NotificationCompat.Builder
    fun downloadsFixChannelBuilder(): NotificationCompat.Builder
    fun downloadsFixCompleteChannelBuilder(): NotificationCompat.Builder
    fun openEpisodeNotificationSettings(activity: Activity?)
    fun openDailyReminderNotificationSettings(activity: Activity?)
    fun openTrendingAndRecommendationsNotificationSettings(activity: Activity?)
    fun openNewFeaturesAndTipsNotificationSettings(activity: Activity?)
    fun openOffersNotificationSettings(activity: Activity?)
    fun dailyRemindersChannelBuilder(): NotificationCompat.Builder
    fun trendingAndRecommendationsChannelBuilder(): NotificationCompat.Builder
    fun featuresAndTipsChannelBuilder(): NotificationCompat.Builder
    fun offersChannelBuilder(): NotificationCompat.Builder
    fun isShowing(notificationId: Int): Boolean
    fun removeNotification(intentExtras: Bundle?, notificationId: Int)
}
