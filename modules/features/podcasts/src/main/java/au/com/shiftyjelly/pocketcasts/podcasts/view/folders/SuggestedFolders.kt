package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as VR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class SuggestedFolders : BaseFragment() {

    companion object {
        private const val FOLDERS_KEY = "folders_key"

        fun newInstance(folders: List<Folder>): SuggestedFolders {
            return SuggestedFolders().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(FOLDERS_KEY, ArrayList(folders))
                }
            }
        }
    }

    private val viewModel: SuggestedFoldersViewModel by viewModels<SuggestedFoldersViewModel>()

    private val suggestedFolders
        get() = requireNotNull(BundleCompat.getParcelableArrayList(requireArguments(), FOLDERS_KEY, Folder::class.java)) {
            "Missing input parameters"
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            val state by viewModel.state.collectAsState()

            LaunchedEffect(state) {
                if (state is SuggestedFoldersViewModel.FoldersState.Created) {
                    (activity as FragmentHostListener).closeModal(this@SuggestedFolders)
                } else if (state is SuggestedFoldersViewModel.FoldersState.ShowConfirmationDialog) {
                    showConfirmationDialog()
                }
            }

            SuggestedFoldersPage(
                folders = suggestedFolders,
                onShown = {
                    viewModel.onShown()
                },
                onDismiss = {
                    viewModel.onDismissed()
                    (activity as FragmentHostListener).closeModal(this)
                },
                onUseTheseFolders = {
                    viewModel.onUseTheseFolders(suggestedFolders)
                },
                onHowItWorks = {
                    viewModel.onHowItWorksTapped()
                    showHowItWorksDialog()
                },
                onCreateCustomFolders = {
                    viewModel.onCreateCustomFolders()
                    FolderCreateFragment.newInstance(source = "suggested_folders").show(parentFragmentManager, "create_folder_card")
                    (activity as FragmentHostListener).closeModal(this)
                },
            )
        }
    }

    private fun showConfirmationDialog() {
        viewModel.onReplaceExistingFoldersShown()
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.suggested_folders_replace_folders_button)))
            .setTitle(getString(LR.string.suggested_folders_replace_folders_confirmation_tittle))
            .setSummary(getString(LR.string.suggested_folders_replace_folders_confirmation_description))
            .setOnConfirm {
                viewModel.onReplaceExistingFoldersTapped()
                viewModel.overrideFoldersWithSuggested(suggestedFolders)
            }
            .setIconId(VR.drawable.ic_replace)
            .setIconTint(UR.attr.primary_interactive_01)
            .show(childFragmentManager, "suggested-folders-confirmation-dialog")
    }

    private fun showHowItWorksDialog() {
        val dialog = ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.got_it)))
            .setIconId(VR.drawable.ic_folder_plus)
            .setTitle(getString(LR.string.suggested_folders_how_it_works))
            .setSummary(getString(LR.string.suggested_folders_how_it_works_description))
            .setOnConfirm { viewModel.onHowItWorksGotItTapped() }
        dialog.show(parentFragmentManager, "suggested-folders-how-it-works-dialog")
    }
}
