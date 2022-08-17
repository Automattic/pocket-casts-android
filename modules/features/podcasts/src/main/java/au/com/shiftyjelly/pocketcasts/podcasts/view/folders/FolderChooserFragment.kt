package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FolderChooserFragment : BaseDialogFragment() {

    @Inject lateinit var settings: Settings
    private val viewModel: FolderEditViewModel by viewModels()
    private var navHostController: NavHostController? = null
    private var podcastUuid: String? = null

    private object NavRoutes {
        const val folders = "folder_chooser"
        const val podcasts = "folder_podcasts"
        const val name = "folder_name"
        const val color = "folder_color"
    }

    companion object {
        const val ARG_PODCAST_UUID = "ARG_PODCAST_UUID"

        fun newInstance(podcastUuid: String): FolderChooserFragment {
            return FolderChooserFragment().apply {
                arguments = bundleOf(
                    ARG_PODCAST_UUID to podcastUuid
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        podcastUuid = arguments?.getString(ARG_PODCAST_UUID)?.apply {
            viewModel.loadFolderForPodcast(this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    navHostController = rememberNavController()
                    val navController = navHostController ?: return@AppThemeWithBackground
                    NavHost(navController = navController, startDestination = NavRoutes.folders) {
                        composable(NavRoutes.folders) {
                            FolderChooserPage(
                                podcastUuid = podcastUuid,
                                onCloseClick = { dismiss() },
                                onNewFolderClick = {
                                    val uuid = podcastUuid
                                    if (uuid != null) {
                                        viewModel.addPodcast(uuid)
                                        viewModel.removePodcastFromFolder(uuid)
                                        navController.navigate(NavRoutes.podcasts)
                                    }
                                },
                                viewModel = viewModel,
                            )
                        }
                        composable(NavRoutes.podcasts) {
                            FolderEditPodcastsPage(
                                onCloseClick = { navController.popBackStack() },
                                navigationButton = NavigationButton.Back,
                                onNextClick = { navController.navigate(NavRoutes.name) },
                                viewModel = viewModel,
                                settings = settings,
                                fragmentManager = parentFragmentManager
                            )
                        }
                        composable(NavRoutes.name) {
                            FolderEditNamePage(
                                onBackClick = { navController.popBackStack() },
                                onNextClick = { navController.navigate(NavRoutes.color) },
                                viewModel = viewModel
                            )
                        }
                        composable(NavRoutes.color) {
                            FolderEditColorPage(
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = {
                                    viewModel.saveFolder(resources = resources) {
                                        dismiss()
                                    }
                                },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return addNavControllerToBackStack(loadNavController = { navHostController }, initialRoute = NavRoutes.folders)
    }
}
