package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.core.graphics.ColorUtils
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingInfoRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.dialogs.RadioOptionsDialog
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PodcastAutoArchiveFragment : BaseFragment() {
    private val viewModel by viewModels<PodcastAutoArchiveViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<PodcastAutoArchiveViewModel.Factory> { factory ->
                factory.create(args.podcastUuid)
            }
        },
    )

    private val args get() = extractArgs(arguments) ?: error("$NEW_INSTANCE_ARGS argument is missing. Fragment must be created using newInstance function")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            val state by viewModel.state.collectAsState(PodcastAutoArchiveViewModel.State())

            AppThemeWithBackground(themeType = theme.activeTheme) {
                AutoArchiveSettings(
                    state = state,
                    onBackPressed = {
                        @Suppress("DEPRECATION")
                        activity?.onBackPressed()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tintToolbar(args.toolbarColors.backgroundColor)
    }

    private fun tintToolbar(color: Int) {
        theme.updateWindowStatusBar(
            window = requireActivity().window,
            statusBarColor = StatusBarColor.Custom(color, isWhiteIcons = ColorUtils.calculateLuminance(color) < 0.5),
            context = requireActivity(),
        )
    }

    @Composable
    private fun AutoArchiveSettings(
        state: PodcastAutoArchiveViewModel.State,
        onBackPressed: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .verticalScroll(rememberScrollState()),
        ) {
            val toolbarColors = if (state.podcast != null) {
                ToolbarColors.podcast(state.podcast, theme)
            } else {
                args.toolbarColors
            }
            LaunchedEffect(toolbarColors.backgroundColor) {
                tintToolbar(toolbarColors.backgroundColor)
            }
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_title_auto_archive),
                onNavigationClick = onBackPressed,
                bottomShadow = true,
                iconColor = Color(toolbarColors.iconColor),
                textColor = Color(toolbarColors.titleColor),
                backgroundColor = Color(toolbarColors.backgroundColor),
            )
            GlobalOverrideSection(
                overrideGlobalSettings = state.overrideAutoArchiveSettings,
            )
            if (state.overrideAutoArchiveSettings) {
                AutoArchiveSection(
                    archiveAfterPlaying = state.archiveAfterPlaying,
                    archiveInactive = state.archiveInactive,
                )
                EpisodeLimitSection(
                    limit = state.episodeLimit,
                )
            }
        }
    }

    @Composable
    private fun GlobalOverrideSection(
        overrideGlobalSettings: Boolean,
        modifier: Modifier = Modifier,
    ) {
        SettingSection(
            showDivider = overrideGlobalSettings,
            modifier = modifier,
        ) {
            SettingRow(
                primaryText = stringResource(LR.string.podcast_settings_auto_archive_custom),
                secondaryText = stringResource(LR.string.podcast_settings_auto_archive_custom_summary),
                toggle = SettingRowToggle.Switch(checked = overrideGlobalSettings),
                modifier = Modifier.toggleable(value = overrideGlobalSettings, role = Role.Switch) { newValue ->
                    viewModel.updateGlobalOverride(newValue)
                },
            )
        }
    }

    @Composable
    private fun AutoArchiveSection(
        archiveAfterPlaying: AutoArchiveAfterPlaying,
        archiveInactive: AutoArchiveInactive,
        modifier: Modifier = Modifier,
    ) {
        var openArchiveAfterPlayingDialog by remember { mutableStateOf(false) }
        var openArchiveInactiveDialog by remember { mutableStateOf(false) }

        SettingSection(
            modifier = modifier,
        ) {
            SettingRow(
                primaryText = stringResource(LR.string.settings_archive_played),
                secondaryText = stringResource(archiveAfterPlaying.stringRes),
                toggle = SettingRowToggle.None,
                modifier = Modifier.clickable { openArchiveAfterPlayingDialog = true },
            )
            SettingRow(
                primaryText = stringResource(LR.string.settings_auto_archive_inactive),
                secondaryText = stringResource(archiveInactive.stringRes),
                toggle = SettingRowToggle.None,
                modifier = Modifier.clickable { openArchiveInactiveDialog = true },
            )
            SettingInfoRow(
                text = stringResource(LR.string.settings_auto_archive_time_limits),
            )
        }

        if (openArchiveAfterPlayingDialog) {
            AutoArchiveAfterPlayedDialog(
                archiveAfterPlaying = archiveAfterPlaying,
                onConfirm = { newValue ->
                    viewModel.updateAfterPlaying(newValue)
                    openArchiveAfterPlayingDialog = false
                },
                onDismiss = { openArchiveAfterPlayingDialog = false },
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
        if (openArchiveInactiveDialog) {
            AutoArchiveInactiveDialog(
                archiveInactive = archiveInactive,
                onConfirm = { newValue ->
                    viewModel.updateInactive(newValue)
                    openArchiveInactiveDialog = false
                },
                onDismiss = { openArchiveInactiveDialog = false },
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }

    @Composable
    private fun EpisodeLimitSection(
        limit: Int?,
        modifier: Modifier = Modifier,
    ) {
        var openEpisodeLimitDialog by remember { mutableStateOf(false) }

        SettingSection(
            showDivider = false,
            modifier = modifier,
        ) {
            SettingRow(
                primaryText = stringResource(LR.string.settings_auto_archive_episode_limit),
                secondaryText = stringResource(EpisodeLimits[limit] ?: LR.string.settings_auto_archive_limit_none),
                toggle = SettingRowToggle.None,
                modifier = Modifier.clickable { openEpisodeLimitDialog = true },
            )
            SettingInfoRow(
                text = stringResource(LR.string.settings_auto_archive_episode_limit_summary),
            )
        }

        if (openEpisodeLimitDialog) {
            EpisodeLimitDialog(
                limit = limit,
                onConfirm = { newValue ->
                    viewModel.updateEpisodeLimit(newValue)
                    openEpisodeLimitDialog = false
                },
                onDismiss = { openEpisodeLimitDialog = false },
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }

    @Composable
    private fun AutoArchiveAfterPlayedDialog(
        archiveAfterPlaying: AutoArchiveAfterPlaying,
        modifier: Modifier = Modifier,
        onConfirm: (AutoArchiveAfterPlaying) -> Unit,
        onDismiss: () -> Unit,
    ) {
        RadioOptionsDialog(
            title = stringResource(LR.string.podcast_settings_played_episodes),
            selectedOption = archiveAfterPlaying,
            allOptions = AutoArchiveAfterPlaying.All,
            optionName = { option -> stringResource(option.stringRes) },
            onSelectOption = onConfirm,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }

    @Composable
    private fun AutoArchiveInactiveDialog(
        archiveInactive: AutoArchiveInactive,
        modifier: Modifier = Modifier,
        onConfirm: (AutoArchiveInactive) -> Unit,
        onDismiss: () -> Unit,
    ) {
        RadioOptionsDialog(
            title = stringResource(LR.string.settings_inactive_episodes),
            selectedOption = archiveInactive,
            allOptions = AutoArchiveInactive.All,
            optionName = { option -> stringResource(option.stringRes) },
            onSelectOption = onConfirm,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }

    @Composable
    private fun EpisodeLimitDialog(
        limit: Int?,
        modifier: Modifier = Modifier,
        onConfirm: (Int?) -> Unit,
        onDismiss: () -> Unit,
    ) {
        RadioOptionsDialog(
            title = stringResource(LR.string.settings_auto_archive_episode_limit),
            selectedOption = limit,
            allOptions = EpisodeLimits.keys.toList(),
            optionName = { option -> stringResource(EpisodeLimits[option] ?: LR.string.settings_auto_archive_limit_none) },
            onSelectOption = onConfirm,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }

    @Parcelize
    data class PodcastAutoArchiveArgs(
        val podcastUuid: String,
        val toolbarColors: ToolbarColors,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "PodcastAutoArchiveFragmentArg"

        private val EpisodeLimits = mapOf(
            null to LR.string.settings_auto_archive_limit_none,
            1 to LR.string.settings_auto_archive_limit_1,
            2 to LR.string.settings_auto_archive_limit_2,
            5 to LR.string.settings_auto_archive_limit_5,
            10 to LR.string.settings_auto_archive_limit_10,
        )

        fun newInstance(
            podcastUuid: String,
            toolbarColors: ToolbarColors,
        ): PodcastAutoArchiveFragment {
            return PodcastAutoArchiveFragment().apply {
                arguments = bundleOf(
                    NEW_INSTANCE_ARGS to PodcastAutoArchiveArgs(
                        podcastUuid = podcastUuid,
                        toolbarColors = toolbarColors,
                    ),
                )
            }
        }

        private fun extractArgs(bundle: Bundle?): PodcastAutoArchiveArgs? = bundle?.let {
            BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, PodcastAutoArchiveArgs::class.java)
        }
    }
}
