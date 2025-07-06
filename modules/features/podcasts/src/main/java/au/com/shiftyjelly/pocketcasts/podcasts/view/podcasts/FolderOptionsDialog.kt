package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class FolderOptionsDialog(
    val folder: Folder,
    val onOpenSortOptions: () -> Unit,
    val onSortTypeChanged: (PodcastsSortType) -> Unit,
    val onEditFolder: () -> Unit,
    val onAddOrRemovePodcast: () -> Unit,
    val fragment: Fragment,
    val settings: Settings,
) {
    companion object {
        private const val FRAGMENT_FOLDER_OPTIONS_DIALOG = "folder_options_dialog"
        private const val FRAGMENT_FOLDER_SORT_DIALOG = "folder_sort_dialog"
    }

    private val showDialog: OptionsDialog
    private val sortDialog: OptionsDialog
    private val fragmentManager: FragmentManager?
        get() = fragment.activity?.supportFragmentManager

    init {
        showDialog = (fragmentManager?.findFragmentByTag(FRAGMENT_FOLDER_OPTIONS_DIALOG) as? OptionsDialog ?: OptionsDialog())
            .addTextOption(
                titleId = LR.string.podcasts_menu_sort_by,
                imageId = IR.drawable.ic_sort,
                valueId = folder.podcastsSortType.labelId,
                click = {
                    onOpenSortOptions()
                    openSortOptions()
                },
            )
            .addTextOption(
                titleId = LR.string.edit_folder,
                imageId = R.drawable.ic_pencil_edit,
                click = {
                    onEditFolder()
                },
            )
            .addTextOption(
                titleId = LR.string.add_or_remove_podcasts,
                imageId = R.drawable.ic_podcasts,
                click = {
                    onAddOrRemovePodcast()
                },
            )

        val podcastsSortType = folder.podcastsSortType
        sortDialog = (fragmentManager?.findFragmentByTag(FRAGMENT_FOLDER_SORT_DIALOG) as? OptionsDialog ?: OptionsDialog())
        sortDialog.setTitle(fragment.getString(LR.string.sort_by))
        for (sortType in PodcastsSortType.entries) {
            sortDialog.addCheckedOption(
                titleId = sortType.labelId,
                checked = sortType == podcastsSortType,
                click = {
                    onSortTypeChanged(sortType)
                },
            )
        }
    }

    fun show() {
        fragmentManager?.let {
            showDialog.show(it, FRAGMENT_FOLDER_OPTIONS_DIALOG)
        }
    }

    private fun openSortOptions() {
        fragmentManager?.let {
            sortDialog.show(it, FRAGMENT_FOLDER_SORT_DIALOG)
        }
    }
}
