package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersViewModel.UseFoldersState.Applied
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
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

    private val viewModel by viewModels<SuggestedFoldersViewModel>()

    private val args
        get() = requireNotNull(BundleCompat.getParcelable(requireArguments(), ARGS_KEY, Args::class.java)) {
            "Missing input parameters"
        }

    private var isFinalizingActionUsed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.useFolderesState) {
            if (state.useFolderesState == Applied) {
                finalizeAndDismiss()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight(0.93f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        ) {
            AppThemeWithBackground(theme.activeTheme) {
                Column(
                    modifier = Modifier
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
                ) {
                    IconButton(
                        onClick = { dismiss() },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(LR.string.close),
                            tint = MaterialTheme.theme.colors.primaryInteractive01,
                            modifier = Modifier.padding(16.dp),
                        )
                    }

                    val action = state.action
                    if (action != null) {
                        SuggestedFoldersPage(
                            folders = state.suggestedFolders,
                            action = action,
                            onActionClick = {
                                if (state.isUserPlusOrPatreon) {
                                    when (action) {
                                        SuggestedAction.UseFolders -> viewModel.useSuggestedFolders()
                                        SuggestedAction.ReplaceFolders -> showConfirmationDialog()
                                    }
                                } else {
                                    OnboardingLauncher.openOnboardingFlow(activity, OnboardingFlow.Upsell(OnboardingUpgradeSource.SUGGESTED_FOLDERS))
                                }
                            },
                            onCreateCustomFolderClick = {
                                FolderCreateFragment
                                    .newInstance(source = "suggested_folders")
                                    .show(parentFragmentManager, "create_folder_card")
                                finalizeAndDismiss()
                            },
                            onFolderClick = { Timber.i("Clicked folder: ${it.name}") },
                        )
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (isDismissingPopupWithoutAction()) {
            viewModel.markPopupAsDismissed()
        }
    }

    private fun finalizeAndDismiss() {
        isFinalizingActionUsed = true
        dismiss()
    }

    private fun isDismissingPopupWithoutAction() =
        !requireActivity().isChangingConfigurations &&
            !isFinalizingActionUsed &&
            args.source == Source.PodcastsPopup

    private fun showConfirmationDialog() {
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.suggested_folders_replace_folders_button)))
            .setTitle(getString(LR.string.suggested_folders_replace_folders_confirmation_tittle))
            .setSummary(getString(LR.string.suggested_folders_replace_folders_confirmation_description))
            .setOnConfirm { viewModel.useSuggestedFolders() }
            .setIconId(VR.drawable.ic_replace)
            .setIconTint(UR.attr.primary_interactive_01)
            .show(childFragmentManager, "suggested-folders-confirmation-dialog")
    }

    enum class Source {
        PodcastsPopup,
        CreateFolderButton,
    }

    @Parcelize
    private class Args(
        val source: Source,
    ) : Parcelable
}
