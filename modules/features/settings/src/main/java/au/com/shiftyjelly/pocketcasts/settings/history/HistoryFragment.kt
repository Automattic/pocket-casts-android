package au.com.shiftyjelly.pocketcasts.settings.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HistoryFragment : BaseFragment(), HasBackstack {

    @Inject
    lateinit var settings: Settings
    private lateinit var navController: NavHostController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)
            val bottomInsetDp = bottomInset.value.pxToDp(LocalContext.current).dp
            navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = HistoryNavRoutes.History,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(HistoryNavRoutes.History) {
                    HistoryPage(
                        onBackClick = {
                            navController.popBackStack()
                            onBackClick()
                        },
                        onUpNextHistoryClick = {
                        },
                        bottomInset = bottomInsetDp,
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun onBackClick() {
        activity?.onBackPressed()
    }

    override fun onBackPressed() =
        if (navController.currentDestination?.route == HistoryNavRoutes.History) {
            super.onBackPressed()
        } else {
            navController.popBackStack()
        }

    object HistoryNavRoutes {
        const val History = "main"
    }
}
