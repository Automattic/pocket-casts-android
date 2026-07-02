package au.com.shiftyjelly.pocketcasts.profile.sharing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareProfileFragment : BaseFragment() {

    private val viewModel: ShareProfileViewModel by viewModels()
    private var navHostController: NavHostController? = null

    private object NavRoutes {
        const val DISPLAY_NAME = "display_name"
        const val PREVIEW = "preview"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            navHostController = rememberNavController()
            val navController = navHostController ?: return@AppThemeWithBackground
            NavHost(
                navController = navController,
                startDestination = NavRoutes.DISPLAY_NAME,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            ) {
                composable(NavRoutes.DISPLAY_NAME) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    ShareProfileNamePage(
                        state = state,
                        onBackPress = { activity?.finish() },
                        onDisplayNameChange = { displayName ->
                            viewModel.setDisplayName(displayName)
                        },
                        onContinueClick = {
                            navController.navigate(NavRoutes.PREVIEW)
                        },
                    )
                }
                composable(NavRoutes.PREVIEW) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    ShareProfilePreviewPage(
                        state = state,
                        onBackPress = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
