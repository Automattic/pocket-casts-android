package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FolderEditPodcastsFragment : BaseDialogFragment() {

    @Inject lateinit var settings: Settings
    private val viewModel: FolderEditViewModel by viewModels()

    companion object {
        const val ARG_FOLDER_UUID = "ARG_FOLDER_UUID"

        fun newInstance(folderUuid: String): FolderEditPodcastsFragment {
            return FolderEditPodcastsFragment().apply {
                arguments = bundleOf(
                    ARG_FOLDER_UUID to folderUuid
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString(ARG_FOLDER_UUID)?.let { folderUuid ->
            viewModel.setFolderUuid(folderUuid)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    FolderEditPodcastsPage(
                        onCloseClick = { dismiss() },
                        onNextClick = {
                            viewModel.saveFolderPodcasts {
                                dismiss()
                            }
                        },
                        viewModel = viewModel,
                        settings = settings,
                        fragmentManager = parentFragmentManager
                    )
                }
            }
        }
    }
}
