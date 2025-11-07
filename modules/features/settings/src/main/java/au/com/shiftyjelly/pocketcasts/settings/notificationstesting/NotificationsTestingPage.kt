package au.com.shiftyjelly.pocketcasts.settings.notificationstesting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.FormFieldDialog
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRadioDialogRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun NotificationsTestingPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsTestingViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    NotificationsTestingContent(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        state = state.value,
        onScheduleNotification = viewModel::trigger,
        onResetDatabase = viewModel::clearNotificationsTable,
        onCancelNotifications = viewModel::cancelAllNotifications,
        onScheduleCategory = viewModel::scheduleCategory,
        onBack = onBack,
    )
}

@Composable
private fun NotificationsTestingContent(
    state: NotificationsTestingViewModel.UiState,
    onScheduleNotification: (NotificationsTestingViewModel.NotificationTrigger) -> Unit,
    onResetDatabase: () -> Unit,
    onCancelNotifications: () -> Unit,
    onScheduleCategory: (NotificationsTestingViewModel.NotificationCategorySchedule) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ThemedTopAppBar(
            title = "Notification Testing",
            bottomShadow = true,
            onNavigationClick = onBack,
        )

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            UniqueNotificationSchedulerContent(
                notificationTypes = state.uniqueNotifications,
                onButtonClick = onScheduleNotification,
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            SettingNotifications(
                onResetDatabase = onResetDatabase,
                onCancelNotifications = onCancelNotifications,
                categories = state.notificationCategories,
                scheduleCategory = onScheduleCategory,
            )
        }
    }
}

@Composable
private fun SettingNotifications(
    categories: List<NotificationsTestingViewModel.NotificationCategoryType>,
    onResetDatabase: () -> Unit,
    onCancelNotifications: () -> Unit,
    scheduleCategory: (NotificationsTestingViewModel.NotificationCategorySchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TextP60(
            modifier = Modifier
                .padding(horizontal = 24.dp),
            text = "Alternatively, you can utilize the real NotificationScheduler to schedule notifications that fall into specific settings categories.\nIt is recommended to clear the table first, then cancel all the scheduled work. Use the buttons below to achieve that.",
        )
        Button(
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.theme.colors.primaryInteractive03),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            onClick = onResetDatabase,
        ) {
            TextP40(
                text = "Reset notifications table",
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
        Button(
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.theme.colors.primaryInteractive03),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            onClick = onCancelNotifications,
        ) {
            TextP40(
                text = "Cancel ALL scheduled notifications",
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }

        var selectedCategory by remember { mutableStateOf<NotificationsTestingViewModel.NotificationCategoryType?>(null) }
        var selectedDelay by remember { mutableIntStateOf(1) }

        val notificationSchedule = if (selectedDelay > 0 && selectedCategory != null) {
            NotificationsTestingViewModel.NotificationCategorySchedule(
                category = selectedCategory!!,
                consecutiveDelay = selectedDelay.seconds,
            )
        } else {
            null
        }

        SettingRadioDialogRow(
            primaryText = "Select category",
            secondaryText = selectedCategory?.toString() ?: "Nothing is selected",
            options = categories,
            savedOption = selectedCategory,
            optionToLocalisedString = { it.toString() },
            onSave = { selectedCategory = it },
            indent = false,
        )
        DelayPicker(
            primaryText = "Select consecutive delay (seconds)",
            saved = selectedDelay,
            onSave = { selectedDelay = it },
            modifier = Modifier.fillMaxWidth(),
        )
        TextP60(
            modifier = Modifier.padding(
                horizontal = 24.dp,
            ),
            text = "Enter a positive number! When we schedule a category, its notifications will be scheduled sequentially, delayed by the specified amount",
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            enabled = notificationSchedule != null,
            onClick = {
                scheduleCategory(notificationSchedule!!)
            },
        ) {
            TextP40(text = "Schedule category notifications")
        }
    }
}

@Composable
private fun UniqueNotificationSchedulerContent(
    notificationTypes: List<NotificationsTestingViewModel.NotificationType>,
    onButtonClick: (NotificationsTestingViewModel.NotificationTrigger) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf<NotificationsTestingViewModel.NotificationType?>(null) }
    var selectedDelay by remember { mutableIntStateOf(0) }

    val notificationTrigger = selectedType?.let {
        NotificationsTestingViewModel.NotificationTrigger(
            notificationType = it,
            triggerType = if (selectedDelay > 0) {
                NotificationsTestingViewModel.NotificationTriggerType.Delayed(
                    selectedDelay.toLong().seconds,
                )
            } else {
                NotificationsTestingViewModel.NotificationTriggerType.Now
            },
        )
    }

    Column(
        modifier = modifier,
    ) {
        SettingRadioDialogRow(
            primaryText = "Select notification type",
            secondaryText = selectedType?.toString() ?: "Nothing is selected",
            options = notificationTypes,
            savedOption = selectedType,
            optionToLocalisedString = { it.toString() },
            onSave = { selectedType = it },
            indent = false,
        )
        DelayPicker(
            primaryText = "Select delay (seconds)",
            saved = selectedDelay,
            onSave = { selectedDelay = it },
        )
        TextP60(
            modifier = Modifier.padding(
                horizontal = 24.dp,
            ),
            text = "A delay of 0 seconds means that the notification will be displayed instantaneously\nPreviously scheduled, but not yet displayed notifications will be cancelled when scheduling a new one.",
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            enabled = notificationTrigger != null,
            onClick = {
                onButtonClick(notificationTrigger!!)
            },
        ) {
            TextP40(text = if (notificationTrigger?.triggerType is NotificationsTestingViewModel.NotificationTriggerType.Delayed) "Schedule" else "Fire now")
        }
    }
}

@Composable
private fun DelayPicker(
    primaryText: String,
    saved: Int,
    onSave: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingRow(
        primaryText = primaryText,
        secondaryText = stringResource(R.string.seconds_plural, saved),
        modifier = modifier.clickable { showDialog = true },
        indent = false,
    ) {
        if (showDialog) {
            FormFieldDialog(
                title = primaryText,
                placeholder = stringResource(R.string.seconds_label),
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

@Preview
@Composable
private fun PreviewNotificationTestingScreen(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        NotificationsTestingContent(
            state = NotificationsTestingViewModel.UiState(),
            onScheduleNotification = {},
            onResetDatabase = {},
            onCancelNotifications = {},
            onScheduleCategory = {},
            onBack = {},
        )
    }
}
