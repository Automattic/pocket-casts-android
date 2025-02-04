package au.com.shiftyjelly.pocketcasts.profile.winback

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.BillingPeriod
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.HelpPage
import au.com.shiftyjelly.pocketcasts.settings.LogsPage
import au.com.shiftyjelly.pocketcasts.settings.status.StatusPage
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class WinbackFragment : BaseDialogFragment() {
    private val viewModel by viewModels<WinbackViewModel>()

    private val params
        get() = requireNotNull(BundleCompat.getParcelable(requireArguments(), INPUT_ARGS, WinbackInitParams::class.java)) {
            "Missing input parameters"
        }

    private var currentScreenId: String? = null

    override val includeNavigationBarPadding = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        LaunchedEffect(Unit) {
            setBackgroundTint(Color.Transparent.toArgb())
        }

        val scope = rememberCoroutineScope()
        val state by viewModel.uiState.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxHeight(0.93f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        ) {
            AppThemeWithBackground(
                themeType = theme.activeTheme,
            ) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                Box(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                        .padding(top = 8.dp),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = if (params.hasGoogleSubscription) {
                            WinbackNavRoutes.WinbackOffer
                        } else {
                            WinbackNavRoutes.CancelConfirmation
                        },
                        enterTransition = { slideInToStart() },
                        exitTransition = { slideOutToStart() },
                        popEnterTransition = { slideInToEnd() },
                        popExitTransition = { slideOutToEnd() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        composable(WinbackNavRoutes.WinbackOffer) {
                            WinbackOfferPage(
                                offer = state.winbackOfferState?.offer,
                                onClaimOffer = { offer ->
                                    viewModel.trackClaimOfferTapped()
                                    navController.navigate(WinbackNavRoutes.offerClaimedDestination(offer.details.billingPeriod)) {
                                        popUpTo(WinbackNavRoutes.WinbackOffer) {
                                            inclusive = true
                                        }
                                    }
                                },
                                onSeeAvailablePlans = {
                                    viewModel.trackAvailablePlansTapped()
                                    navController.navigate(WinbackNavRoutes.AvailablePlans)
                                },
                                onSeeHelpAndFeedback = {
                                    viewModel.trackHelpAndFeedbackTapped()
                                    navController.navigate(WinbackNavRoutes.HelpAndFeedback)
                                },
                                onContinueToCancellation = {
                                    viewModel.trackContinueCancellationTapped()
                                    navController.navigate(WinbackNavRoutes.CancelConfirmation)
                                },
                            )
                        }
                        composable(
                            WinbackNavRoutes.offerClaimedRoute(),
                            listOf(
                                navArgument(WinbackNavRoutes.OfferClaimedBillingPeriodArgument) {
                                    type = NavType.EnumType(BillingPeriod::class.java)
                                },
                            ),
                        ) { backStackEntry ->
                            val arguments = requireNotNull(backStackEntry.arguments) { "Missing back stack entry arguments" }
                            val billingPeriod = requireNotNull(BundleCompat.getSerializable(arguments, WinbackNavRoutes.OfferClaimedBillingPeriodArgument, BillingPeriod::class.java)) {
                                "Missing billing period argument"
                            }
                            OfferClaimedPage(
                                billingPeriod = billingPeriod,
                                onConfirm = {
                                    viewModel.trackOfferClaimedConfirmationTapped()
                                    dismiss()
                                },
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
                                onReload = { viewModel.loadWinbackData() },
                                onGoBack = {
                                    viewModel.trackPlansBackButtonTapped()
                                    navController.popBackStack()
                                },
                            )
                        }
                        composable(WinbackNavRoutes.HelpAndFeedback) {
                            HelpPage(
                                activity = requireActivity(),
                                appBarInsets = AppBarDefaults.topAppBarWindowInsets.only(WindowInsetsSides.Horizontal),
                                onShowLogs = { navController.navigate(WinbackNavRoutes.SupportLogs) },
                                onShowStatusPage = { navController.navigate(WinbackNavRoutes.StatusCheck) },
                                onGoBack = { navController.popBackStack() },
                            )
                        }
                        composable(WinbackNavRoutes.SupportLogs) {
                            LogsPage(
                                bottomInset = 0.dp,
                                appBarInsets = AppBarDefaults.topAppBarWindowInsets.only(WindowInsetsSides.Horizontal),
                                onBackPressed = { navController.popBackStack() },
                            )
                        }
                        composable(WinbackNavRoutes.StatusCheck) {
                            StatusPage(
                                bottomInset = 0.dp,
                                appBarInsets = AppBarDefaults.topAppBarWindowInsets.only(WindowInsetsSides.Horizontal),
                                onBackPressed = { navController.popBackStack() },
                            )
                        }
                        composable(WinbackNavRoutes.CancelConfirmation) {
                            CancelConfirmationPage(
                                expirationDate = state.currentSubscriptionExpirationDate,
                                onKeepSubscription = {
                                    viewModel.trackKeepSubscriptionTapped()
                                    dismiss()
                                },
                                onCancelSubscription = {
                                    viewModel.trackCancelSubscriptionTapped()
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

                    val hasPlanChangeFailed = (state.subscriptionPlansState as? SubscriptionPlansState.Loaded)?.hasPlanChangeFailed == true
                    if (hasPlanChangeFailed) {
                        LaunchedEffect(Unit) {
                            snackbarHostState.showSnackbar(getString(LR.string.error_generic_message))
                        }
                    }

                    LaunchedEffect(navController) {
                        navController.currentBackStackEntryFlow.collect { entry ->
                            val screenId = entry.destination.route
                                ?.substringBefore('/') // Track only the top part of the route
                                .also { currentScreenId = it }
                            if (screenId != null) {
                                viewModel.trackScreenShown(screenId)
                            }
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
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val screenId = currentScreenId
        if (screenId != null) {
            viewModel.trackScreenDismissed(screenId)
        }
    }

    private fun handleSubscriptionCancellation(productIds: List<String>): Boolean {
        return if (productIds.isNotEmpty() && params.hasGoogleSubscription) {
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

    companion object {
        private const val INPUT_ARGS = "WinbackFragment.Params"

        fun create(params: WinbackInitParams) = WinbackFragment().apply {
            arguments = bundleOf(INPUT_ARGS to params)
        }
    }
}

@Parcelize
data class WinbackInitParams(
    val hasGoogleSubscription: Boolean,
) : Parcelable {
    companion object {
        val Empty = WinbackInitParams(
            hasGoogleSubscription = false,
        )
    }
}

private object WinbackNavRoutes {
    const val WinbackOffer = "main"
    const val AvailablePlans = "available_plans"
    const val HelpAndFeedback = "help_and_feedback"
    const val SupportLogs = "logs"
    const val StatusCheck = "connection_status"
    const val CancelConfirmation = "cancel_confirmation"
    private const val OfferClaimed = "offer_claimed"

    const val OfferClaimedBillingPeriodArgument = "billingPeriod"

    fun offerClaimedRoute() = "$OfferClaimed/{$OfferClaimedBillingPeriodArgument}"

    fun offerClaimedDestination(billingPeriod: BillingPeriod) = "$OfferClaimed/$billingPeriod"
}

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
