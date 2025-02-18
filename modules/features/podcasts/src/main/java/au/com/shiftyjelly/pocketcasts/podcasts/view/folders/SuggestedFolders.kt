package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SuggestedFolders : BaseDialogFragment() {

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

    val suggestedFolders: ArrayList<Folder>
        get() = arguments?.let { (BundleCompat.getParcelableArrayList(it, FOLDERS_KEY, Folder::class.java)) } ?: ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            SuggestedFoldersPage(
                folders = suggestedFolders,
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
