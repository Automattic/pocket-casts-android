package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.deeplink.CreateAccountDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.DownloadsDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ImportDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowFiltersDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowUpNextTabDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.StaffPicksDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ThemesDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.UpsellDeepLink
import au.com.shiftyjelly.pocketcasts.preferences.Settings.NotificationId
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed interface NotificationType {
    val notificationId: Int
    val subcategory: String
    val titleRes: Int
    val messageRes: Int

    fun toIntent(context: Context): Intent
}

sealed class OnboardingNotificationType(
    override val notificationId: Int,
    override val subcategory: String,
    override val titleRes: Int,
    override val messageRes: Int,
    val dayOffset: Int,
) : NotificationType {

    object Sync : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_SYNC.value,
        subcategory = SUBCATEGORY_SYNC,
        titleRes = LR.string.notification_sync_title,
        messageRes = LR.string.notification_sync_message,
        dayOffset = 0,
    ) {
        override fun toIntent(context: Context) = CreateAccountDeepLink.toIntent(context)
    }

    object Import : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_IMPORT.value,
        subcategory = SUBCATEGORY_IMPORT,
        titleRes = LR.string.notification_import_title,
        messageRes = LR.string.notification_import_message,
        dayOffset = 1,
    ) {
        override fun toIntent(context: Context) = ImportDeepLink.toIntent(context)
    }

    object UpNext : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_UPNEXT.value,
        subcategory = SUBCATEGORY_UP_NEXT,
        titleRes = LR.string.notification_up_next_title,
        messageRes = LR.string.notification_up_next_message,
        dayOffset = 2,
    ) {
        override fun toIntent(context: Context) = ShowUpNextTabDeepLink.toIntent(context)
    }

    object Filters : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_FILTERS.value,
        subcategory = SUBCATEGORY_FILTERS,
        titleRes = LR.string.notification_filters_title,
        messageRes = LR.string.notification_filters_message,
        dayOffset = 3,
    ) {
        override fun toIntent(context: Context) = ShowFiltersDeepLink.toIntent(context)
    }

    object Themes : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_THEMES.value,
        subcategory = SUBCATEGORY_THEMES,
        titleRes = LR.string.notification_themes_title,
        messageRes = LR.string.notification_themes_message,
        dayOffset = 4,
    ) {
        override fun toIntent(context: Context) = ThemesDeepLink.toIntent(context)
    }

    object StaffPicks : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_STAFF_PICKS.value,
        subcategory = SUBCATEGORY_STAFF_PICKS,
        titleRes = LR.string.notification_staff_picks_title,
        messageRes = LR.string.notification_staff_picks_message,
        dayOffset = 5,
    ) {
        override fun toIntent(context: Context) = StaffPicksDeepLink.toIntent(context)
    }

    object PlusUpsell : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_UPSELL.value,
        subcategory = SUBCATEGORY_PLUS_UP_SELL,
        titleRes = LR.string.notification_plus_upsell_title,
        messageRes = LR.string.notification_plus_upsell_message,
        dayOffset = 6,
    ) {
        override fun toIntent(context: Context) = UpsellDeepLink.toIntent(context)
    }

    companion object {
        const val SUBCATEGORY_SYNC = "sync"
        const val SUBCATEGORY_IMPORT = "import"
        const val SUBCATEGORY_UP_NEXT = "up_next"
        const val SUBCATEGORY_FILTERS = "filters"
        const val SUBCATEGORY_THEMES = "themes"
        const val SUBCATEGORY_STAFF_PICKS = "staff_picks"
        const val SUBCATEGORY_PLUS_UP_SELL = "plus_upsell"

        val values: List<OnboardingNotificationType>
            get() = listOf(
                Sync,
                Import,
                UpNext,
                Filters,
                Themes,
                StaffPicks,
                PlusUpsell,
            )

        fun fromSubcategory(subcategory: String): OnboardingNotificationType? {
            return values.firstOrNull { it.subcategory == subcategory }
        }
    }
}

sealed class ReEngagementNotificationType(
    override val notificationId: Int,
    override val subcategory: String,
    override val titleRes: Int,
    override val messageRes: Int,
) : NotificationType {

    object WeMissYou : ReEngagementNotificationType(
        notificationId = NotificationId.RE_ENGAGEMENT.value,
        subcategory = SUBCATEGORY_REENGAGE_WE_MISS_YOU,
        titleRes = LR.string.notification_reengage_we_miss_you_title,
        messageRes = LR.string.notification_reengage_we_miss_you_message,
    ) {
        override fun toIntent(context: Context): Intent = DownloadsDeepLink.toIntent(context)
    }

    object CatchUpOffline : ReEngagementNotificationType(
        notificationId = NotificationId.RE_ENGAGEMENT.value,
        subcategory = SUBCATEGORY_REENGAGE_CATCH_UP_OFFLINE,
        titleRes = LR.string.notification_reengage_catch_up_offline_title,
        messageRes = LR.string.notification_reengage_catch_up_offline_message,
    ) {
        override fun toIntent(context: Context): Intent = DownloadsDeepLink.toIntent(context)
    }

    companion object {
        const val SUBCATEGORY_REENGAGE_WE_MISS_YOU = "we_miss_you"
        const val SUBCATEGORY_REENGAGE_CATCH_UP_OFFLINE = "catch_up_offline"

        val values: List<ReEngagementNotificationType>
            get() = listOf(
                WeMissYou,
                CatchUpOffline,
            )

        fun fromSubcategory(subcategory: String): ReEngagementNotificationType? {
            return values.firstOrNull { it.subcategory == subcategory }
        }
    }
}
