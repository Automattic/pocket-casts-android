package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import android.content.ActivityNotFoundException
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object OnboardingImportFlow {

    const val route = "onboardingImportFlow"

    private const val start = "start"
    private const val castbox = "castbox"
    private const val otherApps = "otherApps"

    fun NavGraphBuilder.importFlowGraph(navController: NavController) {
        navigation(
            route = this@OnboardingImportFlow.route,
            startDestination = start,
        ) {
            composable(start) {
                OnboardingImportStartPage(
                    onCastboxClicked = { navController.navigate(castbox) },
                    onOtherAppsClicked = { navController.navigate(otherApps) },
                    onBackPressed = { navController.popBackStack() },
                )
            }

            composable(castbox) {
                OnboardingImportFrom(
                    drawableRes = IR.drawable.castbox,
                    title = (stringResource(LR.string.onboarding_import_from_castbox)),
                    steps = listOf(
                        stringResource(LR.string.onboarding_import_from_castbox_step_1),
                        stringResource(LR.string.onboarding_import_from_castbox_step_2),
                        stringResource(LR.string.onboarding_import_from_castbox_step_3),
                        stringResource(LR.string.onboarding_import_from_castbox_step_4),
                        stringResource(LR.string.onboarding_import_from_castbox_step_5),
                    ),
                    buttonText = stringResource(LR.string.onboarding_import_from_castbox_open),
                    buttonClick = openCastboxFun(),
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(otherApps) {
                OnboardingImportFrom(
                    drawableRes = IR.drawable.other_apps,
                    title = stringResource(LR.string.onboarding_import_from_other_apps),
                    text = stringResource(LR.string.onboarding_can_import_from_opml),
                    steps = listOf(
                        stringResource(LR.string.onboarding_import_from_other_apps_step_1),
                        stringResource(LR.string.onboarding_import_from_other_apps_step_2),
                    ),
                    onBackPressed = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun openCastboxFun(): (() -> Unit)? {
    val context = LocalContext.current
    return context
        .packageManager
        .getLaunchIntentForPackage("fm.castbox.audiobook.radio.podcast")
        ?.let { intent ->
            {
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // should only happen if the user uninstalls castbox after this screen is composed
                }
            }
        }
}
