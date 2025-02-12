package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SuggestedFolders : BaseDialogFragment() {

    private val viewModel: SuggestedFoldersViewModel by viewModels<SuggestedFoldersViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            SuggestedFoldersPage(
                onShown = {
                    viewModel.onShown()
                },
                onDismiss = {
                    viewModel.onDismissed()
                    dismiss()
                },
                onUseTheseFolders = {
                    viewModel.onUseTheseFolders()
                },
                onCreateCustomFolders = {
                    viewModel.onCreateCustomFolders()
                    FolderCreateFragment.newInstance(source = "suggested_folders").show(parentFragmentManager, "create_folder_card")
                    dismiss()
                },
            )
        }
    }
}
