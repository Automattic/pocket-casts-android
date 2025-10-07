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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryDetailsPage
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryDetailsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryPage
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireLong
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class HistoryFragment :
    BaseFragment(),
    HasBackstack {

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
                startDestination = HistoryNavRoutes.HISTORY,
                enterTransition = { slideInToStart() },
                exitTransition = { slideOutToStart() },
                popEnterTransition = { slideInToEnd() },
                popExitTransition = { slideOutToEnd() },
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(HistoryNavRoutes.HISTORY) {
                    HistoryPage(
                        onBackPress = {
                            navController.popBackStack()
                            onBackPress()
                        },
                        onUpNextHistoryClick = {
                            navController.navigate(HistoryNavRoutes.UP_NEXT_HISTORY)
                        },
                        bottomInset = bottomInsetDp,
                    )
                }
                composable(HistoryNavRoutes.UP_NEXT_HISTORY) {
                    UpNextHistoryPage(
                        onHistoryEntryClick = { date ->
                            navController.navigate(
                                HistoryNavRoutes.upNextHistoryDetailsDestination(
                                    date.time,
                                ),
                            )
                        },
                        onBackPress = navController::popBackStack,
                        bottomInset = bottomInsetDp,
                    )
                }
                composable(
                    HistoryNavRoutes.upNextHistoryDetailsRoute(),
                    listOf(
                        navArgument(HistoryNavRoutes.UP_NEXT_HISTORY_DATE_ARGUMENT) {
                            type = NavType.LongType
                        },
                    ),
                ) { backStackEntry ->
                    val arguments = requireNotNull(backStackEntry.arguments) { "Missing back stack entry arguments" }
                    val date = arguments.requireLong(HistoryNavRoutes.UP_NEXT_HISTORY_DATE_ARGUMENT)

                    UpNextHistoryDetailsPage(
                        date = date,
                        onRestoreClick = ::onRestoreClick,
                        onBackPress = navController::popBackStack,
                        bottomInset = bottomInsetDp,
                    )
                }
            }
        }
    }

    private fun onRestoreClick(state: UiState, restoreUpNext: () -> Unit) {
        if (state !is UiState.Loaded) return
        if (state.isUpNextQueueEmpty) {
            restoreUpNext()
            return
        }
        showUpNextRestoreConfirmationDialog(restoreUpNext)
    }

    private fun showUpNextRestoreConfirmationDialog(onRestoreConfirm: () -> Unit) {
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.up_next_history_restore_button)))
            .setTitle(getString(LR.string.up_next_history_restore_confirmation_title))
            .setSummary(getString(LR.string.up_next_history_restore_confirmation_description))
            .setOnConfirm {
                onRestoreConfirm()
            }
            .setIconId(IR.drawable.ic_history)
            .setIconTint(UR.attr.primary_interactive_01)
            .show(childFragmentManager, "up-next-restore-confirmation-dialog")
    }

    @Suppress("DEPRECATION")
    private fun onBackPress() {
        activity?.onBackPressed()
    }

    override fun onBackPressed() = if (navController.currentDestination?.route == HistoryNavRoutes.HISTORY) {
        super.onBackPressed()
    } else {
        navController.popBackStack()
    }

    object HistoryNavRoutes {
        const val HISTORY = "main"
        const val UP_NEXT_HISTORY = "up_next_history"
        private const val UP_NEXT_HISTORY_DETAILS = "up_next_history_details"

        const val UP_NEXT_HISTORY_DATE_ARGUMENT = "upNextHistoryDate"
        fun upNextHistoryDetailsRoute() = "$UP_NEXT_HISTORY_DETAILS/{$UP_NEXT_HISTORY_DATE_ARGUMENT}"
        fun upNextHistoryDetailsDestination(date: Long) = "$UP_NEXT_HISTORY_DETAILS/$date"
    }
}
