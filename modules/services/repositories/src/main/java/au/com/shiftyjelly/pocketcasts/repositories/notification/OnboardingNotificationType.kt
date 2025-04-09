package au.com.shiftyjelly.pocketcasts.repositories.notification
import au.com.shiftyjelly.pocketcasts.preferences.Settings.NotificationId
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class OnboardingNotificationType(
    val notificationId: Int,
    val subcategory: String,
    val titleRes: Int,
    val messageRes: Int,
    val dayOffset: Int,
) {
    object Sync : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_SYNC.value,
        subcategory = SUBCATEGORY_SYNC,
        titleRes = LR.string.notification_sync_title,
        messageRes = LR.string.notification_sync_message,
        dayOffset = 0,
    )

    object Import : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_IMPORT.value,
        subcategory = SUBCATEGORY_IMPORT,
        titleRes = LR.string.notification_import_title,
        messageRes = LR.string.notification_import_message,
        dayOffset = 1,
    )

    object UpNext : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_UPNEXT.value,
        subcategory = SUBCATEGORY_UP_NEXT,
        titleRes = LR.string.notification_up_next_title,
        messageRes = LR.string.notification_up_next_message,
        dayOffset = 2,
    )

    object Filters : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_FILTERS.value,
        subcategory = SUBCATEGORY_FILTERS,
        titleRes = LR.string.notification_filters_title,
        messageRes = LR.string.notification_filters_message,
        dayOffset = 3,
    )

    object Themes : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_THEMES.value,
        subcategory = SUBCATEGORY_THEMES,
        titleRes = LR.string.notification_themes_title,
        messageRes = LR.string.notification_themes_message,
        dayOffset = 4,
    )

    object StaffPicks : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_STAFF_PICKS.value,
        subcategory = SUBCATEGORY_STAFF_PICKS,
        titleRes = LR.string.notification_staff_picks_title,
        messageRes = LR.string.notification_staff_picks_message,
        dayOffset = 5,
    )

    object PlusUpsell : OnboardingNotificationType(
        notificationId = NotificationId.ONBOARDING_UPSELL.value,
        subcategory = SUBCATEGORY_PLUS_UP_SELL,
        titleRes = LR.string.notification_plus_upsell_title,
        messageRes = LR.string.notification_plus_upsell_message,
        dayOffset = 6,
    )

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
