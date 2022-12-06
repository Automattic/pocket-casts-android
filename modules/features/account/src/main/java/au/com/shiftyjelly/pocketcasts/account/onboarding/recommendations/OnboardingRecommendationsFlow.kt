package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow.importFlowGraph

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

            importFlowGraph(navController)

            composable(start) {
                OnboardingRecommendationsStartPage(
                    onShown = onShown,
                    onImportClicked = { navController.navigate(OnboardingImportFlow.route) },
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
