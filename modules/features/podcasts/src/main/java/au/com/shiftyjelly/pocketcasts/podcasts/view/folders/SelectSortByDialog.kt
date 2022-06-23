package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * Sort order dialog used when picking podcasts from their collection. For example which podcasts are in a folder.
 */
class SelectSortByDialog(val settings: Settings) {

    private var optionsDialog: OptionsDialog? = null

    fun show(context: Context, fragmentManager: FragmentManager) {
        val sortOrder = settings.getSelectPodcastsSortType()
        val dialog = OptionsDialog().setTitle(context.resources.getString(LR.string.sort_by))
        for (order in PodcastsSortType.values()) {
            if (order == PodcastsSortType.DRAG_DROP) {
                // sorting by drag and drop doesn't make sense on this podcast picking page
                continue
            }
            dialog.addCheckedOption(
                titleId = order.labelId,
                checked = order.clientId == sortOrder.clientId,
                click = { settings.setSelectPodcastsSortType(order) }
            )
        }
        dialog.show(fragmentManager, "select_podcasts_sort_dialog")
        optionsDialog = dialog
    }

    fun dismiss() {
        optionsDialog?.dismiss()
    }
}
