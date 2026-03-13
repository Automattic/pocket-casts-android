package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.FolderDeletedEvent
import com.automattic.eventhorizon.FolderEditDeleteButtonTappedEvent
import com.automattic.eventhorizon.FolderEditDismissedEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class FolderEditFragment : BaseDialogFragment() {

    @Inject lateinit var eventHorizon: EventHorizon

    private val viewModel: FolderEditViewModel by viewModels()

    companion object {
        private const val ARG_FOLDER_UUID = "ARG_FOLDER_UUID"

        fun newInstance(folderUuid: String): FolderEditFragment {
            return FolderEditFragment().apply {
                arguments = bundleOf(
                    ARG_FOLDER_UUID to folderUuid,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString(FolderEditPodcastsFragment.ARG_FOLDER_UUID)?.let { folderUuid ->
            viewModel.setFolderUuid(folderUuid)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            FolderEditPage(
                viewModel = viewModel,
                onDeleteClick = { confirmFolderDelete() },
                onBackPress = { dismiss() },
            )
        }
    }

    private fun confirmFolderDelete() {
        eventHorizon.track(FolderEditDeleteButtonTappedEvent)
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.delete_folder)))
            .setTitle(getString(LR.string.are_you_sure))
            .setSummary(getString(LR.string.delete_folder_question))
            .setOnConfirm {
                eventHorizon.track(FolderDeletedEvent)
                viewModel.deleteFolder {
                    (activity as FragmentHostListener).closePodcastsToRoot()
                    dismiss()
                }
            }
            .setIconId(VR.drawable.ic_delete)
            .setIconTint(UR.attr.support_05)
            .show(childFragmentManager, "delete_folder_warning")
    }

    override fun onDismiss(dialog: DialogInterface) {
        eventHorizon.track(
            FolderEditDismissedEvent(
                didChangeName = viewModel.isNameChanged,
                didChangeColor = viewModel.isColorIdChanged,
            ),
        )
        super.onDismiss(dialog)
    }
}
