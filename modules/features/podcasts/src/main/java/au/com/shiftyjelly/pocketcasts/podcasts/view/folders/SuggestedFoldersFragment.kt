package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as VR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class SuggestedFoldersFragment : BaseDialogFragment() {

    companion object {
        private const val ARGS_KEY = "args"

        fun newInstance(
            source: Source,
            folders: List<Folder>,
        ): SuggestedFoldersFragment {
            return SuggestedFoldersFragment().apply {
                arguments = bundleOf(
                    ARGS_KEY to Args(source, folders),
                )
            }
        }
    }

    private val viewModel by viewModels<SuggestedFoldersViewModel>()

    private val args
        get() = requireNotNull(BundleCompat.getParcelable(requireArguments(), ARGS_KEY, Args::class.java)) {
            "Missing input parameters"
        }

    private var isFinalizingActionUsed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        Box(
            modifier = Modifier
                .fillMaxHeight(0.93f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        ) {
            AppThemeWithBackground(theme.activeTheme) {
                val state by viewModel.state.collectAsState()

                LaunchedEffect(state) {
                    if (state is SuggestedFoldersViewModel.FoldersState.Created) {
                        finalizeAndDismiss()
                    } else if (state is SuggestedFoldersViewModel.FoldersState.ShowConfirmationDialog) {
                        showConfirmationDialog()
                    }
                }

                SuggestedFoldersPage(
                    folders = args.folders,
                    useWhiteColorForHowItWorks = theme.activeTheme == Theme.ThemeType.ELECTRIC,
                    onShown = {},
                    onDismiss = {
                        dismiss()
                    },
                    onUseTheseFolders = {
                        viewModel.onUseTheseFolders(args.folders)
                    },
                    onHowItWorks = {
                    },
                    onCreateCustomFolders = {
                        FolderCreateFragment.newInstance(source = "suggested_folders").show(parentFragmentManager, "create_folder_card")
                        finalizeAndDismiss()
                    },
                )
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (isDismissingPopupWithoutAction()) {
            viewModel.markPopupAsDismissed()
        }
    }

    private fun finalizeAndDismiss() {
        isFinalizingActionUsed = true
        dismiss()
    }

    private fun isDismissingPopupWithoutAction() =
        !requireActivity().isChangingConfigurations &&
            !isFinalizingActionUsed &&
            args.source == Source.PodcastsPopup

    private fun showConfirmationDialog() {
        viewModel.onReplaceExistingFoldersShown()
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.suggested_folders_replace_folders_button)))
            .setTitle(getString(LR.string.suggested_folders_replace_folders_confirmation_tittle))
            .setSummary(getString(LR.string.suggested_folders_replace_folders_confirmation_description))
            .setOnConfirm {
                viewModel.overrideFoldersWithSuggested(args.folders)
            }
            .setIconId(VR.drawable.ic_replace)
            .setIconTint(UR.attr.primary_interactive_01)
            .show(childFragmentManager, "suggested-folders-confirmation-dialog")
    }

    enum class Source {
        PodcastsPopup,
        CreateFolderButton,
    }

    @Parcelize
    private class Args(
        val source: SuggestedFoldersFragment.Source,
        val folders: List<Folder>,
    ) : Parcelable
}
