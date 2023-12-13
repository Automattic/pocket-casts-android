package au.com.shiftyjelly.pocketcasts.search.searchhistory

import android.content.Context
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class SearchHistoryClearAllConfirmationDialog(
    context: Context,
    onConfirm: () -> Unit,
) : ConfirmationDialog() {
    init {
        setTitle(context.getString(LR.string.clear_all))
        setSummary(context.getString(LR.string.search_history_clear_all_confirmation_message))
        setIconId(IR.drawable.ic_delete)
        setButtonType(ButtonType.Danger(context.getString(LR.string.search_history_clear_all_confirm_button_title)))
        setOnConfirm { onConfirm() }
    }
}
