package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.fragment.app.FragmentActivity
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class PodcastFolderOptionsDialog(
    val folder: Folder,
    val onRemoveFolder: () -> Unit,
    val onChangeFolder: () -> Unit,
    val onOpenFolder: () -> Unit,
    val activity: FragmentActivity?
) {

    private var showDialog: OptionsDialog? = null

    fun show() {
        val dialog = OptionsDialog()
            .setTitle(folder.name)
            .addTextOption(
                titleId = LR.string.remove_from_folder,
                imageId = IR.drawable.folder_remove,
                click = {
                    onRemoveFolder()
                    dismiss()
                }
            )
            .addTextOption(
                titleId = LR.string.change_folder,
                imageId = IR.drawable.folder_change,
                click = {
                    onChangeFolder()
                    dismiss()
                }
            )
            .addTextOption(
                titleId = LR.string.go_to_folder,
                imageId = IR.drawable.go_to,
                click = {
                    onOpenFolder()
                    dismiss()
                }
            )
        activity?.supportFragmentManager?.let {
            dialog.show(it, "podcast_folder_options_dialog")
            showDialog = dialog
        }
    }

    fun dismiss() {
        showDialog?.dismiss()
    }
}
