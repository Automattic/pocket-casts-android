package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun OnboardingImportFlow(
    onBackPressed: () -> Unit,
) {

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = OnboardingImportNavRoute.start
    ) {
        composable(OnboardingImportNavRoute.start) {
            OnboardingImportStartPage(
                onCastboxClicked = { navController.navigate(OnboardingImportNavRoute.castbox) },
                onOtherAppsClicked = {},
                onBackPressed = { navController.popBackStack() },
            )
        }

        composable(OnboardingImportNavRoute.castbox) {
            OnboardingImportCastbox(
                onBackPressed = { navController.popBackStack() },
            )
        }
    }
}

private object OnboardingImportNavRoute {
    const val start = "start"
    const val castbox = "castbox"
}
