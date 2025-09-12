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
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationOpenReceiverActivity.Companion.toIntentRelayed
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SuggestedFoldersManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.awaitFirstOrNull
import au.com.shiftyjelly.pocketcasts.images.R as IR

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: Settings,
    private val notificationHelper: NotificationHelper,
    private val notificationManager: NotificationManager,
    private val suggestedFoldersManager: SuggestedFoldersManager,
    private val userManager: UserManager,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (Util.getAppPlatform(applicationContext) != AppPlatform.Phone) return Result.failure()

        val subcategory = inputData.getString(SUBCATEGORY) ?: return Result.failure()

        val type = NotificationType.fromSubCategory(subcategory) ?: return Result.failure()

        val shouldSkipValidations = inputData.getBoolean(SHOULD_SKIP_VALIDATIONS, false)

        if (!shouldSkipValidations && !type.isSettingsToggleOn(settings)) {
            return Result.failure()
        }

        if (!shouldSkipValidations && (notificationManager.hasUserInteractedWithFeature(type) || !shouldSchedule(type))) {
            return Result.failure()
        }

        val notification = getNotificationBuilder(type).build()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && (shouldSkipValidations || FeatureFlag.isEnabled(Feature.NOTIFICATIONS_REVAMP))) {
            NotificationManagerCompat.from(applicationContext).notify(type.notificationId, notification)
            notificationManager.updateNotificationSent(type)
        }

        return Result.success()
    }

    private suspend fun shouldSchedule(type: NotificationType): Boolean {
        return when (type) {
            is OnboardingNotificationType.Sync -> {
                userManager.getSignInState().awaitFirstOrNull()?.isSignedIn != true
            }
            is TrendingAndRecommendationsNotificationType.Recommendations -> {
                userManager.getSignInState().awaitFirstOrNull()?.isSignedIn == true
            }
            is NewFeaturesAndTipsNotificationType.SmartFolders -> {
                suggestedFoldersManager.refreshSuggestedFolders()
                val folders = suggestedFoldersManager.observeSuggestedFolders().firstOrNull()
                !folders.isNullOrEmpty()
            }
            is OnboardingNotificationType.PlusUpsell,
            is OffersNotificationType.UpgradeNow,
            -> {
                val subscription = settings.cachedSubscription.value
                subscription == null || subscription.expiryDate.isBefore(Instant.now())
            }
            else -> true
        }
    }

    private fun getNotificationBuilder(type: NotificationType): NotificationCompat.Builder {
        val downloadedEpisodes = inputData.getInt(DOWNLOADED_EPISODES, 0)

        val builder = when (type) {
            is TrendingAndRecommendationsNotificationType -> {
                notificationHelper.trendingAndRecommendationsChannelBuilder()
            }

            is NewFeaturesAndTipsNotificationType -> {
                notificationHelper.featuresAndTipsChannelBuilder()
            }

            is OffersNotificationType -> {
                notificationHelper.offersChannelBuilder()
            }

            else -> notificationHelper.dailyRemindersChannelBuilder()
        }

        return builder
            .setSmallIcon(IR.drawable.notification)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(applicationContext.resources.getString(type.titleRes))
            .setContentText(type.formattedMessage(applicationContext, downloadedEpisodes))
            .setColor(ContextCompat.getColor(applicationContext, R.color.notification_color))
            .setAutoCancel(true)
            .setContentIntent(broadcastIntent(type))
    }

    private fun broadcastIntent(type: NotificationType): PendingIntent {
        return PendingIntent.getActivity(
            applicationContext,
            0,
            type.toIntentRelayed(applicationContext),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val SUBCATEGORY = "subcategory"
        const val DOWNLOADED_EPISODES = "downloaded_episodes"
        const val SHOULD_SKIP_VALIDATIONS = "should_skip_validations"
    }
}
