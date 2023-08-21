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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
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
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
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

    private val isAddBookmarkEnabled: Boolean
        get() = FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED) &&
            (settings.getCachedSubscription() as? SubscriptionStatus.Paid)?.tier == SubscriptionTier.PATRON

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppThemeWithBackground(theme.activeTheme) {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                val previousAction = settings.headphonePreviousActionFlow.collectAsState().value
                val nextAction = settings.headphoneNextActionFlow.collectAsState().value
                val confirmationSound =
                    settings.headphonePlayBookmarkConfirmationSoundFlow.collectAsState().value

                val viewModel = hiltViewModel<HeadphoneControlsSettingsPageViewModel>()

                CallOnce {
                    viewModel.onShown()
                }

                HeadphoneControlsSettingsPage(
                    previousAction = previousAction,
                    nextAction = nextAction,
                    onNextActionSave = {
                        settings.setHeadphoneControlsNextAction(it)
                        viewModel.onNextActionChanged(it)
                    },
                    onPreviousActionSave = {
                        settings.setHeadphoneControlsPreviousAction(it)
                        viewModel.onPreviousActionChanged(it)
                    },
                    confirmationSound = confirmationSound,
                    onConfirmationSoundSave = {
                        settings.setHeadphoneControlsPlayBookmarkConfirmationSound(it)
                        if (settings.getHeadphoneControlsPlayBookmarkConfirmationSound()) {
                            playbackManager.playTone()
                        }
                        viewModel.onConfirmationSoundChanged(it)
                    },
                    onBackPressed = {
                        @Suppress("DEPRECATION")
                        activity?.onBackPressed()
                    }
                )
            }
        }
    }

    @Composable
    private fun HeadphoneControlsSettingsPage(
        previousAction: HeadphoneAction,
        nextAction: HeadphoneAction,
        onPreviousActionSave: (HeadphoneAction) -> Unit,
        onNextActionSave: (HeadphoneAction) -> Unit,
        confirmationSound: Boolean,
        onConfirmationSoundSave: (Boolean) -> Unit,
        onBackPressed: () -> Unit,
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
                NextActionRow(
                    saved = nextAction,
                    onSave = onNextActionSave
                )
                PreviousActionRow(
                    saved = previousAction,
                    onSave = onPreviousActionSave
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
        saved: HeadphoneAction,
        onSave: (HeadphoneAction) -> Unit,
    ) {
        SettingRow(
            primaryText = stringResource(LR.string.settings_headphone_controls_action_next),
            secondaryText = stringResource(headphoneActionToStringRes(saved)),
            icon = painterResource(IR.drawable.ic_skip_forward),
            modifier = Modifier
                .clickable {
                    var optionsDialog = OptionsDialog()
                        .setTitle(getString(LR.string.settings_headphone_controls_action_next))
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

                    if (isAddBookmarkEnabled) {
                        optionsDialog = optionsDialog.addCheckedOption(
                            titleId = headphoneActionToStringRes(HeadphoneAction.ADD_BOOKMARK),
                            checked = saved == HeadphoneAction.ADD_BOOKMARK,
                        ) {
                            onSave(HeadphoneAction.ADD_BOOKMARK)
                        }
                    }

                    optionsDialog.show(childFragmentManager, "action_next_options")
                }
        )
    }

    @Composable
    private fun PreviousActionRow(
        saved: HeadphoneAction,
        onSave: (HeadphoneAction) -> Unit,
    ) {
        SettingRow(
            primaryText = stringResource(LR.string.settings_headphone_controls_action_previous),
            secondaryText = stringResource(headphoneActionToStringRes(saved)),
            icon = painterResource(IR.drawable.ic_skip_back),
            modifier = Modifier
                .clickable {
                    var optionsDialog = OptionsDialog()
                        .setTitle(getString(LR.string.settings_headphone_controls_action_previous))
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

                    if (isAddBookmarkEnabled) {
                        optionsDialog = optionsDialog.addCheckedOption(
                            titleId = headphoneActionToStringRes(HeadphoneAction.ADD_BOOKMARK),
                            checked = saved == HeadphoneAction.ADD_BOOKMARK,
                        ) {
                            onSave(HeadphoneAction.ADD_BOOKMARK)
                        }
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
    private fun OnboardingCreateAccountPagePreview(
        @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
    ) {
        AppThemeWithBackground(themeType) {
            HeadphoneControlsSettingsPage(
                previousAction = HeadphoneAction.SKIP_BACK,
                nextAction = HeadphoneAction.ADD_BOOKMARK,
                onPreviousActionSave = {},
                onNextActionSave = {},
                confirmationSound = true,
                onConfirmationSoundSave = {},
                onBackPressed = {}
            )
        }
    }
}
