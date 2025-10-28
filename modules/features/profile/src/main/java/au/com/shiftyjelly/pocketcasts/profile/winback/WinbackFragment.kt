package au.com.shiftyjelly.pocketcasts.profile.winback

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.ProgressDialog
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.HelpPage
import au.com.shiftyjelly.pocketcasts.settings.LogsPage
import au.com.shiftyjelly.pocketcasts.settings.status.StatusPage
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireSerializable
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class WinbackFragment : BaseDialogFragment() {
    private val viewModel by viewModels<WinbackViewModel>()

    private val params get() = requireArguments().requireParcelable<WinbackInitParams>(INPUT_ARGS)

    private var currentScreenId: String? = null

    override val includeNavigationBarPadding = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        CallOnce {
            viewModel.loadWinbackData()
        }

        LaunchedEffect(Unit) {
            setBackgroundTint(Color.Transparent.toArgb())
        }

        val scope = rememberCoroutineScope()
        val state by viewModel.uiState.collectAsState()

        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }

        DialogBox(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(top = 8.dp),
        ) {
            NavHost(
                navController = navController,
                startDestination = if (params.hasGoogleSubscription) {
                    WinbackNavRoutes.MAIN
                } else {
                    WinbackNavRoutes.CANCEL_CONFIRMATION
                },
                enterTransition = { slideInToStart() },
                exitTransition = { slideOutToStart() },
                popEnterTransition = { slideInToEnd() },
                popExitTransition = { slideOutToEnd() },
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(WinbackNavRoutes.MAIN) {
                    CancelOfferPage(
                        onSeeAvailablePlans = {
                            viewModel.trackAvailablePlansTapped()
                            navController.navigate(WinbackNavRoutes.AVAILABLE_PLANS)
                        },
                        onSeeHelpAndFeedback = {
                            viewModel.trackHelpAndFeedbackTapped()
                            navController.navigate(WinbackNavRoutes.HELP_AND_FEEDBACK)
                        },
                        onContinueToCancellation = {
                            viewModel.trackContinueCancellationTapped()
                            navController.navigate(WinbackNavRoutes.CANCEL_CONFIRMATION)
                        },
                    )
                }
                composable(
                    WinbackNavRoutes.offerClaimedRoute(),
                    listOf(
                        navArgument(WinbackNavRoutes.OFER_CLAIMED_BILLING_CYCLE_ARGUMENT) {
                            type = NavType.EnumType(BillingCycle::class.java)
                        },
                    ),
                ) { backStackEntry ->
                    val arguments = requireNotNull(backStackEntry.arguments) { "Missing back stack entry arguments" }
                    val billingCycle = arguments.requireSerializable<BillingCycle>(WinbackNavRoutes.OFER_CLAIMED_BILLING_CYCLE_ARGUMENT)

                    OfferClaimedPage(
                        billingCycle = billingCycle,
                        onConfirm = {
                            viewModel.trackOfferClaimedConfirmationTapped()
                            dismiss()
                        },
                    )
                }
                composable(WinbackNavRoutes.AVAILABLE_PLANS) {
                    AvailablePlansPage(
                        plansState = state.subscriptionPlansState,
                        onSelectPlan = { plan -> viewModel.changePlan(plan, requireActivity()) },
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
                composable(WinbackNavRoutes.HELP_AND_FEEDBACK) {
                    HelpPage(
                        activity = requireActivity(),
                        appBarInsets = AppBarDefaults.topAppBarWindowInsets.only(WindowInsetsSides.Horizontal),
                        onShowLogs = { navController.navigate(WinbackNavRoutes.SUPPORT_LOGS) },
                        onShowStatusPage = { navController.navigate(WinbackNavRoutes.STATUS_CHECK) },
                        onGoBack = { navController.popBackStack() },
                    )
                }
                composable(WinbackNavRoutes.SUPPORT_LOGS) {
                    LogsPage(
                        bottomInset = 0.dp,
                        appBarInsets = AppBarDefaults.topAppBarWindowInsets.only(WindowInsetsSides.Horizontal),
                        onBackPress = { navController.popBackStack() },
                    )
                }
                composable(WinbackNavRoutes.STATUS_CHECK) {
                    StatusPage(
                        bottomInset = 0.dp,
                        appBarInsets = AppBarDefaults.topAppBarWindowInsets.only(WindowInsetsSides.Horizontal),
                        onBackPress = { navController.popBackStack() },
                    )
                }
                composable(WinbackNavRoutes.CANCEL_CONFIRMATION) {
                    CancelConfirmationPage(
                        expirationDate = state.currentSubscriptionExpirationDate,
                        onKeepSubscription = {
                            viewModel.trackKeepSubscriptionTapped()
                            dismiss()
                        },
                        onCancelSubscription = {
                            viewModel.trackCancelSubscriptionTapped()
                            val offer = state.winbackOfferState?.offer
                            if (offer == null) {
                                handleSubscriptionCancellation(state.subscriptionPlansState)
                            } else {
                                navController.navigate(WinbackNavRoutes.WINBACK_OFFER)
                            }
                        },
                    )
                }
                composable(WinbackNavRoutes.WINBACK_OFFER) {
                    val offer = state.winbackOfferState?.offer
                    if (offer != null) {
                        WinbackOfferPage(
                            offer = offer,
                            onAcceptOffer = {
                                viewModel.claimOffer(requireActivity())
                            },
                            onCancelSubscription = {
                                viewModel.trackContinueWithCancellationTapped()
                                handleSubscriptionCancellation(state.subscriptionPlansState)
                            },
                        )
                    } else {
                        WinbackOfferErrorPage(
                            onDismiss = {
                                dismiss()
                            },
                        )
                    }
                }
            }

            val offerState = state.winbackOfferState
            if (offerState?.isClaimingOffer == true) {
                ProgressDialog(
                    text = stringResource(LR.string.winback_claiming_offer),
                )
            }

            if (offerState?.isOfferClaimed == true) {
                LaunchedEffect(Unit) {
                    viewModel.consumeClaimedOffer()
                    val billingCycle = offerState.offer.billingCycle
                    navController.navigate(WinbackNavRoutes.offerClaimedDestination(billingCycle)) {
                        popUpTo(WinbackNavRoutes.MAIN) {
                            inclusive = true
                        }
                    }
                }
            }

            val hasClaimOfferFailed = offerState?.hasOfferClaimFailed == true
            if (hasClaimOfferFailed) {
                LaunchedEffect(Unit) {
                    snackbarHostState.showSnackbar(getString(LR.string.error_generic_message))
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val screenId = currentScreenId
        if (screenId != null) {
            viewModel.trackScreenDismissed(screenId)
        }
    }

    private fun handleSubscriptionCancellation(state: SubscriptionPlansState) {
        val isPlayStoreSubscriptionOpened = if (params.hasGoogleSubscription) {
            val productId = when (state) {
                is SubscriptionPlansState.Loaded -> state.currentSubscription.productId
                is SubscriptionPlansState.Failure -> null
                is SubscriptionPlansState.Loading -> null
            }
            goToPlayStoreSubscriptions(productId)
        } else {
            false
        }
        if (!isPlayStoreSubscriptionOpened) {
            goToWebViewCancellationInfo()
        }

        dismiss()
    }

    private fun goToPlayStoreSubscriptions(sku: String? = null): Boolean {
        val uri = "https://play.google.com/store/account/subscriptions".toUri()
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

    private fun goToWebViewCancellationInfo() {
        WebViewActivity.show(
            context,
            resources.getString(LR.string.winback_cancel_subscription_cancel_button_label),
            Settings.INFO_CANCEL_URL,
        )
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
    const val MAIN = "main"
    const val AVAILABLE_PLANS = "available_plans"
    const val HELP_AND_FEEDBACK = "help_and_feedback"
    const val SUPPORT_LOGS = "logs"
    const val STATUS_CHECK = "connection_status"
    const val CANCEL_CONFIRMATION = "cancel_confirmation"
    const val WINBACK_OFFER = "winback_offer"
    private const val OFFER_CLAIMED = "offer_claimed"

    const val OFER_CLAIMED_BILLING_CYCLE_ARGUMENT = "billingCycle"

    fun offerClaimedRoute() = "$OFFER_CLAIMED/{$OFER_CLAIMED_BILLING_CYCLE_ARGUMENT}"

    fun offerClaimedDestination(billingCycle: BillingCycle) = "$OFFER_CLAIMED/$billingCycle"
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
