package au.com.shiftyjelly.pocketcasts.settings

import android.content.Context
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * A dialog that shows a Downloads - Cleanup confirmation dialog.
 */
class ManualCleanupConfirmationDialog(context: Context, onConfirm: () -> Unit) : ConfirmationDialog() {
    init {
        setTitle(context.getString(LR.string.settings_downloads_clean_up))
        setSummary(context.getString(LR.string.settings_downloads_clean_up_summary))
        setIconId(au.com.shiftyjelly.pocketcasts.views.R.drawable.ic_delete)
        setButtonType(ButtonType.Danger(context.getString(LR.string.delete)))
        setOnConfirm { onConfirm() }
    }
}
