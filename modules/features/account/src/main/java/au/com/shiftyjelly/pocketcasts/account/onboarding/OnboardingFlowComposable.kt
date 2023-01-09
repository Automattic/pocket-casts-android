package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow.importFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow.onboardingRecommendationsFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingPlusUpgradeFlow
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.io.Serializable

@Composable
fun OnboardingFlowComposable(
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    exitOnboarding: () -> Unit,
    completeOnboardingToDiscover: () -> Unit,
    signInState: SignInState
) {
    AppThemeWithBackground(theme) {
        val navController = rememberNavController()

        val startDestination = when (flow) {
            OnboardingFlow.LoggedOut,
            is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
            OnboardingFlow.InitialOnboarding -> OnboardingNavRoute.logInOrSignUp

            // Cannot use OnboardingNavRoute.PlusUpgrade.routeWithSource here because
            // the startDestination cannot be dynamic, see https://stackoverflow.com/a/70410872/1910286
            is OnboardingFlow.PlusAccountUpgrade, // FIXME this should just open the purchase modal
            is OnboardingFlow.PlusFlow -> OnboardingNavRoute.PlusUpgrade.route
        }

        NavHost(navController, startDestination) {

            importFlowGraph(theme, navController, flow)

            onboardingRecommendationsFlowGraph(
                theme = theme,
                flow = flow,
                onBackPressed = exitOnboarding,
                onComplete = {
                    val route = OnboardingNavRoute.PlusUpgrade.routeWithSource(OnboardingUpgradeSource.RECOMMENDATIONS)
                    navController.navigate(route)
                },
                navController = navController,
            )

            composable(OnboardingNavRoute.logInOrSignUp) {
                OnboardingLoginOrSignUpPage(
                    theme = theme,
                    flow = flow,
                    onDismiss = {
                        when (flow) {
                            // This should never happen. If the user isn't logged in they should be in the AccountUpgradeNeedsLogin flow
                            is OnboardingFlow.PlusAccountUpgrade -> throw IllegalStateException("PlusAccountUpgrade flow tried to present LoginOrSignupPage")

                            OnboardingFlow.PlusAccountUpgradeNeedsLogin,
                            is OnboardingFlow.PlusUpsell -> navController.popBackStack()

                            OnboardingFlow.InitialOnboarding,
                            OnboardingFlow.LoggedOut -> exitOnboarding()
                        }
                    },
                    onSignUpClicked = { navController.navigate(OnboardingNavRoute.createFreeAccount) },
                    onLoginClicked = { navController.navigate(OnboardingNavRoute.logIn) },
                    onContinueWithGoogleClicked = { navController.navigate(OnboardingNavRoute.logInGoogle) },
                )
            }

            composable(OnboardingNavRoute.createFreeAccount) {
                OnboardingCreateAccountPage(
                    theme = theme,
                    onBackPressed = { navController.popBackStack() },
                    onAccountCreated = {
                        navController.navigate(OnboardingRecommendationsFlow.route) {
                            // clear backstack after account is created
                            popUpTo(OnboardingNavRoute.logInOrSignUp) {
                                inclusive = true
                            }
                        }
                    },
                )
            }

            composable(OnboardingNavRoute.logIn) {
                OnboardingLoginPage(
                    theme = theme,
                    onBackPressed = { navController.popBackStack() },
                    onLoginComplete = exitOnboarding,
                    onForgotPasswordTapped = { navController.navigate(OnboardingNavRoute.forgotPassword) },
                )
            }

            composable(OnboardingNavRoute.logInGoogle) {
                OnboardingLoginGooglePage()
            }

            composable(OnboardingNavRoute.forgotPassword) {
                OnboardingForgotPasswordPage(
                    theme = theme,
                    onBackPressed = { navController.popBackStack() },
                    onCompleted = exitOnboarding,
                )
            }

            composable(
                route = OnboardingNavRoute.PlusUpgrade.route,
                arguments = listOf(
                    navArgument(OnboardingNavRoute.PlusUpgrade.sourceArgumentKey) {
                        type = NavType.EnumType(OnboardingUpgradeSource::class.java)
                    }
                )
            ) { navBackStackEntry ->

                val upgradeSource = navBackStackEntry.arguments
                    ?.getSerializableCompat(OnboardingNavRoute.PlusUpgrade.sourceArgumentKey, OnboardingUpgradeSource::class.java)
                    // If null, that means upgradeSource was not passed as an argument, so this must be the
                    // startDestination. We have to use the flow to get the upgradeSource when this is
                    // the startDestination because arguments can not be passed in the startDestination,
                    // see https://stackoverflow.com/a/70410872/1910286. In that case, the flow should
                    // always be a PlusFlow, but the compiler doesn't enforce that.
                    ?: when (flow) {
                        is OnboardingFlow.PlusFlow -> flow.source
                        else -> throw IllegalStateException("upgradeSource not set")
                    }

                OnboardingPlusUpgradeFlow(
                    flow = flow,
                    source = upgradeSource,
                    isLoggedIn = signInState.isSignedIn,
                    onBackPressed = {
                        when (upgradeSource) {
                            OnboardingUpgradeSource.NEEDS_LOGIN,
                            OnboardingUpgradeSource.PLUS_DETAILS,
                            OnboardingUpgradeSource.PROFILE -> exitOnboarding()
                            OnboardingUpgradeSource.RECOMMENDATIONS -> navController.popBackStack()
                        }
                    },
                    onNeedLogin = { navController.navigate(OnboardingNavRoute.logInOrSignUp) },
                    onProceed = {
                        when (upgradeSource) {
                            OnboardingUpgradeSource.NEEDS_LOGIN,
                            OnboardingUpgradeSource.PLUS_DETAILS,
                            OnboardingUpgradeSource.PROFILE -> { exitOnboarding() }
                            OnboardingUpgradeSource.RECOMMENDATIONS -> { navController.navigate(OnboardingNavRoute.welcome) }
                        }
                        navController.navigate(OnboardingNavRoute.welcome)
                    }
                )
            }

            composable(OnboardingNavRoute.welcome) {
                OnboardingWelcomePage(
                    activeTheme = theme,
                    flow = flow,
                    isSignedInAsPlus = signInState.isSignedInAsPlus,
                    onDone = exitOnboarding,
                    onContinueToDiscover = completeOnboardingToDiscover,
                    onImportTapped = { navController.navigate(OnboardingImportFlow.route) },
                    onBackPressed = {
                        // Don't allow navigation back to the upgrade screen after the user upgrades
                        if (signInState.isSignedInAsPlus) {
                            exitOnboarding()
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    }
}

private fun <T : Serializable> Bundle.getSerializableCompat(key: String, clazz: Class<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getSerializable(key)?.let { result ->
            if (clazz.isInstance(result)) {
                @Suppress("UNCHECKED_CAST")
                result as T
            } else {
                null
            }
        }
    }

private object OnboardingNavRoute {

    const val createFreeAccount = "create_free_account"
    const val forgotPassword = "forgot_password"
    const val logIn = "log_in"
    const val logInGoogle = "log_in_google"
    const val logInOrSignUp = "log_in_or_sign_up"
    const val welcome = "welcome"

    object PlusUpgrade {
        private const val routeBase = "plus_upgrade"

        const val sourceArgumentKey = "source"
        // The route variable should only be used to navigate to the PlusUpgrade screens
        // when they are the startDestination. In all other cases, use the routeWithSource function.
        const val route = "$routeBase/{$sourceArgumentKey}"
        fun routeWithSource(source: OnboardingUpgradeSource) = "$routeBase/$source"
    }
}
