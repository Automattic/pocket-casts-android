package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersViewModel.UseFoldersState.Applied
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as VR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class SuggestedFoldersFragment : BaseDialogFragment() {

    companion object {
        private const val ARGS_KEY = "args"

        fun newInstance(
            source: Source,
        ): SuggestedFoldersFragment {
            return SuggestedFoldersFragment().apply {
                arguments = bundleOf(
                    ARGS_KEY to Args(source),
                )
            }
        }
    }

    private val args
        get() = requireNotNull(BundleCompat.getParcelable(requireArguments(), ARGS_KEY, Args::class.java)) {
            "Missing input parameters"
        }

    private val viewModel by viewModels<SuggestedFoldersViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SuggestedFoldersViewModel.Factory> { factory ->
                factory.crate(args.source)
            }
        },
    )

    private var isFinalizingActionUsed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        CallOnce {
            viewModel.trackPageShown()
        }

        val state by viewModel.state.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxHeight(0.93f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        ) {
            AppThemeWithBackground(theme.activeTheme) {
                Box(
                    modifier = Modifier
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = SuggestedFoldersNavRoutes.SuggestedFolders,
                        enterTransition = { slideInToStart() },
                        exitTransition = { slideOutToStart() },
                        popEnterTransition = { slideInToEnd() },
                        popExitTransition = { slideOutToEnd() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        composable(SuggestedFoldersNavRoutes.SuggestedFolders) {
                            SuggestedFoldersPage(
                                folders = state.suggestedFolders,
                                action = state.action,
                                onActionClick = { state.action?.let { handleSuggestedAction(it, state.signInState.isSignedInAsPlusOrPatron) } },
                                onCreateCustomFolderClick = { handleCustomFolderCreation(state.signInState.isSignedInAsPlusOrPatron) },
                                onFolderClick = { folder ->
                                    viewModel.trackPreviewFolderTapped(folder)
                                    navController.navigate(SuggestedFoldersNavRoutes.folderDetailsDestination(folder.name))
                                },
                                onCloseClick = ::dismiss,
                            )
                        }
                        composable(
                            SuggestedFoldersNavRoutes.folderDetailsRoute(),
                            listOf(
                                navArgument(SuggestedFoldersNavRoutes.SuggestedFolderNameArgument) {
                                    type = NavType.StringType
                                },
                            ),
                        ) { backStackEntry ->
                            val arguments = requireNotNull(backStackEntry.arguments) { "Missing back stack entry arguments" }
                            val folderName = requireNotNull(arguments.getString(SuggestedFoldersNavRoutes.SuggestedFolderNameArgument)) {
                                "Missing folder name period argument"
                            }
                            SuggestedFolderPodcastsPage(
                                folder = remember(folderName, state.suggestedFolders) {
                                    state.suggestedFolders.find { it.name == folderName }
                                },
                                onGoBackClick = navController::popBackStack,
                            )
                        }
                    }
                }
            }
        }

        LaunchedEffect(state.useFoldersState) {
            if (state.useFoldersState == Applied) {
                finalizeAndDismiss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (isDismissingWithoutAction()) {
            viewModel.trackPageDismissed()

            if (args.source == Source.Popup) {
                viewModel.markPopupAsDismissed()
            }
        }
    }

    private fun finalizeAndDismiss() {
        isFinalizingActionUsed = true
        dismiss()
    }

    private fun isDismissingWithoutAction() = !requireActivity().isChangingConfigurations && !isFinalizingActionUsed

    private fun handleCustomFolderCreation(isUserPlusOrPatreon: Boolean) {
        viewModel.trackCreateCustomFolderTapped()
        if (isUserPlusOrPatreon) {
            FolderCreateFragment
                .newInstance(source = "suggested_folders")
                .show(parentFragmentManager, "create_folder_card")
            finalizeAndDismiss()
        } else {
            OnboardingLauncher.openOnboardingFlow(activity, OnboardingFlow.Upsell(OnboardingUpgradeSource.SUGGESTED_FOLDERS))
        }
    }

    private fun handleSuggestedAction(action: SuggestedAction, isUserPlusOrPatreon: Boolean) {
        if (isUserPlusOrPatreon) {
            when (action) {
                SuggestedAction.UseFolders -> {
                    viewModel.trackUseSuggestedFoldersTapped()
                    viewModel.useSuggestedFolders()
                }
                SuggestedAction.ReplaceFolders -> {
                    viewModel.trackReplaceFolderTapped()
                    showConfirmationDialog()
                }
            }
        } else {
            viewModel.trackUseSuggestedFoldersTapped()
            OnboardingLauncher.openOnboardingFlow(activity, OnboardingFlow.Upsell(OnboardingUpgradeSource.SUGGESTED_FOLDERS))
        }
    }

    private fun showConfirmationDialog() {
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.suggested_folders_replace_folders_button)))
            .setTitle(getString(LR.string.suggested_folders_replace_folders_confirmation_tittle))
            .setSummary(getString(LR.string.suggested_folders_replace_folders_confirmation_description))
            .setOnConfirm {
                viewModel.trackReplaceFoldersConfirmationTapped()
                viewModel.useSuggestedFolders()
            }
            .setIconId(VR.drawable.ic_replace)
            .setIconTint(UR.attr.primary_interactive_01)
            .show(childFragmentManager, "suggested-folders-confirmation-dialog")
    }

    enum class Source(
        val analyticsValue: String,
    ) {
        Popup(
            analyticsValue = "popup",
        ),
        ToolbarButton(
            analyticsValue = "podcasts_list",
        ),
    }

    @Parcelize
    private class Args(
        val source: Source,
    ) : Parcelable
}

private object SuggestedFoldersNavRoutes {
    const val SuggestedFolders = "main"
    private const val FolderDetails = "suggested_folder_details"

    const val SuggestedFolderNameArgument = "folderName"

    fun folderDetailsRoute() = "$FolderDetails/{$SuggestedFolderNameArgument}"

    fun folderDetailsDestination(folderName: String) = "$FolderDetails/$folderName"
}

private val intOffsetAnimationSpec = tween<IntOffset>(350)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInToStart() = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Start,
    animationSpec = intOffsetAnimationSpec,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToStart() = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Start,
    animationSpec = intOffsetAnimationSpec,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInToEnd() = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.End,
    animationSpec = intOffsetAnimationSpec,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToEnd() = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.End,
    animationSpec = intOffsetAnimationSpec,
)
