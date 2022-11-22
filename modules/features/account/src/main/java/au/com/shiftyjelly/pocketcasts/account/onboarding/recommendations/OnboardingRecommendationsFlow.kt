package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingRecommendationsStartPage

@Composable
fun OnboardingRecommendationsFlow(
    onShown: () -> Unit,
    onBackPressed: () -> Unit,
    onComplete: () -> Unit,
) {

    LaunchedEffect(Unit) { onShown() }
    BackHandler { onBackPressed() }

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = OnboardingRecommendationsNavRoute.start
    ) {
        composable(OnboardingRecommendationsNavRoute.start) {
            OnboardingRecommendationsStartPage(
                onSearch = { navController.navigate(OnboardingRecommendationsNavRoute.search) },
                onComplete = onComplete,
            )
        }
        composable(OnboardingRecommendationsNavRoute.search) {
            OnboardingRecommendationsSearch(
                onBackPressed = { navController.popBackStack() },
            )
        }
    }
}
private object OnboardingRecommendationsNavRoute {
    const val start = "start"
    const val search = "search"
}
