package au.com.shiftyjelly.pocketcasts.settings

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.isDeviceRunningOnLowStorage
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class LowStorageDialogPresenter(
    val context: Context,
    val analyticsTracker: AnalyticsTracker,
    val settings: Settings,
) {

    suspend fun shouldShow(downloadedFiles: Long): Boolean = isDeviceRunningOnLowStorage() &&
        downloadedFiles != 0L &&
        settings.shouldShowLowStorageModalAfterSnooze()

    fun getDialog(
        totalDownloadSize: Long,
        sourceView: SourceView,
        onManageDownloadsClick: () -> Unit,
    ): ConfirmationDialog {
        val formattedTotalDownloadSize = Util.formattedBytes(bytes = totalDownloadSize, context = context)

        analyticsTracker.track(AnalyticsEvent.FREE_UP_SPACE_MODAL_SHOWN, mapOf("source" to sourceView.analyticsValue))

        return ConfirmationDialog()
            .setTitle(context.getString(LR.string.need_to_free_up_space))
            .setSummary(context.getString(LR.string.save_space_by_managing_downloaded_episodes, formattedTotalDownloadSize))
            .setSummaryTextColor(UR.attr.primary_text_01)
            .setSummaryTextSize(14f)
            .setButtonType(ConfirmationDialog.ButtonType.Normal(context.getString(LR.string.manage_downloads)))
            .setSecondaryButtonType(ConfirmationDialog.ButtonType.Normal(context.getString(LR.string.maybe_later)))
            .setSecondaryTextColor(UR.attr.primary_text_01)
            .setIconId(IR.drawable.pencil_cleanup)
            .setIconTint(UR.attr.primary_interactive_01)
            .setDisplayConfirmButtonFirst(true)
            .setRemoveSecondaryButtonBorder(true)
            .setOnConfirm {
                analyticsTracker.track(AnalyticsEvent.FREE_UP_SPACE_MANAGE_DOWNLOADS_TAPPED, mapOf("source" to sourceView.analyticsValue))
                onManageDownloadsClick.invoke()
            }
            .setOnSecondary {
                analyticsTracker.track(AnalyticsEvent.FREE_UP_SPACE_MAYBE_LATER_TAPPED, mapOf("source" to sourceView.analyticsValue))
                settings.setDismissLowStorageModalTime(System.currentTimeMillis())
            }
    }
}
