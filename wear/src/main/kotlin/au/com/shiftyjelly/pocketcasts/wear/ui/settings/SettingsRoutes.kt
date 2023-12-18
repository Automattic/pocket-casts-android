package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import au.com.shiftyjelly.pocketcasts.wear.extensions.responsive
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationSubGraph
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.navscaffold.scrollable

fun NavGraphBuilder.settingsRoutes(navController: NavController) {
    settingsUrlScreens()

    scrollable(
        route = SettingsScreen.route,
        columnStateFactory = ScalingLazyColumnDefaults.responsive(firstItemIsFullWidth = false),
    ) {
        SettingsScreen(
            scrollState = it.columnState,
            signInClick = { navController.navigate(authenticationSubGraph) },
            navigateToPrivacySettings = { navController.navigate(PrivacySettingsScreen.route) },
            navigateToAbout = { navController.navigate(WearAboutScreen.route) },
            navigateToHelp = { navController.navigate(HelpScreen.route) }
        )
    }

    scrollable(
        route = PrivacySettingsScreen.route,
        columnStateFactory = ScalingLazyColumnDefaults.responsive(firstItemIsFullWidth = false),
    ) {
        PrivacySettingsScreen(scrollState = it.columnState)
    }

    scrollable(
        route = WearAboutScreen.route,
        columnStateFactory = ScalingLazyColumnDefaults.responsive(firstItemIsFullWidth = false),
    ) {
        WearAboutScreen(
            columnState = it.columnState,
            onTermsOfServiceClick = { navController.navigate(UrlScreenRoutes.termsOfService) },
            onPrivacyClick = { navController.navigate(UrlScreenRoutes.privacy) }
        )
    }

    scrollable(
        route = HelpScreen.route,
        columnStateFactory = ScalingLazyColumnDefaults.responsive(firstItemIsFullWidth = true),
    ) {
        HelpScreen(columnState = it.columnState)
    }
}
