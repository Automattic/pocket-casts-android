package au.com.shiftyjelly.pocketcasts.profile.winback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.settings.HelpPage
import au.com.shiftyjelly.pocketcasts.settings.LogsPage
import au.com.shiftyjelly.pocketcasts.settings.status.StatusPage
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WinbackFragment : BaseDialogFragment() {
    private val viewModel by viewModels<WinbackViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val state by viewModel.uiState.collectAsState()

        AppThemeWithBackground(
            themeType = theme.activeTheme,
            backgroundColor = { MaterialTheme.theme.colors.primaryUi04 },
        ) {
            val navController = rememberNavController()

            DialogTintEffect(navController)

            NavHost(
                navController = navController,
                startDestination = WinbackNavRoutes.WinbackOffer,
                enterTransition = { slideInToStart() },
                exitTransition = { slideOutToStart() },
                popEnterTransition = { slideInToEnd() },
                popExitTransition = { slideOutToEnd() },
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(WinbackNavRoutes.WinbackOffer) {
                    WinbackOfferPage(
                        onClaimOffer = {
                            navController.navigate(WinbackNavRoutes.OfferClaimed) {
                                popUpTo(WinbackNavRoutes.WinbackOffer) {
                                    inclusive = true
                                }
                            }
                        },
                        onSeeAvailablePlans = { navController.navigate(WinbackNavRoutes.AvailablePlans) },
                        onSeeHelpAndFeedback = { navController.navigate(WinbackNavRoutes.HelpAndFeedback) },
                        onContinueToCancellation = { navController.navigate(WinbackNavRoutes.CancelConfirmation) },
                    )
                }
                composable(WinbackNavRoutes.OfferClaimed) {
                    OfferClaimedPage(
                        theme = theme.activeTheme,
                        onConfirm = { dismiss() },
                    )
                }
                composable(WinbackNavRoutes.AvailablePlans) {
                    AvailablePlansPage(
                        plansState = state.subscriptionPlansState,
                        onSelectPlan = { },
                        onReload = { viewModel.loadInitialPlans() },
                        onGoBack = { navController.popBackStack() },
                    )
                }
                composable(WinbackNavRoutes.HelpAndFeedback) {
                    HelpPage(
                        activity = requireActivity(),
                        onShowLogs = { navController.navigate(WinbackNavRoutes.SupportLogs) },
                        onShowStatusPage = { navController.navigate(WinbackNavRoutes.StatusCheck) },
                        onGoBack = { navController.popBackStack() },
                    )
                }
                composable(WinbackNavRoutes.SupportLogs) {
                    LogsPage(
                        bottomInset = 0.dp,
                        onBackPressed = { navController.popBackStack() },
                    )
                }
                composable(WinbackNavRoutes.StatusCheck) {
                    StatusPage(
                        bottomInset = 0.dp,
                        onBackPressed = { navController.popBackStack() },
                    )
                }
                composable(WinbackNavRoutes.CancelConfirmation) {
                    CancelConfirmationPage(
                        onKeepSubscription = { dismiss() },
                        onCancelSubscription = { Toast.makeText(requireActivity(), "Go to Play Store subscription", Toast.LENGTH_LONG).show() },
                    )
                }
            }
        }
    }

    @Composable
    private fun DialogTintEffect(
        navController: NavHostController,
    ) {
        var isBackgroundStyled by remember { mutableStateOf(false) }
        var isNavBarWhite by remember { mutableStateOf(false) }
        LaunchedEffect(navController) {
            navController.currentBackStackEntryFlow.collect { entry ->
                isBackgroundStyled = entry.destination.route in routesWithAppBar
                isNavBarWhite = entry.destination.route == WinbackNavRoutes.HelpAndFeedback
            }
        }
        val backgroundTint by animateColorAsState(
            animationSpec = colorAnimationSpec,
            targetValue = with(MaterialTheme.theme.colors) { if (isBackgroundStyled) secondaryUi01 else primaryUi04 },
        )
        val navigationBarTint by animateColorAsState(
            animationSpec = colorAnimationSpec,
            targetValue = with(MaterialTheme.theme.colors) { if (isNavBarWhite) Color.White else primaryUi04 },
        )
        LaunchedEffect(Unit) {
            launch {
                snapshotFlow { backgroundTint }.collect { tint -> setBackgroundTint(tint.toArgb()) }
            }
            launch {
                snapshotFlow { navigationBarTint }.collect { tint -> setNavigationBarTint(tint.toArgb()) }
            }
        }
    }
}

private object WinbackNavRoutes {
    const val WinbackOffer = "WinbackOffer"
    const val OfferClaimed = "OfferClaimed"
    const val AvailablePlans = "AvailablePlans"
    const val HelpAndFeedback = "HelpAndFeedback"
    const val SupportLogs = "SupportLogs"
    const val StatusCheck = "StatusCheck"
    const val CancelConfirmation = "CancelConfirmation"
}

private val routesWithAppBar = listOf(
    WinbackNavRoutes.HelpAndFeedback,
    WinbackNavRoutes.SupportLogs,
    WinbackNavRoutes.StatusCheck,
)

private val colorAnimationSpec = tween<Color>(350)
private val intOffsetAnimationSpec = tween<IntOffset>(350)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInToStart() = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Start,
    animationSpec = intOffsetAnimationSpec,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToStart() = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Start,
    animationSpec = intOffsetAnimationSpec,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInToEnd() = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.End,
    animationSpec = intOffsetAnimationSpec,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToEnd() = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.End,
    animationSpec = intOffsetAnimationSpec,
)
