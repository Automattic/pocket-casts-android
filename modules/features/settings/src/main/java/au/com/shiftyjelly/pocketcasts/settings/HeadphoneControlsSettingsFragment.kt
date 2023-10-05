package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.featureflag.ReleaseVersion
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class HeadphoneControlsSettingsFragment : BaseFragment() {
    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var playbackManager: PlaybackManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppThemeWithBackground(theme.activeTheme) {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                val previousAction = settings.headphoneControlsPreviousAction.flow.collectAsState().value
                val nextAction = settings.headphoneControlsNextAction.flow.collectAsState().value
                val confirmationSound = settings.headphoneControlsPlayBookmarkConfirmationSound.flow.collectAsState().value

                HeadphoneControlsSettingsPage(
                    previousAction = previousAction,
                    nextAction = nextAction,
                    confirmationSound = confirmationSound,
                    onBackPressed = {
                        @Suppress("DEPRECATION")
                        activity?.onBackPressed()
                    },
                )
            }
        }
    }

    @Composable
    private fun HeadphoneControlsSettingsPage(
        viewModel: HeadphoneControlsSettingsPageViewModel = hiltViewModel(),
        previousAction: HeadphoneAction,
        nextAction: HeadphoneAction,
        confirmationSound: Boolean,
        onBackPressed: () -> Unit,
    ) {
        val state by viewModel.state.collectAsState()

        CallOnce {
            viewModel.onShown()
        }

        Content(
            state = state,
            previousAction = previousAction,
            nextAction = nextAction,
            onNextActionSave = { viewModel.onNextActionSave(it) },
            onPreviousActionSave = { viewModel.onPreviousActionSave(it) },
            onConfirmationSoundSave = { newValue ->
                settings.headphoneControlsPlayBookmarkConfirmationSound.set(newValue)
                if (newValue) {
                    playbackManager.playTone()
                }
                viewModel.onConfirmationSoundChanged(newValue)
            },
            confirmationSound = confirmationSound,
            onBackPressed = onBackPressed,
            onOptionsDialogShown = { viewModel.onOptionsDialogShown() }
        )

        LaunchedEffect(state) {
            state.startUpsellFromSource?.let { startUpsellFlow() }
        }
    }

    @Composable
    private fun Content(
        state: HeadphoneControlsSettingsPageViewModel.UiState,
        previousAction: HeadphoneAction,
        nextAction: HeadphoneAction,
        onPreviousActionSave: (HeadphoneAction) -> Unit,
        onNextActionSave: (HeadphoneAction) -> Unit,
        confirmationSound: Boolean,
        onConfirmationSoundSave: (Boolean) -> Unit,
        onBackPressed: () -> Unit,
        onOptionsDialogShown: () -> Unit,
    ) {
        Column {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_title_headphone_controls),
                bottomShadow = true,
                onNavigationClick = { onBackPressed() }
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                TextP50(
                    text = stringResource(LR.string.settings_headphone_controls_summary),
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.padding(16.dp)
                )
                PreviousActionRow(
                    state = state,
                    saved = previousAction,
                    onSave = onPreviousActionSave,
                    onOptionsDialogShown = onOptionsDialogShown
                )
                NextActionRow(
                    state = state,
                    saved = nextAction,
                    onSave = onNextActionSave,
                    onOptionsDialogShown = onOptionsDialogShown
                )
                if (previousAction == HeadphoneAction.ADD_BOOKMARK || nextAction == HeadphoneAction.ADD_BOOKMARK) {
                    ConfirmationSoundRow(
                        saved = confirmationSound,
                        onSave = onConfirmationSoundSave
                    )
                }
            }
        }
    }

    @Composable
    private fun NextActionRow(
        state: HeadphoneControlsSettingsPageViewModel.UiState,
        saved: HeadphoneAction,
        onSave: (HeadphoneAction) -> Unit,
        onOptionsDialogShown: () -> Unit,
    ) {
        val iconColor = state.addBookmarkIconColor.toArgb()
        SettingRow(
            primaryText = stringResource(LR.string.settings_headphone_controls_action_next),
            secondaryText = stringResource(headphoneActionToStringRes(saved)),
            icon = painterResource(IR.drawable.ic_skip_forward),
            modifier = Modifier
                .clickable {
                    onOptionsDialogShown()
                    val optionsDialog = OptionsDialog()
                        .setTitle(getString(LR.string.settings_headphone_controls_action_next))
                        .setIconColor(iconColor)
                        .addCheckedOption(
                            titleId = headphoneActionToStringRes(HeadphoneAction.SKIP_FORWARD),
                            checked = saved == HeadphoneAction.SKIP_FORWARD
                        ) {
                            onSave(HeadphoneAction.SKIP_FORWARD)
                        }
                        .addCheckedOption(
                            titleId = headphoneActionToStringRes(HeadphoneAction.SKIP_BACK),
                            checked = saved == HeadphoneAction.SKIP_BACK
                        ) {
                            onSave(HeadphoneAction.SKIP_BACK)
                        }
                        .addCheckedOption(
                            imageId = state.addBookmarkIconId,
                            titleId = headphoneActionToStringRes(HeadphoneAction.ADD_BOOKMARK),
                            checked = saved == HeadphoneAction.ADD_BOOKMARK
                        ) {
                            onSave(HeadphoneAction.ADD_BOOKMARK)
                        }
                    optionsDialog.show(childFragmentManager, "action_next_options")
                }
        )
    }

    @Composable
    private fun PreviousActionRow(
        state: HeadphoneControlsSettingsPageViewModel.UiState,
        saved: HeadphoneAction,
        onSave: (HeadphoneAction) -> Unit,
        onOptionsDialogShown: () -> Unit,
    ) {
        val iconColor = state.addBookmarkIconColor.toArgb()
        SettingRow(
            primaryText = stringResource(LR.string.settings_headphone_controls_action_previous),
            secondaryText = stringResource(headphoneActionToStringRes(saved)),
            icon = painterResource(IR.drawable.ic_skip_back),
            modifier = Modifier
                .clickable {
                    onOptionsDialogShown()
                    val optionsDialog = OptionsDialog()
                        .setTitle(getString(LR.string.settings_headphone_controls_action_previous))
                        .setIconColor(iconColor)
                        .addCheckedOption(
                            titleId = headphoneActionToStringRes(HeadphoneAction.SKIP_BACK),
                            checked = saved == HeadphoneAction.SKIP_BACK
                        ) {
                            onSave(HeadphoneAction.SKIP_BACK)
                        }
                        .addCheckedOption(
                            titleId = headphoneActionToStringRes(HeadphoneAction.SKIP_FORWARD),
                            checked = saved == HeadphoneAction.SKIP_FORWARD
                        ) {
                            onSave(HeadphoneAction.SKIP_FORWARD)
                        }
                        .addCheckedOption(
                            imageId = state.addBookmarkIconId,
                            titleId = headphoneActionToStringRes(HeadphoneAction.ADD_BOOKMARK),
                            checked = saved == HeadphoneAction.ADD_BOOKMARK
                        ) {
                            onSave(HeadphoneAction.ADD_BOOKMARK)
                        }
                    optionsDialog.show(childFragmentManager, "action_previous_options")
                }
                .padding(vertical = 6.dp)
        )
    }

    @Composable
    private fun ConfirmationSoundRow(
        saved: Boolean,
        onSave: (Boolean) -> Unit,
    ) {
        SettingRow(
            primaryText = stringResource(LR.string.settings_headphone_controls_confirmation_sound),
            secondaryText = stringResource(LR.string.settings_headphone_controls_confirmation_sound_summary),
            toggle = SettingRowToggle.Switch(checked = saved),
            modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) }
        )
    }

    private fun startUpsellFlow() {
        val source = OnboardingUpgradeSource.HEADPHONE_CONTROLS_SETTINGS
        val onboardingFlow = OnboardingFlow.Upsell(
            source = source,
            showPatronOnly = Feature.BOOKMARKS_ENABLED.tier == FeatureTier.Patron ||
                (Feature.BOOKMARKS_ENABLED.tier as? FeatureTier.Plus)?.patronExclusiveAccessRelease == ReleaseVersion.currentReleaseVersion,
        )
        OnboardingLauncher.openOnboardingFlow(activity, onboardingFlow)
    }

    @StringRes
    private fun headphoneActionToStringRes(action: HeadphoneAction) = when (action) {
        HeadphoneAction.ADD_BOOKMARK -> LR.string.settings_headphone_controls_choice_add_bookmark
        HeadphoneAction.SKIP_BACK -> LR.string.settings_headphone_controls_choice_skip_back
        HeadphoneAction.SKIP_FORWARD -> LR.string.settings_headphone_controls_choice_skip_forward
        HeadphoneAction.NEXT_CHAPTER -> LR.string.settings_headphone_controls_choice_next_chapter
        HeadphoneAction.PREVIOUS_CHAPTER -> LR.string.settings_headphone_controls_choice_previous_chapter
    }

    @Preview
    @Composable
    private fun HeadphoneControlsSettingsPagePreview(
        @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
    ) {
        AppThemeWithBackground(themeType) {
            Content(
                state = HeadphoneControlsSettingsPageViewModel.UiState(),
                previousAction = HeadphoneAction.SKIP_BACK,
                nextAction = HeadphoneAction.ADD_BOOKMARK,
                onPreviousActionSave = {},
                onNextActionSave = {},
                confirmationSound = true,
                onConfirmationSoundSave = {},
                onBackPressed = {},
                onOptionsDialogShown = {},
            )
        }
    }
}
