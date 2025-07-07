package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow.importFlowGraph
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object OnboardingRecommendationsFlow {
    const val ROUTE = "onboardingRecommendationsFlow"

    private const val START = "start"
    private const val SEARCH = "search"

    fun NavGraphBuilder.onboardingRecommendationsFlowGraph(
        theme: Theme.ThemeType,
        flow: OnboardingFlow,
        onBackPress: () -> Unit,
        onComplete: () -> Unit,
        navController: NavController,
        onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    ) {
        navigation(
            route = this@OnboardingRecommendationsFlow.ROUTE,
            startDestination = START,
        ) {
            importFlowGraph(theme, navController, flow, onUpdateSystemBars)

            composable(START) {
                OnboardingRecommendationsStartPage(
                    theme,
                    onImportClick = { navController.navigate(OnboardingImportFlow.ROUTE) },
                    onSearch = with(LocalContext.current) {
                        {
                            if (Network.isConnected(this)) {
                                navController.navigate(SEARCH)
                            } else {
                                Toast.makeText(
                                    this,
                                    this.getString(LR.string.error_check_your_internet_connection),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    onBackPress = onBackPress,
                    onComplete = onComplete,
                    onUpdateSystemBars = onUpdateSystemBars,
                )
            }
            composable(SEARCH) {
                OnboardingRecommendationsSearchPage(
                    theme = theme,
                    onBackPress = { navController.popBackStack() },
                    onUpdateSystemBars = onUpdateSystemBars,
                )
            }
        }
    }
}
