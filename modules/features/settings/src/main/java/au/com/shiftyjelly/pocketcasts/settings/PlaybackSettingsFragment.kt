package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
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
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonState
import au.com.shiftyjelly.pocketcasts.compose.components.DialogFrame
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRadioDialogRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
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

    @Inject lateinit var settings: Settings
    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppThemeWithBackground(theme.activeTheme) {
                PlaybackSettings(
                    settings = settings,
                    onBackClick = {
                        @Suppress("DEPRECATION")
                        activity?.onBackPressed()
                    },
                )
            }
        }
    }

    @Composable
    private fun PlaybackSettings(
        settings: Settings,
        onBackClick: () -> Unit,
    ) {

        LaunchedEffect(Unit) {
            analyticsTracker.track(AnalyticsEvent.SETTINGS_GENERAL_SHOWN)
        }

        Column {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_title_playback),
                onNavigationClick = onBackClick,
                bottomShadow = true,
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                SettingSection(heading = stringResource(LR.string.settings_general_defaults)) {

                    RowAction(
                        saved = settings.rowActionObservable
                            .subscribeAsState(settings.streamingMode())
                            .value,
                        onSave = {
                            analyticsTracker.track(
                                AnalyticsEvent.SETTINGS_GENERAL_ROW_ACTION_CHANGED,
                                mapOf(
                                    "value" to when (it) {
                                        true -> "play"
                                        false -> "download"
                                    }
                                )
                            )
                            settings.setStreamingMode(it)
                        }
                    )

                    UpNextSwipe(
                        saved = settings.upNextSwipeActionObservable
                            .subscribeAsState(settings.getUpNextSwipeAction())
                            .value,
                        onSave = {
                            analyticsTracker.track(
                                AnalyticsEvent.SETTINGS_GENERAL_UP_NEXT_SWIPE_CHANGED,
                                mapOf(
                                    "value" to when (it) {
                                        Settings.UpNextAction.PLAY_NEXT -> "play_next"
                                        Settings.UpNextAction.PLAY_LAST -> "play_last"
                                    }
                                )
                            )
                            settings.setUpNextSwipeAction(it)
                        }
                    )

                    PodcastEpisodeGrouping(
                        saved = settings.defaultPodcastGroupingFlow.collectAsState().value,
                        onSave = {
                            analyticsTracker.track(
                                AnalyticsEvent.SETTINGS_GENERAL_EPISODE_GROUPING_CHANGED,
                                mapOf(
                                    "value" to when (it) {
                                        PodcastGrouping.Downloaded -> "downloaded"
                                        PodcastGrouping.None -> "none"
                                        PodcastGrouping.Season -> "season"
                                        PodcastGrouping.Starred -> "starred"
                                        PodcastGrouping.Unplayed -> "unplayed"
                                    }
                                )
                            )
                            settings.setDefaultPodcastGrouping(it)
                            showSetAllGroupingDialog(it)
                        }
                    )

                    ShowArchived(
                        saved = settings.defaultShowArchivedFlow.collectAsState().value,
                        onSave = {
                            analyticsTracker.track(
                                AnalyticsEvent.SETTINGS_GENERAL_ARCHIVED_EPISODES_CHANGED,
                                mapOf(
                                    "value" to when (it) {
                                        true -> "show"
                                        false -> "hide"
                                    }
                                )
                            )
                            settings.setDefaultShowArchived(it)
                            showSetAllArchiveDialog(it)
                        }
                    )

                    SettingRow(
                        primaryText = stringResource(LR.string.settings_media_notification_controls),
                        secondaryText = stringResource(LR.string.settings_customize_buttons_displayed_in_android_13_notification_and_android_auto),
                        modifier = Modifier.clickable {
                            (activity as? FragmentHostListener)?.addFragment(MediaNotificationControlsFragment())
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
                        onSave = {
                            analyticsTracker.track(
                                AnalyticsEvent.SETTINGS_GENERAL_SKIP_FORWARD_CHANGED,
                                mapOf("value" to it)
                            )
                            settings.setSkipForwardNeedsSync(true)
                            settings.setSkipForwardInSec(it)
                        }
                    )

                    // Skip back time
                    SkipTime(
                        primaryText = stringResource(LR.string.settings_skip_back_time),
                        saved = settings.skipBackwardInSecsObservable
                            .subscribeAsState(settings.getSkipBackwardInSecs())
                            .value,
                        onSave = {
                            analyticsTracker.track(
                                AnalyticsEvent.SETTINGS_GENERAL_SKIP_BACK_CHANGED,
                                mapOf("value" to it)
                            )
                            settings.setSkipBackNeedsSync(true)
                            settings.setSkipBackwardInSec(it)
                        }
                    )

                    KeepScreenAwake(
                        saved = settings.keepScreenAwakeFlow.collectAsState().value,
                        onSave = {
                            analyticsTracker.track(
                                AnalyticsEvent.SETTINGS_GENERAL_KEEP_SCREEN_AWAKE_TOGGLED,
                                mapOf("enabled" to it)
                            )
                            settings.setKeepScreenAwake(it)
                        }
                    )

                    OpenPlayerAutomatically(
                        saved = settings.openPlayerAutomaticallyFlow.collectAsState().value,
                        onSave = {
                            analyticsTracker.track(
                                AnalyticsEvent.SETTINGS_GENERAL_OPEN_PLAYER_AUTOMATICALLY_TOGGLED,
                                mapOf("enabled" to it)
                            )
                            settings.setOpenPlayerAutomatically(it)
                        }
                    )

                    IntelligentPlaybackResumption(
                        saved = settings.intelligentPlaybackResumptionFlow.collectAsState().value,
                        onSave = {
                            analyticsTracker.track(
                                AnalyticsEvent.SETTINGS_GENERAL_INTELLIGENT_PLAYBACK_TOGGLED,
                                mapOf("enabled" to it)
                            )
                            settings.setIntelligentPlaybackResumption(it)
                        }
                    )

                    PlayUpNextOnTap(
                        saved = settings.tapOnUpNextShouldPlayFlow.collectAsState().value,
                        onSave = {
                            analyticsTracker.track(
                                AnalyticsEvent.SETTINGS_GENERAL_PLAY_UP_NEXT_ON_TAP_TOGGLED,
                                mapOf("enabled" to it)
                            )
                            settings.setTapOnUpNextShouldPlay(it)
                        }
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
            optionToLocalisedString = { getString(rowActionToStringRes(it)) },
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
            optionToLocalisedString = { getString(upNextActionToStringRes(it)) },
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
            optionToLocalisedString = { getString(podcastGroupingToStringRes(it)) },
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
        optionToLocalisedString = {
            when (it) {
                true -> getString(LR.string.settings_show_archived_action_show)
                false -> getString(LR.string.settings_show_archived_action_hide)
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
            toggle = SettingRowToggle.Switch(checked = saved),
            modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) }
        )

    @Composable
    private fun OpenPlayerAutomatically(saved: Boolean, onSave: (Boolean) -> Unit) =
        SettingRow(
            primaryText = stringResource(id = LR.string.settings_open_player_automatically),
            secondaryText = stringResource(id = LR.string.settings_open_player_automatically_summary),
            toggle = SettingRowToggle.Switch(checked = saved),
            modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) }
        )

    @Composable
    private fun IntelligentPlaybackResumption(saved: Boolean, onSave: (Boolean) -> Unit) =
        SettingRow(
            primaryText = stringResource(LR.string.settings_playback_resumption),
            secondaryText = stringResource(LR.string.settings_playback_resumption_summary),
            toggle = SettingRowToggle.Switch(checked = saved),
            modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) }
        )

    @Composable
    private fun PlayUpNextOnTap(saved: Boolean, onSave: (Boolean) -> Unit) =
        SettingRow(
            primaryText = stringResource(LR.string.settings_up_next_tap),
            secondaryText = stringResource(LR.string.settings_up_next_tap_summary),
            toggle = SettingRowToggle.Switch(checked = saved),
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
