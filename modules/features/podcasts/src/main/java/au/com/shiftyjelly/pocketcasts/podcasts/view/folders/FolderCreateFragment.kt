package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.AnalyticsHelper
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FolderCreateFragment : BaseDialogFragment() {

    @Inject lateinit var settings: Settings
    private val viewModel: FolderEditViewModel by viewModels()
    private val sharedViewModel: FolderCreateSharedViewModel by activityViewModels()
    private var navHostController: NavHostController? = null

    private object NavRoutes {
        const val podcasts = "folder_podcasts"
        const val name = "folder_name"
        const val color = "folder_color"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    navHostController = rememberNavController()
                    val navController = navHostController ?: return@AppTheme
                    NavHost(navController = navController, startDestination = NavRoutes.podcasts) {
                        composable(NavRoutes.podcasts) {
                            FolderEditPodcastsPage(
                                onCloseClick = { dismiss() },
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
                                    viewModel.saveFolder(resources = resources) { folder ->
                                        sharedViewModel.folderUuid = folder.uuid
                                        AnalyticsHelper.folderCreated()
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
        return addNavControllerToBackStack(loadNavController = { navHostController }, initialRoute = NavRoutes.podcasts)
    }
}
