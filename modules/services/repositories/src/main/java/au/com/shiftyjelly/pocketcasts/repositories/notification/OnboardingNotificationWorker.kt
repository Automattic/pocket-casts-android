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
import au.com.shiftyjelly.pocketcasts.deeplink.CreateAccountDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ImportDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowFiltersDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowUpNextTabDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.StaffPicksDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ThemesDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.UpsellDeepLink
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.R
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import au.com.shiftyjelly.pocketcasts.images.R as IR

@HiltWorker
class OnboardingNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: Settings,
    private val notificationHelper: NotificationHelper,
    private val notificationManager: NotificationManager,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!settings.dailyRemindersNotification.value) {
            return Result.failure()
        }

        val subcategory = inputData.getString("subcategory") ?: return Result.failure()
        val type = OnboardingNotificationType.fromSubcategory(subcategory) ?: return Result.failure()

        if (notificationManager.hasUserInteractedWithFeature(type)) {
            return Result.failure()
        }

        val notification = getNotificationBuilder(type).build()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && FeatureFlag.isEnabled(Feature.NOTIFICATIONS_REVAMP)) {
            NotificationManagerCompat.from(applicationContext).notify(type.notificationId, notification)
            notificationManager.updateOnboardingNotificationSent(type)
        }

        return Result.success()
    }

    private fun getNotificationBuilder(type: OnboardingNotificationType): NotificationCompat.Builder {
        return notificationHelper.dailyRemindersChannelBuilder()
            .setSmallIcon(IR.drawable.notification)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(applicationContext.resources.getString(type.titleRes))
            .setContentText(applicationContext.resources.getString(type.messageRes))
            .setColor(ContextCompat.getColor(applicationContext, R.color.notification_color))
            .setContentIntent(openPageIntent(type))
    }

    private fun openPageIntent(type: OnboardingNotificationType): PendingIntent {
        val intent = when (type) {
            OnboardingNotificationType.Filters -> ShowFiltersDeepLink.toIntent(applicationContext)
            OnboardingNotificationType.Import -> ImportDeepLink.toIntent(applicationContext)
            OnboardingNotificationType.PlusUpsell -> UpsellDeepLink.toIntent(applicationContext)
            OnboardingNotificationType.StaffPicks -> StaffPicksDeepLink.toIntent(applicationContext)
            OnboardingNotificationType.Sync -> CreateAccountDeepLink.toIntent(applicationContext)
            OnboardingNotificationType.Themes -> ThemesDeepLink.toIntent(applicationContext)
            OnboardingNotificationType.UpNext -> ShowUpNextTabDeepLink.toIntent(applicationContext)
        }
        return PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
