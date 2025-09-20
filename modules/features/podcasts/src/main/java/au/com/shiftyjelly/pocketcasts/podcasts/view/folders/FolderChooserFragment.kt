package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FolderChooserFragment : BaseDialogFragment() {

    @Inject lateinit var settings: Settings

    @Inject lateinit var analyticsTracker: AnalyticsTracker
    private val viewModel: FolderEditViewModel by viewModels()
    private var navHostController: NavHostController? = null
    private var podcastUuid: String? = null

    private object NavRoutes {
        const val FOLDERS = "folder_chooser"
        const val PODCASTS = "folder_podcasts"
        const val NAME = "folder_name"
        const val COLOR = "folder_color"
    }

    companion object {
        const val ARG_PODCAST_UUID = "ARG_PODCAST_UUID"

        fun newInstance(podcastUuid: String): FolderChooserFragment {
            return FolderChooserFragment().apply {
                arguments = bundleOf(
                    ARG_PODCAST_UUID to podcastUuid,
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            navHostController = rememberNavController()
            val navController = navHostController ?: return@AppThemeWithBackground
            NavHost(navController = navController, startDestination = NavRoutes.FOLDERS) {
                composable(NavRoutes.FOLDERS) {
                    FolderChooserPage(
                        podcastUuid = podcastUuid,
                        onCloseClick = { dismiss() },
                        onNewFolderClick = {
                            val uuid = podcastUuid
                            if (uuid != null) {
                                viewModel.addPodcast(uuid)
                                viewModel.removePodcastFromFolder(uuid)
                                navController.navigate(NavRoutes.PODCASTS)
                            }
                        },
                        viewModel = viewModel,
                    )
                }
                composable(NavRoutes.PODCASTS) {
                    FolderEditPodcastsPage(
                        onCloseClick = { navController.popBackStack() },
                        navigationButton = NavigationButton.Back,
                        onNextClick = { navController.navigate(NavRoutes.NAME) },
                        viewModel = viewModel,
                        settings = settings,
                        fragmentManager = parentFragmentManager,
                    )
                }
                composable(NavRoutes.NAME) {
                    FolderEditNamePage(
                        onBackPress = { navController.popBackStack() },
                        onNextClick = { navController.navigate(NavRoutes.COLOR) },
                        viewModel = viewModel,
                    )
                }
                composable(NavRoutes.COLOR) {
                    FolderEditColorPage(
                        onBackPress = { navController.popBackStack() },
                        onSaveClick = {
                            viewModel.saveFolder(resources = resources) {
                                dismiss()
                            }
                        },
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}
