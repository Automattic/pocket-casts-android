package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationSubGraph
import com.google.android.horologist.compose.navscaffold.scrollable

fun NavGraphBuilder.settingsRoutes(navController: NavController) {
    settingsUrlScreens()

    scrollable(SettingsScreen.route) {
        SettingsScreen(
            scrollState = it.columnState,
            signInClick = { navController.navigate(authenticationSubGraph) },
            navigateToPrivacySettings = { navController.navigate(PrivacySettingsScreen.route) },
            navigateToAbout = { navController.navigate(WearAboutScreen.route) },
            navigateToHelp = { navController.navigate(HelpScreen.route) }
        )
    }

    scrollable(PrivacySettingsScreen.route) {
        PrivacySettingsScreen(scrollState = it.columnState)
    }

    scrollable(WearAboutScreen.route) {
        WearAboutScreen(
            columnState = it.columnState,
            onTermsOfServiceClick = { navController.navigate(UrlScreenRoutes.termsOfService) },
            onPrivacyClick = { navController.navigate(UrlScreenRoutes.privacy) }
        )
    }

    scrollable(HelpScreen.route) {
        HelpScreen(columnState = it.columnState)
    }
}
