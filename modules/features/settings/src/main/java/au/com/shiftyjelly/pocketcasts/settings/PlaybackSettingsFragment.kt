package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.FormFieldDialog
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRadioDialogRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSectionHeader
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.notification.MediaActionsFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PlaybackSettingsFragment : BaseFragment() {

    companion object {
        const val SCROLL_TO_SLEEP_TIMER = "scrollToSleepTimer"
    }

    @Inject lateinit var settings: Settings

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val scrollToSleepTimer = arguments?.getBoolean(SCROLL_TO_SLEEP_TIMER, false) ?: false

        AppThemeWithBackground(theme.activeTheme) {
            val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)
            PlaybackSettings(
                settings = settings,
                onBackPress = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                scrollToSleepTimer = scrollToSleepTimer,
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
            )
        }
    }

    @Composable
    private fun PlaybackSettings(
        settings: Settings,
        onBackPress: () -> Unit,
        scrollToSleepTimer: Boolean,
        bottomInset: Dp,
    ) {
        val listState = rememberLazyListState()
        val settingsItemsKey = SettingsItems.entries

        val sleepTimerIndex = settingsItemsKey.indexOf(SettingsItems.SETTINGS_HEADER_SLEEP_TIMER)
        LaunchedEffect(scrollToSleepTimer) {
            if (scrollToSleepTimer) {
                listState.animateScrollToItem(index = sleepTimerIndex)
            }
        }

        LaunchedEffect(Unit) {
            analyticsTracker.track(AnalyticsEvent.SETTINGS_GENERAL_SHOWN)
        }

        Column {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_title_playback),
                onNavigationClick = onBackPress,
                bottomShadow = true,
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .background(MaterialTheme.theme.colors.primaryUi02),
                contentPadding = PaddingValues(bottom = bottomInset),
            ) {
                items(settingsItemsKey.size) { item ->
                    when (settingsItemsKey[item]) {
                        SettingsItems.SETTINGS_HEADER_DEFAULTS -> {
                            Column {
                                Spacer(modifier = Modifier.height(SettingsSection.verticalPadding))
                                SettingSectionHeader(
                                    text = stringResource(LR.string.settings_general_defaults),
                                    indent = false,
                                )
                            }
                        }

                        SettingsItems.SETTINGS_ROW_ACTION -> {
                            RowAction(
                                saved = settings.streamingMode.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_ROW_ACTION_CHANGED,
                                        mapOf(
                                            "value" to when (it) {
                                                true -> "play"
                                                false -> "download"
                                            },
                                        ),
                                    )
                                    settings.streamingMode.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_UP_NEXT_SWIPE -> {
                            UpNextSwipe(
                                saved = settings.upNextSwipe.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_UP_NEXT_SWIPE_CHANGED,
                                        mapOf(
                                            "value" to when (it) {
                                                Settings.UpNextAction.PLAY_NEXT -> "play_next"
                                                Settings.UpNextAction.PLAY_LAST -> "play_last"
                                            },
                                        ),
                                    )
                                    settings.upNextSwipe.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_EPISODE_GROUPING -> {
                            PodcastEpisodeGrouping(
                                saved = settings.podcastGroupingDefault.flow.collectAsState().value,
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
                                            },
                                        ),
                                    )
                                    settings.podcastGroupingDefault.set(it, updateModifiedAt = true)
                                    showSetAllGroupingDialog(it)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_ARCHIVED_EPISODES -> {
                            ShowArchived(
                                saved = settings.showArchivedDefault.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_ARCHIVED_EPISODES_CHANGED,
                                        mapOf(
                                            "value" to when (it) {
                                                true -> "show"
                                                false -> "hide"
                                            },
                                        ),
                                    )
                                    settings.showArchivedDefault.set(it, updateModifiedAt = true)
                                    showSetAllArchiveDialog(it)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_MEDIA_NOTIFICATION_CONTROLS -> {
                            SettingRow(
                                primaryText = stringResource(LR.string.settings_media_notification_controls),
                                secondaryText = stringResource(LR.string.settings_customize_buttons_displayed_in_android_13_notification_and_android_auto),
                                modifier = Modifier.clickable {
                                    (activity as? FragmentHostListener)?.addFragment(MediaActionsFragment())
                                },
                                indent = false,
                            )
                        }

                        SettingsItems.SETTINGS_HEADER_PLAYER -> {
                            Column {
                                Spacer(modifier = Modifier.height(SettingsSection.verticalPadding))
                                SettingSectionHeader(
                                    text = stringResource(LR.string.settings_general_player),
                                    indent = false,
                                )
                            }
                        }

                        SettingsItems.SETTINGS_SKIP_FORWARD_TIME -> {
                            SkipTime(
                                primaryText = stringResource(LR.string.settings_skip_forward_time),
                                saved = settings.skipForwardInSecs.flow
                                    .collectAsState()
                                    .value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_SKIP_FORWARD_CHANGED,
                                        mapOf("value" to it),
                                    )
                                    settings.skipForwardInSecs.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_SKIP_BACK_TIME -> {
                            SkipTime(
                                primaryText = stringResource(LR.string.settings_skip_back_time),
                                saved = settings.skipBackInSecs.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_SKIP_BACK_CHANGED,
                                        mapOf("value" to it),
                                    )
                                    settings.skipBackInSecs.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_KEEP_SCREEN_AWAKE -> {
                            KeepScreenAwake(
                                saved = settings.keepScreenAwake.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_KEEP_SCREEN_AWAKE_TOGGLED,
                                        mapOf("enabled" to it),
                                    )
                                    settings.keepScreenAwake.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_OPEN_PLAYER_AUTOMATICALLY -> {
                            OpenPlayerAutomatically(
                                saved = settings.openPlayerAutomatically.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_OPEN_PLAYER_AUTOMATICALLY_TOGGLED,
                                        mapOf("enabled" to it),
                                    )
                                    settings.openPlayerAutomatically.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_INTELLIGENT_PLAYBACK -> {
                            IntelligentPlaybackResumption(
                                saved = settings.intelligentPlaybackResumption.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_INTELLIGENT_PLAYBACK_TOGGLED,
                                        mapOf("enabled" to it),
                                    )
                                    settings.intelligentPlaybackResumption.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_PLAY_UP_NEXT_EPISODE -> {
                            PlayUpNextOnTap(
                                saved = settings.tapOnUpNextShouldPlay.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_PLAY_UP_NEXT_ON_TAP_TOGGLED,
                                        mapOf("enabled" to it),
                                    )
                                    settings.tapOnUpNextShouldPlay.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_ADJUST_REMAINING_TIME -> {
                            UseRealTimeForPlaybackRemainingTime(
                                saved = settings.useRealTimeForPlaybackRemaingTime.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_USE_REAL_TIME_FOR_PLAYBACK_REMAINING_TIME,
                                        mapOf("enabled" to it),
                                    )
                                    settings.useRealTimeForPlaybackRemaingTime.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_GENERAL_AUTOPLAY -> {
                            AutoPlayNextOnEmpty(
                                saved = settings.autoPlayNextEpisodeOnEmpty.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_AUTOPLAY_TOGGLED,
                                        mapOf("enabled" to it),
                                    )
                                    settings.autoPlayNextEpisodeOnEmpty.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_HEADER_SLEEP_TIMER -> {
                            Column {
                                Spacer(modifier = Modifier.height(SettingsSection.verticalPadding))
                                SettingSectionHeader(
                                    text = stringResource(LR.string.settings_general_sleep_timer),
                                    indent = false,
                                )
                            }
                        }

                        SettingsItems.SETTINGS_SLEEP_TIMER_RESTART -> {
                            AutoSleepTimerRestart(
                                saved = settings.autoSleepTimerRestart.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_AUTO_SLEEP_TIMER_RESTART_TOGGLED,
                                        mapOf("enabled" to it),
                                    )
                                    settings.autoSleepTimerRestart.set(it, updateModifiedAt = true)
                                },
                            )
                        }

                        SettingsItems.SETTINGS_SLEEP_TIMER_SHAKE -> {
                            ShakeToResetSleepTimer(
                                saved = settings.shakeToResetSleepTimer.flow.collectAsState().value,
                                onSave = {
                                    analyticsTracker.track(
                                        AnalyticsEvent.SETTINGS_GENERAL_SHAKE_TO_RESET_SLEEP_TIMER_TOGGLED,
                                        mapOf("enabled" to it),
                                    )
                                    settings.shakeToResetSleepTimer.set(it, updateModifiedAt = true)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RowAction(
        saved: Boolean,
        onSave: (Boolean) -> Unit,
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
            indent = false,
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
        onSave: (Settings.UpNextAction) -> Unit,
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
            onSave = onSave,
            indent = false,
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
        onSave: (PodcastGrouping) -> Unit,
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
            indent = false,
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
        onSave: (Boolean) -> Unit,
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
        indent = false,
    )

    @Composable
    private fun SkipTime(
        primaryText: String,
        saved: Int,
        onSave: (Int) -> Unit,
    ) {
        var showDialog by remember { mutableStateOf(false) }

        SettingRow(
            primaryText = primaryText,
            secondaryText = stringResource(LR.string.seconds_plural, saved),
            modifier = Modifier.clickable { showDialog = true },
            indent = false,
        ) {
            if (showDialog) {
                FormFieldDialog(
                    title = primaryText,
                    placeholder = stringResource(LR.string.seconds_label),
                    initialValue = saved.toString(),
                    keyboardType = KeyboardType.Number,
                    onConfirm = { value ->
                        val intValue = value.toIntOrNull()?.takeIf { it >= 0 }
                        if (intValue != null) {
                            onSave(intValue)
                        }
                    },
                    onDismissRequest = { showDialog = false },
                    isSaveEnabled = { value -> value.toIntOrNull()?.takeIf { it >= 0 } != null },
                )
            }
        }
    }

    @Composable
    private fun KeepScreenAwake(saved: Boolean, onSave: (Boolean) -> Unit) = SettingRow(
        primaryText = stringResource(LR.string.settings_keep_screen_awake),
        secondaryText = stringResource(LR.string.settings_keep_screen_awake_summary),
        toggle = SettingRowToggle.Switch(checked = saved),
        modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) },
        indent = false,
    )

    @Composable
    private fun OpenPlayerAutomatically(saved: Boolean, onSave: (Boolean) -> Unit) = SettingRow(
        primaryText = stringResource(id = LR.string.settings_open_player_automatically),
        secondaryText = stringResource(id = LR.string.settings_open_player_automatically_summary),
        toggle = SettingRowToggle.Switch(checked = saved),
        modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) },
        indent = false,
    )

    @Composable
    private fun IntelligentPlaybackResumption(saved: Boolean, onSave: (Boolean) -> Unit) = SettingRow(
        primaryText = stringResource(LR.string.settings_playback_resumption),
        secondaryText = stringResource(LR.string.settings_playback_resumption_summary),
        toggle = SettingRowToggle.Switch(checked = saved),
        modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) },
        indent = false,
    )

    @Composable
    private fun PlayUpNextOnTap(saved: Boolean, onSave: (Boolean) -> Unit) = SettingRow(
        primaryText = stringResource(LR.string.settings_up_next_tap),
        secondaryText = stringResource(LR.string.settings_up_next_tap_summary),
        toggle = SettingRowToggle.Switch(checked = saved),
        modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) },
        indent = false,
    )

    @Composable
    private fun ShakeToResetSleepTimer(saved: Boolean, onSave: (Boolean) -> Unit) = SettingRow(
        primaryText = stringResource(LR.string.settings_sleep_timer_shake_to_reset),
        secondaryText = stringResource(LR.string.settings_sleep_timer_shake_to_reset_summary),
        toggle = SettingRowToggle.Switch(checked = saved),
        modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) },
        indent = false,
    )

    @Composable
    private fun AutoSleepTimerRestart(saved: Boolean, onSave: (Boolean) -> Unit) = SettingRow(
        primaryText = stringResource(LR.string.settings_sleep_timer_auto_restart),
        secondaryText = stringResource(LR.string.settings_sleep_timer_auto_restart_summary),
        toggle = SettingRowToggle.Switch(checked = saved),
        modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) },
        indent = false,
    )

    @Composable
    private fun AutoPlayNextOnEmpty(
        saved: Boolean,
        onSave: (Boolean) -> Unit,
    ) = SettingRow(
        primaryText = stringResource(LR.string.settings_autoplay),
        secondaryText = stringResource(LR.string.settings_continuous_playback_summary),
        toggle = SettingRowToggle.Switch(checked = saved),
        modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) },
        indent = false,
    )

    @Composable
    private fun UseRealTimeForPlaybackRemainingTime(
        saved: Boolean,
        onSave: (Boolean) -> Unit,
    ) = SettingRow(
        primaryText = stringResource(LR.string.settings_real_time_playback),
        secondaryText = stringResource(LR.string.settings_real_time_playback_summary),
        toggle = SettingRowToggle.Switch(checked = saved),
        modifier = Modifier.toggleable(value = saved, role = Role.Switch) { onSave(!saved) },
        indent = false,
    )

    private fun showSetAllGroupingDialog(grouping: PodcastGrouping) {
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.settings_apply_to_existing)))
            .setSecondaryButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.settings_no_thanks)))
            .setTitle(getString(LR.string.settings_apply_to_existing_podcasts))
            .setSummary(
                getString(
                    LR.string.settings_apply_group_by,
                    getString(grouping.groupName).lowercase(),
                ),
            )
            .setIconId(R.drawable.ic_podcasts)
            .setOnSecondary {
                analyticsTracker.track(AnalyticsEvent.SETTINGS_GENERAL_EPISODE_GROUPING_DO_NOT_APPLY_TO_EXISTING)
            }
            .setOnConfirm {
                analyticsTracker.track(AnalyticsEvent.SETTINGS_GENERAL_EPISODE_GROUPING_APPLY_TO_EXISTING)
                applicationScope.launch {
                    podcastManager.updateGroupingForAllBlocking(grouping)
                }
            }
            .show(parentFragmentManager, "podcast_grouping_set_all_warning")
    }

    private fun showSetAllArchiveDialog(shouldShow: Boolean) {
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.settings_apply_to_existing)))
            .setSecondaryButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.settings_no_thanks)))
            .setTitle(getString(LR.string.settings_apply_to_existing_podcasts))
            .setSummary(getString(if (shouldShow) LR.string.settings_apply_archived_show else LR.string.settings_apply_archived_hide))
            .setIconId(R.drawable.ic_podcasts)
            .setOnSecondary {
                analyticsTracker.track(AnalyticsEvent.SETTINGS_GENERAL_ARCHIVED_EPISODES_DO_NOT_APPLY_TO_EXISTING)
            }
            .setOnConfirm {
                analyticsTracker.track(AnalyticsEvent.SETTINGS_GENERAL_ARCHIVED_EPISODES_APPLY_TO_EXISTING)
                applicationScope.launch {
                    podcastManager.updateAllShowArchived(shouldShow)
                }
            }
            .show(parentFragmentManager, "podcast_grouping_archive_warning")
    }
}

private enum class SettingsItems {
    SETTINGS_HEADER_DEFAULTS,
    SETTINGS_ROW_ACTION,
    SETTINGS_UP_NEXT_SWIPE,
    SETTINGS_EPISODE_GROUPING,
    SETTINGS_ARCHIVED_EPISODES,
    SETTINGS_MEDIA_NOTIFICATION_CONTROLS,
    SETTINGS_HEADER_PLAYER,
    SETTINGS_SKIP_FORWARD_TIME,
    SETTINGS_SKIP_BACK_TIME,
    SETTINGS_KEEP_SCREEN_AWAKE,
    SETTINGS_OPEN_PLAYER_AUTOMATICALLY,
    SETTINGS_INTELLIGENT_PLAYBACK,
    SETTINGS_PLAY_UP_NEXT_EPISODE,
    SETTINGS_ADJUST_REMAINING_TIME,
    SETTINGS_GENERAL_AUTOPLAY,
    SETTINGS_HEADER_SLEEP_TIMER,
    SETTINGS_SLEEP_TIMER_RESTART,
    SETTINGS_SLEEP_TIMER_SHAKE,
}
