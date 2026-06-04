package au.com.shiftyjelly.pocketcasts.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.home.TvScaffold
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun TvOnboardingNavHost() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = TvOnboardingRoutes.HOME,
            ) {
                composable(TvOnboardingRoutes.HOME) {
                    TvScaffold()
                }
            }
        }
    }
}
