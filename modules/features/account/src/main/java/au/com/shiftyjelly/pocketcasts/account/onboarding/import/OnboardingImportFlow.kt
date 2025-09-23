package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import android.content.ActivityNotFoundException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingImportViewModel
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.repositories.opml.OpmlImportTask
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.flow.map
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object OnboardingImportFlow {
    const val ROUTE = "onboardingImportFlow"

    fun NavGraphBuilder.importFlowGraph(
        theme: Theme.ThemeType,
        navController: NavController,
        flow: OnboardingFlow,
        onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    ) {
        navigation(
            route = ROUTE,
            startDestination = NavigationRoutes.START,
        ) {
            composable(NavigationRoutes.START) {
                val viewModel = hiltViewModel<OnboardingImportViewModel>()
                OnboardingImportStartPage(
                    theme = theme,
                    onShow = { viewModel.onImportStartPageShown(flow) },
                    onCastboxClick = {
                        viewModel.onAppSelected(flow, AnalyticsProps.CASTBOX)
                        navController.navigate(NavigationRoutes.CASTBOX)
                    },
                    onOtherAppsClick = {
                        viewModel.onAppSelected(flow, AnalyticsProps.OTHER_APPS)
                        navController.navigate(NavigationRoutes.OTHER_APPS)
                    },
                    onImportFromUrlClick = {
                        viewModel.onAppSelected(flow, AnalyticsProps.OPML_FROM_URL)
                        navController.navigate(NavigationRoutes.OPML_FROM_URL)
                    },
                    onBackPress = {
                        viewModel.onImportDismissed(flow)
                        navController.popBackStack()
                    },
                    onUpdateSystemBars = onUpdateSystemBars,
                )
            }

            composable(NavigationRoutes.CASTBOX) {
                val viewModel = hiltViewModel<OnboardingImportViewModel>()
                OnboardingImportFrom(
                    theme = theme,
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
                    buttonClick = openCastboxFun()?.let { function ->
                        {
                            viewModel.onOpenApp(flow, AnalyticsProps.CASTBOX)
                            function()
                        }
                    },
                    onBackPress = { navController.popBackStack() },
                    onUpdateSystemBars = onUpdateSystemBars,
                )
            }

            composable(NavigationRoutes.OTHER_APPS) {
                OnboardingImportFrom(
                    theme = theme,
                    drawableRes = IR.drawable.other_apps,
                    title = stringResource(LR.string.onboarding_import_from_other_apps),
                    text = stringResource(LR.string.onboarding_can_import_from_opml),
                    steps = listOf(
                        stringResource(LR.string.onboarding_import_from_other_apps_step_1),
                        stringResource(LR.string.onboarding_import_from_other_apps_step_2),
                    ),
                    onBackPress = { navController.popBackStack() },
                    onUpdateSystemBars = onUpdateSystemBars,
                )
            }

            composable(NavigationRoutes.OPML_FROM_URL) {
                val context = LocalContext.current
                val isImporting by remember {
                    OpmlImportTask.workInfos(context).map { infos -> infos.any { !it.state.isFinished } }
                }.collectAsState(false)

                OnboardingImportOpmlUrl(
                    isImporting = isImporting,
                    onImport = {
                        OpmlImportTask.run(it, context.applicationContext)
                    },
                    onBackPress = { navController.popBackStack() },
                    onUpdateSystemBars = onUpdateSystemBars,
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

private object NavigationRoutes {
    const val START = "start"
    const val CASTBOX = "castbox"
    const val OTHER_APPS = "otherApps"
    const val OPML_FROM_URL = "opmlFromUrl"
}

private object AnalyticsProps {
    const val CASTBOX = "castbox"
    const val OTHER_APPS = "other_apps"
    const val OPML_FROM_URL = "opml_from_url"
}
