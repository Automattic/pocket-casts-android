package au.com.shiftyjelly.pocketcasts.account.onboarding

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
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeFlow
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getSerializableCompat

@Composable
fun OnboardingFlowComposable(
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    exitOnboarding: () -> Unit,
    completeOnboardingToDiscover: () -> Unit,
    signInState: SignInState
) {
    if (flow is OnboardingFlow.PlusAccountUpgrade) {
        Content(
            theme = theme,
            flow = flow,
            exitOnboarding = exitOnboarding,
            completeOnboardingToDiscover = completeOnboardingToDiscover,
            signInState = signInState
        )
    } else {
        AppThemeWithBackground(theme) {
            Content(
                theme = theme,
                flow = flow,
                exitOnboarding = exitOnboarding,
                completeOnboardingToDiscover = completeOnboardingToDiscover,
                signInState = signInState
            )
        }
    }
}

@Composable
private fun Content(
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    exitOnboarding: () -> Unit,
    completeOnboardingToDiscover: () -> Unit,
    signInState: SignInState
) {
    val navController = rememberNavController()

    val startDestination = when (flow) {
        OnboardingFlow.LoggedOut,
        is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
        OnboardingFlow.InitialOnboarding -> OnboardingNavRoute.logInOrSignUp

        // Cannot use OnboardingNavRoute.PlusUpgrade.routeWithSource here, it is set as a defaultValue in the PlusUpgrade composable,
        // see https://stackoverflow.com/a/70410872/1910286
        is OnboardingFlow.PlusAccountUpgrade,
        is OnboardingFlow.PlusFlow -> OnboardingNavRoute.PlusUpgrade.route
    }

    val onAccountCreated = {
        navController.navigate(OnboardingRecommendationsFlow.route) {
            // clear backstack after account is created
            popUpTo(OnboardingNavRoute.logInOrSignUp) {
                inclusive = true
            }
        }
    }

    NavHost(navController, startDestination) {

        importFlowGraph(theme, navController, flow)

        onboardingRecommendationsFlowGraph(
            theme = theme,
            flow = flow,
            onBackPressed = exitOnboarding,
            onComplete = {
                navController.navigate(
                    if (signInState.isSignedInAsPlusOrPatron) {
                        OnboardingNavRoute.welcome
                    } else {
                        OnboardingNavRoute.PlusUpgrade.routeWithSource(OnboardingUpgradeSource.RECOMMENDATIONS)
                    }
                )
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
                        is OnboardingFlow.PlusAccountUpgrade,
                        is OnboardingFlow.PatronAccountUpgrade -> throw IllegalStateException("Account upgrade flow tried to present LoginOrSignupPage")

                        OnboardingFlow.PlusAccountUpgradeNeedsLogin,
                        is OnboardingFlow.PlusUpsell -> {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                exitOnboarding()
                            }
                        }

                        OnboardingFlow.InitialOnboarding,
                        OnboardingFlow.LoggedOut -> exitOnboarding()
                    }
                },
                onSignUpClicked = { navController.navigate(OnboardingNavRoute.createFreeAccount) },
                onLoginClicked = { navController.navigate(OnboardingNavRoute.logIn) },
                onContinueWithGoogleComplete = { state ->
                    if (state.isNewAccount) {
                        onAccountCreated()
                    } else {
                        exitOnboarding()
                    }
                },
            )
        }

        composable(OnboardingNavRoute.createFreeAccount) {
            OnboardingCreateAccountPage(
                theme = theme,
                onBackPressed = { navController.popBackStack() },
                onAccountCreated = onAccountCreated,
            )
        }

        composable(OnboardingNavRoute.logIn) {
            OnboardingLoginPage(
                theme = theme,
                onBackPressed = { navController.popBackStack() },
                onLoginComplete = {
                    when (flow) {
                        OnboardingFlow.InitialOnboarding,
                        OnboardingFlow.LoggedOut -> exitOnboarding()

                        is OnboardingFlow.PlusAccountUpgrade,
                        is OnboardingFlow.PatronAccountUpgrade,
                        OnboardingFlow.PlusAccountUpgradeNeedsLogin,
                        is OnboardingFlow.PlusUpsell -> navController.navigate(
                            OnboardingNavRoute.PlusUpgrade.routeWithSource(OnboardingUpgradeSource.LOGIN)
                        ) {
                            // clear backstack after successful login
                            popUpTo(OnboardingNavRoute.logInOrSignUp) { inclusive = true }
                        }
                    }
                },
                onForgotPasswordTapped = { navController.navigate(OnboardingNavRoute.forgotPassword) },
            )
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
                    /* Set default value for onboarding flows with startDestination. */
                    when (flow) {
                        is OnboardingFlow.PlusAccountUpgrade -> defaultValue = flow.source
                        is OnboardingFlow.PlusFlow -> defaultValue = flow.source
                        else -> Unit // Not a startDestination, default value should not be set.
                    }
                },
                navArgument(OnboardingNavRoute.PlusUpgrade.showPatronOnlyArgumentKey) {
                    type = NavType.BoolType
                    defaultValue = when (flow) {
                        is OnboardingFlow.PlusUpsell -> flow.showPatronOnly
                        else -> false
                    }
                }
            )
        ) { navBackStackEntry ->

            val upgradeSource = navBackStackEntry.arguments
                ?.getSerializableCompat(OnboardingNavRoute.PlusUpgrade.sourceArgumentKey, OnboardingUpgradeSource::class.java)
                // upgradeSource should always be present. If startDestination, it is passed as a default argument.
                ?: throw IllegalStateException("upgradeSource not set")

            val userCreatedNewAccount = when (upgradeSource) {
                OnboardingUpgradeSource.APPEARANCE,
                OnboardingUpgradeSource.FILES,
                OnboardingUpgradeSource.FOLDERS,
                OnboardingUpgradeSource.LOGIN,
                OnboardingUpgradeSource.PLUS_DETAILS,
                OnboardingUpgradeSource.PROFILE,
                OnboardingUpgradeSource.ACCOUNT_DETAILS,
                OnboardingUpgradeSource.SETTINGS -> false

                OnboardingUpgradeSource.RECOMMENDATIONS -> true
            }

            OnboardingUpgradeFlow(
                flow = flow,
                source = upgradeSource,
                isLoggedIn = signInState.isSignedIn,
                onBackPressed = {
                    if (userCreatedNewAccount) {
                        navController.popBackStack()
                    } else {
                        exitOnboarding()
                    }
                },
                onNeedLogin = { navController.navigate(OnboardingNavRoute.logInOrSignUp) },
                onProceed = {
                    if (userCreatedNewAccount) {
                        navController.navigate(OnboardingNavRoute.welcome)
                    } else {
                        exitOnboarding()
                    }
                }
            )
        }

        composable(OnboardingNavRoute.welcome) {
            OnboardingWelcomePage(
                activeTheme = theme,
                flow = flow,
                isSignedInAsPlusOrPatron = signInState.isSignedInAsPlusOrPatron,
                onDone = exitOnboarding,
                onContinueToDiscover = completeOnboardingToDiscover,
                onImportTapped = { navController.navigate(OnboardingImportFlow.route) },
                onBackPressed = {
                    // Don't allow navigation back to the upgrade screen after the user upgrades
                    if (signInState.isSignedInAsPlusOrPatron) {
                        exitOnboarding()
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
    }
}

private object OnboardingNavRoute {

    const val createFreeAccount = "create_free_account"
    const val forgotPassword = "forgot_password"
    const val logIn = "log_in"
    const val logInOrSignUp = "log_in_or_sign_up"
    const val welcome = "welcome"

    object PlusUpgrade {
        private const val routeBase = "plus_upgrade"

        const val sourceArgumentKey = "source"
        const val showPatronOnlyArgumentKey = "show_patron_only"
        // The route variable should only be used to navigate to the PlusUpgrade screens
        // when they are the startDestination and the args for these startDestinations are set using default values.
        // They are parsed based on this deep-link-like route by the navigation component.
        // For more details check here: https://developer.android.com/jetpack/compose/navigation#nav-with-args
        // In all other cases, use the routeWithSource function.
        const val route = "$routeBase/{$sourceArgumentKey}?{$showPatronOnlyArgumentKey}={$showPatronOnlyArgumentKey}"
        fun routeWithSource(source: OnboardingUpgradeSource) = "$routeBase/$source"
    }
}
