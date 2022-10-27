package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment

class OnboardingFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    val navHostController = rememberNavController()
                    BackHandler(enabled = navHostController.backQueue.isNotEmpty()) {
                        navHostController.popBackStack()
                    }

                    NavHost(navController = navHostController, startDestination = OnboardingNavRoute.loginOrSignUp) {
                        composable(OnboardingNavRoute.loginOrSignUp) {
                            LoginOrSignUpView(
                                onNotNowPressed = {
                                    @Suppress("DEPRECATION")
                                    activity?.onBackPressed()
                                },
                                onSignUpFreePressed = {
                                    navHostController.navigate(OnboardingNavRoute.createFreeAccount)
                                },
                                showToast = {
                                    Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        composable(OnboardingNavRoute.createFreeAccount) {
                            CreateFreeAccountView()
                        }
                    }
                }
            }
        }
    }
}

object OnboardingNavRoute {
    const val loginOrSignUp = "login_or_sign_up"
    const val createFreeAccount = "create_free_account"
}
