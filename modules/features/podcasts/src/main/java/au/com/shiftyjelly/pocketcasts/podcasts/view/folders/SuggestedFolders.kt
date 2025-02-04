package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SuggestedFolders : BaseDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            SuggestedFoldersPage(
                onDismiss = {
                    dismiss()
                },
                onUseTheseFolders = {},
                onCreateCustomFolders = {
                    FolderCreateFragment().show(parentFragmentManager, "create_folder_card")
                },
            )
        }
    }
}
