package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderEditViewModel.Companion.COLOR_KEY
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FolderCreateFragment : BaseDialogFragment() {

    @Inject lateinit var settings: Settings
    private val viewModel: FolderEditViewModel by viewModels()
    private val sharedViewModel: FolderCreateSharedViewModel by activityViewModels()
    private var navHostController: NavHostController? = null

    companion object {
        const val ARG_SOURCE = "ARG_SOURCE"

        fun newInstance(source: String): FolderCreateFragment {
            return FolderCreateFragment().apply {
                arguments = bundleOf(
                    ARG_SOURCE to source,
                )
            }
        }
    }

    private object NavRoutes {
        const val PODCASTS = "folder_podcasts"
        const val NAME = "folder_name"
        const val COLOR = "folder_color"
    }

    private val source: String
        get() = arguments?.getString(ARG_SOURCE) ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            CallOnce {
                viewModel.trackShown(source)
            }

            navHostController = rememberNavController()
            val navController = navHostController ?: return@AppThemeWithBackground
            NavHost(navController = navController, startDestination = NavRoutes.PODCASTS) {
                composable(NavRoutes.PODCASTS) {
                    FolderEditPodcastsPage(
                        onCloseClick = { dismiss() },
                        onNextClick = {
                            viewModel.trackCreateFolderNavigation(AnalyticsEvent.FOLDER_CREATE_NAME_SHOWN)
                            navController.navigate(NavRoutes.NAME)
                        },
                        viewModel = viewModel,
                        settings = settings,
                        fragmentManager = parentFragmentManager,
                    )
                }
                composable(NavRoutes.NAME) {
                    FolderEditNamePage(
                        onBackPress = { navController.popBackStack() },
                        onNextClick = {
                            viewModel.trackCreateFolderNavigation(AnalyticsEvent.FOLDER_CREATE_COLOR_SHOWN)
                            navController.navigate(NavRoutes.COLOR)
                        },
                        viewModel = viewModel,
                    )
                }
                composable(NavRoutes.COLOR) {
                    val colors = MaterialTheme.theme.colors
                    FolderEditColorPage(
                        onBackPress = { navController.popBackStack() },
                        onSaveClick = {
                            viewModel.saveFolder(resources = resources) { folder ->
                                sharedViewModel.folderUuid = folder.uuid
                                val colorHex = ColorUtils.colorIntToHexString(colors.getFolderColor(folder.color).toArgb())
                                viewModel.trackCreateFolderNavigation(AnalyticsEvent.FOLDER_SAVED, mapOf(COLOR_KEY to colorHex))
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
