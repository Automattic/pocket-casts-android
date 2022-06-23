package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonState
import au.com.shiftyjelly.pocketcasts.compose.components.DialogFrame
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRadioDialogRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.isPositive
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PlaybackSettingsFragment : BaseFragment() {

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var podcastManager: PodcastManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme(theme.activeTheme) {
                PlaybackSettings(
                    settings = settings,
                    onBackClick = { activity?.onBackPressed() },
                )
            }
        }
    }

    @Composable
    private fun PlaybackSettings(
        settings: Settings,
        onBackClick: () -> Unit,
    ) {
        Column {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_title_playback),
                onNavigationClick = onBackClick,
                bottomShadow = true,
            )
            Column(
                modifier = Modifier
                    .background(MaterialTheme.theme.colors.primaryUi02)
                    .verticalScroll(rememberScrollState())
            ) {
                SettingSection(heading = stringResource(LR.string.settings_general_defaults)) {

                    RowAction(
                        saved = settings.rowActionObservable
                            .subscribeAsState(settings.streamingMode())
                            .value,
                        onSave = settings::setStreamingMode
                    )

                    UpNextSwipe(
                        saved = settings.upNextSwipeActionObservable
                            .subscribeAsState(settings.getUpNextSwipeAction())
                            .value,
                        onSave = settings::setUpNextSwipeAction
                    )

                    PodcastEpisodeGrouping(
                        saved = settings.defaultPodcastGroupingFlow.collectAsState().value,
                        onSave = {
                            settings.setDefaultPodcastGrouping(it)
                            showSetAllGroupingDialog(it)
                        }
                    )

                    ShowArchived(
                        saved = settings.defaultShowArchivedFlow.collectAsState().value,
                        onSave = {
                            settings.setDefaultShowArchived(it)
                            showSetAllArchiveDialog(it)
                        }
                    )
                }

                SettingSection(heading = stringResource(LR.string.settings_general_player)) {

                    // Skip forward time
                    SkipTime(
                        primaryText = stringResource(LR.string.settings_skip_forward_time),
                        saved = settings.skipForwardInSecsObservable
                            .subscribeAsState(settings.getSkipForwardInSecs())
                            .value,
                        onSave = settings::setSkipForwardInSec
                    )

                    // Skip back time
                    SkipTime(
                        primaryText = stringResource(LR.string.settings_skip_back_time),
                        saved = settings.skipBackwardInSecsObservable
                            .subscribeAsState(settings.getSkipForwardInSecs())
                            .value,
                        onSave = settings::setSkipBackwardInSec
                    )

                    KeepScreenAwake(
                        saved = settings.keepScreenAwakeFlow.collectAsState().value,
                        onSave = settings::setKeepScreenAwake
                    )

                    IntelligentPlaybackResumption(
                        saved = settings.intelligentPlaybackResumptionFlow.collectAsState().value,
                        onSave = settings::setIntelligentPlaybackResumption
                    )

                    PlayUpNextOnTap(
                        saved = settings.tapOnUpNextShouldPlayFlow.collectAsState().value,
                        onSave = settings::setTapOnUpNextShouldPlay
                    )
                }
            }
        }
    }

    @Composable
    private fun RowAction(
        saved: Boolean,
        onSave: (Boolean) -> Unit
    ) {
        val savedString = stringResource(rowActionToStringRes(saved)).lowercase(Locale.getDefault())
        val secondaryText = stringResource(LR.string.settings_row_action_summary, savedString)
        SettingRadioDialogRow(
            primaryText = stringResource(LR.string.settings_row_action),
            secondaryText = secondaryText,
            options = listOf(true, false),
            savedOption = saved,
            onSave = onSave,
            optionToStringRes = ::rowActionToStringRes,
        )
    }

    @StringRes
    private fun rowActionToStringRes(value: Boolean) = when (value) {
        true -> LR.string.settings_row_action_play
        false -> LR.string.settings_row_action_download
    }

    @Composable
    private fun UpNextSwipe(
        saved: Settings.UpNextAction,
        onSave: (Settings.UpNextAction) -> Unit
    ) {
        val savedString =
            stringResource(upNextActionToStringRes(saved)).lowercase(Locale.getDefault())
        val secondaryText = stringResource(LR.string.settings_up_next_swipe_summary, savedString)
        SettingRadioDialogRow(
            primaryText = stringResource(LR.string.settings_up_next_swipe),
            secondaryText = secondaryText,
            options = listOf(Settings.UpNextAction.PLAY_NEXT, Settings.UpNextAction.PLAY_LAST),
            savedOption = saved,
            optionToStringRes = ::upNextActionToStringRes,
            onSave = onSave
        )
    }

    @StringRes
    private fun upNextActionToStringRes(a: Settings.UpNextAction) = when (a) {
        Settings.UpNextAction.PLAY_NEXT -> LR.string.settings_up_next_swipe_play_next
        Settings.UpNextAction.PLAY_LAST -> LR.string.settings_up_next_swipe_play_last
    }

    @Composable
    private fun PodcastEpisodeGrouping(
        saved: PodcastGrouping,
        onSave: (PodcastGrouping) -> Unit
    ) {
        SettingRadioDialogRow(
            primaryText = stringResource(LR.string.settings_podcast_episode_grouping),
            secondaryText = when (saved) {
                PodcastGrouping.None -> stringResource(LR.string.settings_podcast_episode_grouping_summary_none)
                else -> {
                    val selected = stringResource(saved.groupName).lowercase(Locale.getDefault())
                    stringResource(LR.string.settings_podcast_episode_grouping_summary, selected)
                }
            },
            options = PodcastGrouping.All,
            savedOption = saved,
            optionToStringRes = ::podcastGroupingToStringRes,
            onSave = onSave,
        )
    }

    @StringRes
    private fun podcastGroupingToStringRes(grouping: PodcastGrouping) = when (grouping) {
        PodcastGrouping.Downloaded -> LR.string.settings_podcast_episode_grouping_title_downloaded
        PodcastGrouping.None -> LR.string.settings_podcast_episode_grouping_title_none
        PodcastGrouping.Season -> LR.string.settings_podcast_episode_grouping_title_season
        PodcastGrouping.Starred -> LR.string.settings_podcast_episode_grouping_title_starred
        PodcastGrouping.Unplayed -> LR.string.settings_podcast_episode_grouping_title_unplayed
    }

    @Composable
    private fun ShowArchived(
        saved: Boolean,
        onSave: (Boolean) -> Unit
    ) = SettingRadioDialogRow(
        primaryText = stringResource(LR.string.settings_show_archived),
        secondaryText = when (saved) {
            true -> getString(LR.string.settings_show_archived_summary_show)
            false -> getString(LR.string.settings_show_archived_summary_hide)
        },
        options = listOf(false, true),
        savedOption = saved,
        onSave = onSave,
        optionToStringRes = {
            when (it) {
                true -> LR.string.settings_show_archived_action_show
                false -> LR.string.settings_show_archived_action_hide
            }
        },
    )

    @Composable
    private fun SkipTime(
        primaryText: String,
        saved: Int,
        onSave: (Int) -> Unit
    ) {

        var showDialog by remember { mutableStateOf(false) }

        SettingRow(
            primaryText = primaryText,
            secondaryText = stringResource(LR.string.seconds_plural, saved),
            modifier = Modifier.clickable { showDialog = true }
        ) {
            if (showDialog) {

                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    // delay apparently needed to ensure the soft keyboard opens
                    delay(100)
                    focusRequester.requestFocus()
                }

                var value by remember {
                    mutableStateOf(
                        TextFieldValue(
                            text = saved.toString(),
                            selection = TextRange(0, saved.toString().length)
                        )
                    )
                }

                val onFinish = {
                    val saveableValue = value.text.toPositiveNumberOrNull()
                    if (saveableValue != null) {
                        onSave(saveableValue)
                        showDialog = false
                    }
                }

                DialogFrame(
                    title = primaryText,
                    buttons = listOf(
                        DialogButtonState(
                            text = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.cancel).uppercase(
                                Locale.getDefault()
                            ),
                            onClick = { showDialog = false }
                        ),
                        DialogButtonState(
                            text = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.ok),
                            onClick = onFinish,
                            enabled = value.text.toPositiveNumberOrNull() != null
                        )
                    ),
                    onDismissRequest = { showDialog = false }
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {
                            if (it.text.isEmpty()) {
                                value = it
                            } else {
                                val positiveNumber = it.text.toPositiveNumberOrNull()
                                if (positiveNumber != null) {
                                    value = it.copy(text = positiveNumber.toString())
                                }
                            }
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MaterialTheme.theme.colors.primaryText01,
                            placeholderColor = MaterialTheme.theme.colors.primaryText02,
                            backgroundColor = MaterialTheme.theme.colors.primaryUi01
                        ),
                        label = {
                            Text(stringResource(LR.string.seconds_label))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        keyboardActions = KeyboardActions { onFinish() },
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .focusRequester(focusRequester)
                    )
                }
            }
        }
    }

    private fun String?.toPositiveNumberOrNull(): Int? {
        return this?.toIntOrNull().let { int ->
            if (int.isPositive()) int else null
        }
    }

    @Composable
    private fun KeepScreenAwake(saved: Boolean, onSave: (Boolean) -> Unit) =
        SettingRow(
            primaryText = stringResource(LR.string.settings_keep_screen_awake),
            secondaryText = stringResource(LR.string.settings_keep_screen_awake_summary),
            switchState = saved,
            modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) }
        )

    @Composable
    private fun IntelligentPlaybackResumption(saved: Boolean, onSave: (Boolean) -> Unit) =
        SettingRow(
            primaryText = stringResource(LR.string.settings_playback_resumption),
            secondaryText = stringResource(LR.string.settings_playback_resumption_summary),
            switchState = saved,
            modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) }
        )

    @Composable
    private fun PlayUpNextOnTap(saved: Boolean, onSave: (Boolean) -> Unit) =
        SettingRow(
            primaryText = stringResource(LR.string.settings_up_next_tap),
            secondaryText = stringResource(LR.string.settings_up_next_tap_summary),
            switchState = saved,
            modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) }
        )

    @OptIn(DelicateCoroutinesApi::class)
    private fun showSetAllGroupingDialog(grouping: PodcastGrouping) {
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.settings_apply_to_existing)))
            .setSecondaryButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.settings_no_thanks)))
            .setTitle(getString(LR.string.settings_apply_to_existing_podcasts))
            .setSummary(
                getString(
                    LR.string.settings_apply_group_by,
                    getString(grouping.groupName).lowercase()
                )
            )
            .setIconId(R.drawable.ic_podcasts)
            .setOnConfirm {
                GlobalScope.launch {
                    podcastManager.updateGroupingForAll(grouping)
                }
            }
            .show(parentFragmentManager, "podcast_grouping_set_all_warning")
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun showSetAllArchiveDialog(shouldShow: Boolean) {
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.settings_apply_to_existing)))
            .setSecondaryButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.settings_no_thanks)))
            .setTitle(getString(LR.string.settings_apply_to_existing_podcasts))
            .setSummary(getString(if (shouldShow) LR.string.settings_apply_archived_show else LR.string.settings_apply_archived_hide))
            .setIconId(R.drawable.ic_podcasts)
            .setOnConfirm {
                GlobalScope.launch {
                    podcastManager.updateAllShowArchived(shouldShow)
                }
            }
            .show(parentFragmentManager, "podcast_grouping_archive_warning")
    }
}
