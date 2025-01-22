package au.com.shiftyjelly.pocketcasts.profile.winback

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.HelpPage
import au.com.shiftyjelly.pocketcasts.settings.LogsPage
import au.com.shiftyjelly.pocketcasts.settings.status.StatusPage
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class WinbackFragment : BaseDialogFragment() {
    private val viewModel by viewModels<WinbackViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val scope = rememberCoroutineScope()
        val state by viewModel.uiState.collectAsState()

        AppThemeWithBackground(
            themeType = theme.activeTheme,
        ) {
            val navController = rememberNavController()
            val snackbarHostState = remember { SnackbarHostState() }

            Box {
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
                            onSelectPlan = { plan -> viewModel.changePlan(requireActivity() as AppCompatActivity, plan) },
                            onGoToSubscriptions = {
                                if (!goToPlayStoreSubscriptions()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(getString(LR.string.error_generic_message))
                                    }
                                }
                            },
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
                            expirationDate = state.currentSubscriptionExpirationDate,
                            onKeepSubscription = { dismiss() },
                            onCancelSubscription = {
                                if (handleSubscriptionCancellation(state.purchasedProductIds)) {
                                    dismiss()
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(getString(LR.string.error_generic_message))
                                    }
                                }
                            },
                        )
                    }
                }

                DialogTintEffect(navController)

                val hasPlanChangeFailed = (state.subscriptionPlansState as? SubscriptionPlansState.Loaded)?.hasPlanChangeFailed == true
                if (hasPlanChangeFailed) {
                    LaunchedEffect(Unit) {
                        snackbarHostState.showSnackbar(getString(LR.string.error_generic_message))
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { data ->
                        val isLightTheme = MaterialTheme.theme.isLight
                        Snackbar(
                            backgroundColor = if (isLightTheme) Color.Black else Color.White,
                            content = { TextH50(data.message, color = if (isLightTheme) Color.White else Color.Black) },
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }

    @Composable
    private fun DialogTintEffect(
        navController: NavHostController,
    ) {
        var isNavBarWhite by remember { mutableStateOf(false) }
        LaunchedEffect(navController) {
            navController.currentBackStackEntryFlow.collect { entry ->
                isNavBarWhite = entry.destination.route == WinbackNavRoutes.HelpAndFeedback
            }
        }
        val navigationBarTint by animateColorAsState(
            animationSpec = colorAnimationSpec,
            targetValue = if (isNavBarWhite) Color.White else MaterialTheme.theme.colors.primaryUi01,
        )
        LaunchedEffect(Unit) {
            snapshotFlow { navigationBarTint }.collect { tint -> setNavigationBarTint(tint.toArgb()) }
        }
    }

    private fun handleSubscriptionCancellation(productIds: List<String>): Boolean {
        return if (productIds.isNotEmpty()) {
            goToPlayStoreSubscriptions(productIds.singleOrNull())
        } else {
            WebViewActivity.show(
                context,
                resources.getString(LR.string.winback_cancel_subscription_cancel_button_label),
                Settings.INFO_CANCEL_URL,
            )
            true
        }
    }

    private fun goToPlayStoreSubscriptions(sku: String? = null): Boolean {
        val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
            .buildUpon()
            .let { builder ->
                if (sku != null) {
                    builder.appendQueryParameter("sku", sku)
                } else {
                    builder
                }
            }
            .appendQueryParameter("package", requireContext().packageName)
            .build()
        return runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }.isSuccess
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
