package au.com.shiftyjelly.pocketcasts.settings

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.isDeviceRunningOnLowStorage
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.FreeUpSpaceManageDownloadsTappedEvent
import com.automattic.eventhorizon.FreeUpSpaceMaybeLaterTappedEvent
import com.automattic.eventhorizon.FreeUpSpaceModalShownEvent
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class LowStorageDialogPresenter(
    val context: Context,
    val eventHorizon: EventHorizon,
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

        eventHorizon.track(
            FreeUpSpaceModalShownEvent(
                source = sourceView.eventHorizonValue,
            ),
        )

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
                eventHorizon.track(
                    FreeUpSpaceManageDownloadsTappedEvent(
                        source = sourceView.eventHorizonValue,
                    ),
                )
                onManageDownloadsClick.invoke()
            }
            .setOnSecondary {
                eventHorizon.track(
                    FreeUpSpaceMaybeLaterTappedEvent(
                        source = sourceView.eventHorizonValue,
                    ),
                )
                settings.setDismissLowStorageModalTime(System.currentTimeMillis())
            }
    }
}
