package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationSubGraph
import com.google.android.horologist.compose.navscaffold.scrollable

fun NavGraphBuilder.settingsRoutes(navController: NavController) {
    settingsUrlScreens()

    @Suppress("DEPRECATION")
    scrollable(SettingsScreen.route) {
        SettingsScreen(
            scrollState = it.columnState,
            signInClick = { navController.navigate(authenticationSubGraph) },
            navigateToPrivacySettings = { navController.navigate(PrivacySettingsScreen.route) },
            navigateToAbout = { navController.navigate(WearAboutScreen.route) },
            navigateToHelp = { navController.navigate(HelpScreen.route) },
        )
    }

    @Suppress("DEPRECATION")
    scrollable(PrivacySettingsScreen.route) {
        PrivacySettingsScreen(scrollState = it.columnState)
    }

    @Suppress("DEPRECATION")
    scrollable(WearAboutScreen.route) {
        WearAboutScreen(
            columnState = it.columnState,
            onTermsOfServiceClick = { navController.navigate(UrlScreenRoutes.termsOfService) },
            onPrivacyClick = { navController.navigate(UrlScreenRoutes.privacy) },
        )
    }

    @Suppress("DEPRECATION")
    scrollable(HelpScreen.route) {
        HelpScreen(columnState = it.columnState)
    }
}
