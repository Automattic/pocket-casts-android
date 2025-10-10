package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersViewModel.UseFoldersState.Applied
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.SuggestedFoldersAction
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireString
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.parcelize.Parcelize
import timber.log.Timber
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

    private val args get() = requireArguments().requireParcelable<Args>(ARGS_KEY)

    private val viewModel by viewModels<SuggestedFoldersViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SuggestedFoldersViewModel.Factory> { factory ->
                factory.crate(args.source)
            }
        },
    )

    private val onboardingLauncher = registerForActivityResult(OnboardingActivityContract()) { result ->
        when (result) {
            is OnboardingFinish.DoneApplySuggestedFolders -> {
                resolveSuggestedFolders(result)
            }

            is OnboardingFinish.Done -> Unit

            is OnboardingFinish.DoneGoToDiscover,
            is OnboardingFinish.DoneShowPlusPromotion,
            is OnboardingFinish.DoneShowWelcomeInReferralFlow,
            null,
            -> {
                Timber.w("Unexpected onboarding result: $result")
            }
        }
    }

    private var isFinalizingActionUsed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        CallOnce {
            viewModel.trackPageShown()
            viewModel.registerFeatureInteraction()
        }

        val state by viewModel.state.collectAsState()

        DialogBox(
            modifier = Modifier
                .nestedScroll(rememberNestedScrollInteropConnection())
                .fillMaxSize(),
        ) {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = SuggestedFoldersNavRoutes.SUGGESTED_FOLDERS,
                enterTransition = { slideInToStart() },
                exitTransition = { slideOutToStart() },
                popEnterTransition = { slideInToEnd() },
                popExitTransition = { slideOutToEnd() },
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(SuggestedFoldersNavRoutes.SUGGESTED_FOLDERS) {
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
                        navArgument(SuggestedFoldersNavRoutes.SUGGESTED_FOLDER_NAME_ARGUMENT) {
                            type = NavType.StringType
                        },
                    ),
                ) { backStackEntry ->
                    val arguments = requireNotNull(backStackEntry.arguments) { "Missing back stack entry arguments" }
                    val folderName = arguments.requireString(SuggestedFoldersNavRoutes.SUGGESTED_FOLDER_NAME_ARGUMENT)

                    SuggestedFolderPodcastsPage(
                        folder = remember(folderName, state.suggestedFolders) {
                            state.suggestedFolders.find { it.name == folderName }
                        },
                        onGoBackClick = navController::popBackStack,
                    )
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
            launchOnboarding(SuggestedFoldersAction.CreateCustom)
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
            launchOnboarding(SuggestedFoldersAction.UseSuggestion)
        }
    }

    private fun launchOnboarding(action: SuggestedFoldersAction) {
        val launchFlow = OnboardingFlow.UpsellSuggestedFolder(action)
        val launchIntent = OnboardingLauncher.launchIntent(requireActivity(), launchFlow)
        onboardingLauncher.launch(launchIntent)
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

    private fun resolveSuggestedFolders(result: OnboardingFinish.DoneApplySuggestedFolders) {
        when (result.action) {
            SuggestedFoldersAction.UseSuggestion -> {
                val suggestedAction = viewModel.state.value.action
                if (suggestedAction != null) {
                    handleSuggestedAction(suggestedAction, isUserPlusOrPatreon = true)
                } else {
                    Timber.w("Missing suggested action after onboarding process")
                }
            }

            SuggestedFoldersAction.CreateCustom -> {
                handleCustomFolderCreation(isUserPlusOrPatreon = true)
            }
        }
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
        DEEPLINK(
            analyticsValue = "deeplink",
        ),
    }

    @Parcelize
    private class Args(
        val source: Source,
    ) : Parcelable
}

private object SuggestedFoldersNavRoutes {
    const val SUGGESTED_FOLDERS = "main"
    private const val FOLDER_DETAILS = "suggested_folder_details"

    const val SUGGESTED_FOLDER_NAME_ARGUMENT = "folderName"

    fun folderDetailsRoute() = "$FOLDER_DETAILS/{$SUGGESTED_FOLDER_NAME_ARGUMENT}"

    fun folderDetailsDestination(folderName: String) = "$FOLDER_DETAILS/$folderName"
}
