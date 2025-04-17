package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.R
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationSchedulerImpl.Companion.DOWNLOADED_EPISODES
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationSchedulerImpl.Companion.SUBCATEGORY
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import au.com.shiftyjelly.pocketcasts.images.R as IR

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: Settings,
    private val notificationHelper: NotificationHelper,
    private val notificationManager: NotificationManager,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val subcategory = inputData.getString(SUBCATEGORY) ?: return Result.failure()

        val type =
            OnboardingNotificationType.fromSubcategory(subcategory) ?: ReEngagementNotificationType.fromSubcategory(subcategory) ?: return Result.failure()

        if (!type.isSettingsToggleOn(settings)) {
            return Result.failure()
        }

        if (notificationManager.hasUserInteractedWithFeature(type)) {
            return Result.failure()
        }

        val notification = getNotificationBuilder(type).build()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && FeatureFlag.isEnabled(Feature.NOTIFICATIONS_REVAMP)) {
            NotificationManagerCompat.from(applicationContext).notify(type.notificationId, notification)
            notificationManager.updateNotificationSent(type)
        }

        return Result.success()
    }

    private fun getNotificationBuilder(type: NotificationType): NotificationCompat.Builder {
        return when (type) {
            is OnboardingNotificationType -> buildOnboardingNotification(type)
            is ReEngagementNotificationType -> buildReEngagementNotification(type)
        }
    }

    private fun buildOnboardingNotification(type: OnboardingNotificationType): NotificationCompat.Builder {
        return notificationHelper.dailyRemindersChannelBuilder()
            .setSmallIcon(IR.drawable.notification)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(applicationContext.resources.getString(type.titleRes))
            .setContentText(applicationContext.resources.getString(type.messageRes))
            .setColor(ContextCompat.getColor(applicationContext, R.color.notification_color))
            .setContentIntent(openPageIntent(type))
    }

    private fun buildReEngagementNotification(type: ReEngagementNotificationType): NotificationCompat.Builder {
        val downloadedEpisodes = inputData.getInt(DOWNLOADED_EPISODES, 0)

        val contentText = if (type is ReEngagementNotificationType.CatchUpOffline && downloadedEpisodes != 0) {
            applicationContext.resources.getString(type.messageRes, downloadedEpisodes)
        } else {
            applicationContext.resources.getString(type.messageRes)
        }

        return notificationHelper.dailyRemindersChannelBuilder()
            .setSmallIcon(IR.drawable.notification)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(applicationContext.resources.getString(type.titleRes))
            .setContentText(contentText)
            .setColor(ContextCompat.getColor(applicationContext, R.color.notification_color))
            .setContentIntent(openPageIntent(type))
    }

    private fun openPageIntent(type: NotificationType): PendingIntent {
        return PendingIntent.getActivity(applicationContext, 0, type.toIntent(applicationContext), PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
