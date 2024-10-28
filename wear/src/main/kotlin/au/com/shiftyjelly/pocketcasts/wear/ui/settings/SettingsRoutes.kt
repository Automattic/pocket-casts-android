package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.navigation.composable
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationSubGraph

fun NavGraphBuilder.settingsRoutes(navController: NavController) {
    settingsUrlScreens()

    composable(
        route = SettingsScreen.route,
    ) {
        SettingsScreen(
            signInClick = { navController.navigate(authenticationSubGraph) },
            navigateToPrivacySettings = { navController.navigate(PrivacySettingsScreen.route) },
            navigateToAbout = { navController.navigate(WearAboutScreen.route) },
            navigateToHelp = { navController.navigate(HelpScreen.route) },
        )
    }

    composable(
        route = PrivacySettingsScreen.route,
    ) {
        PrivacySettingsScreen()
    }

    composable(
        route = WearAboutScreen.route,
    ) {
        WearAboutScreen(
            onTermsOfServiceClick = { navController.navigate(UrlScreenRoutes.termsOfService) },
            onPrivacyClick = { navController.navigate(UrlScreenRoutes.privacy) },
        )
    }

    composable(
        route = HelpScreen.route,
    ) {
        HelpScreen()
    }
}
