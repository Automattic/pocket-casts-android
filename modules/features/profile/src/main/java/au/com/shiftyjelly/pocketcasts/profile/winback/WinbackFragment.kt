package au.com.shiftyjelly.pocketcasts.profile.winback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.fragment.compose.content
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WinbackFragment : BaseDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        AppThemeWithBackground(
            themeType = theme.activeTheme,
        ) {
            val navController = rememberNavController()
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
                        onClaimOffer = { navController.navigate(WinbackNavRoutes.OfferClaimed) },
                        onSeeAvailablePlans = { navController.navigate(WinbackNavRoutes.AvailablePlans) },
                        onSeeHelpAndFeedback = { navController.navigate(WinbackNavRoutes.HelpAndFeedback) },
                        onContinueToCancellation = { navController.navigate(WinbackNavRoutes.CancelConfirmation) },
                    )
                }
                composable(WinbackNavRoutes.OfferClaimed) {
                    OfferClaimedPage(
                        onConfirm = { dismiss() },
                    )
                }
                composable(WinbackNavRoutes.AvailablePlans) {
                    AvailablePlansPage(
                        onGoBack = { navController.popBackStack() },
                    )
                }
                composable(WinbackNavRoutes.HelpAndFeedback) {
                    HelpAndFeedbackPage(
                        onGoBack = { navController.popBackStack() },
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
}

private object WinbackNavRoutes {
    const val WinbackOffer = "WinbackOffer"
    const val OfferClaimed = "OfferClaimed"
    const val AvailablePlans = "AvailablePlans"
    const val HelpAndFeedback = "HelpAndFeedback"
    const val CancelConfirmation = "CancelConfirmation"
}

private val animationSpec = tween<IntOffset>(350)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInToStart() = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Start,
    animationSpec = animationSpec,
)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToStart() = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Start,
    animationSpec = animationSpec,
)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInToEnd() = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.End,
    animationSpec = animationSpec,
)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToEnd() = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.End,
    animationSpec = animationSpec,
)
