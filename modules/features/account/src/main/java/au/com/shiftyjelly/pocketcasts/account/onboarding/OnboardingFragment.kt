package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
                    NavHost(navController = navHostController, startDestination = OnboardingNavRoute.login_or_sign_up) {
                        composable(OnboardingNavRoute.login_or_sign_up) {
                            LoginOrSignUpView(
                                onNotNowPressed = {
                                    @Suppress("DEPRECATION")
                                    activity?.onBackPressed()
                                },
                                showToast = {
                                    Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

object OnboardingNavRoute {
    const val login_or_sign_up = "login_or_sign_up"
}
