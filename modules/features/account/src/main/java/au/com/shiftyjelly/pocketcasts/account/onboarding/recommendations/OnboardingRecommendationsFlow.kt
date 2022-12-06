package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingRecommendationsStartPage

object OnboardingRecommendationsFlow {

    const val route = "onboardingRecommendationsFlow"

    private const val start = "start"
    private const val search = "search"

    fun NavGraphBuilder.onboardingRecommendationsFlowGraph(
        onShown: () -> Unit,
        onBackPressed: () -> Unit,
        onComplete: () -> Unit,
        navController: NavController,
    ) {
        navigation(
            route = this@OnboardingRecommendationsFlow.route,
            startDestination = start
        ) {
            composable(start) {
                OnboardingRecommendationsStartPage(
                    onShown = onShown,
                    onSearch = { navController.navigate(search) },
                    onBackPressed = onBackPressed,
                    onComplete = onComplete,
                )
            }
            composable(search) {
                OnboardingRecommendationsSearchPage(
                    onBackPressed = { navController.popBackStack() },
                )
            }
        }
    }
}
