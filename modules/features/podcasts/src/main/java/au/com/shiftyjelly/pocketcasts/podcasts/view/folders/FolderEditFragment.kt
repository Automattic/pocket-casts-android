package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class FolderEditFragment : BaseDialogFragment() {

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private val viewModel: FolderEditViewModel by viewModels()

    companion object {
        private const val ARG_FOLDER_UUID = "ARG_FOLDER_UUID"
        private const val DID_CHANGE_COLOR_KEY = "did_change_color"
        private const val DID_CHANGE_NAME_KEY = "did_change_name"

        fun newInstance(folderUuid: String): FolderEditFragment {
            return FolderEditFragment().apply {
                arguments = bundleOf(
                    ARG_FOLDER_UUID to folderUuid
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    FolderEditPage(
                        viewModel = viewModel,
                        onDeleteClick = { confirmFolderDelete() },
                        onBackClick = { dismiss() }
                    )
                }
            }
        }
    }

    private fun confirmFolderDelete() {
        analyticsTracker.track(AnalyticsEvent.FOLDER_EDIT_DELETE_BUTTON_TAPPED)
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.delete_folder)))
            .setTitle(getString(LR.string.are_you_sure))
            .setSummary(getString(LR.string.delete_folder_question))
            .setOnConfirm {
                analyticsTracker.track(AnalyticsEvent.FOLDER_DELETED)
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
        analyticsTracker.track(
            AnalyticsEvent.FOLDER_EDIT_DISMISSED,
            mapOf(
                DID_CHANGE_NAME_KEY to viewModel.isNameChanged,
                DID_CHANGE_COLOR_KEY to viewModel.isColorIdChanged
            )
        )
        super.onDismiss(dialog)
    }
}
