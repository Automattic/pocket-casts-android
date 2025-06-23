package au.com.shiftyjelly.pocketcasts.settings.notifications_testing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRadioDialogRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType

@Composable
internal fun NotificationsTestingPage(
    modifier: Modifier = Modifier,
    viewModel: NotificationsTestingViewModel = hiltViewModel(),
) {

    val state = viewModel.state.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf<NotificationsTestingViewModel.NotificationType?>(null) }
    var selectedDelay by remember { mutableIntStateOf(0) }

    val notificationTrigger = selectedType?.let {
        NotificationsTestingViewModel.NotificationTrigger(
            notificationType = it,
            triggerType = if (selectedDelay > 0) NotificationsTestingViewModel.NotificationTriggerType.Delayed(selectedDelay) else NotificationsTestingViewModel.NotificationTriggerType.Now
        )
    }

    NotificationsTestingContent(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        notificationTypes = state.value,
        selectedType = selectedType,
        onTypeChanged = { selectedType = it },
        selectedDelay = selectedDelay,
        onSelectedDelayChanged = { selectedDelay = it },
        isButtonEnabled = notificationTrigger != null,
        onButtonClick = { viewModel.trigger(notificationTrigger!!) },
        buttonLabel = if (notificationTrigger?.triggerType is NotificationsTestingViewModel.NotificationTriggerType.Now) "Fire now" else "Schedule"
    )

}

@Composable
private fun NotificationsTestingContent(
    notificationTypes: List<NotificationsTestingViewModel.NotificationType>,
    selectedType: NotificationsTestingViewModel.NotificationType?,
    onTypeChanged: (NotificationsTestingViewModel.NotificationType?) -> Unit,
    selectedDelay: Int,
    onSelectedDelayChanged: (Int) -> Unit,
    isButtonEnabled: Boolean,
    onButtonClick: () -> Unit,
    buttonLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        SettingRadioDialogRow(
            primaryText = "Select notification type",
            secondaryText = selectedType?.toString(),
            options = notificationTypes,
            savedOption = selectedType,
            optionToLocalisedString = { it.toString() },
            onSave = onTypeChanged,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextP40(
                text = "Select delay seconds"
            )
            Spacer(modifier = Modifier.width(8.dp))
            FormField(
                value = selectedDelay.toString(),
                placeholder = "Enter seconds here",
                onValueChange = { onSelectedDelayChanged(it.toIntOrNull() ?: 0) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        TextP60(
            modifier = Modifier.padding(
                horizontal = 16.dp
            ),
            text = "A delay of 0 seconds means that the notification will be displayed instantaneously\nPreviously scheduled, but not yet displayed notifications will be cancelled when scheduling a new one."
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = isButtonEnabled,
            onClick = onButtonClick
        ) {
            TextP40(text = buttonLabel)
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
            notificationTypes = listOf(NotificationsTestingViewModel.NotificationType.TRENDING, NotificationsTestingViewModel.NotificationType.OFFERS),
            selectedType = null,
            onTypeChanged = {},
            selectedDelay = 0,
            onSelectedDelayChanged = {},
            isButtonEnabled = false,
            onButtonClick = {},
            buttonLabel = "Label"
        )
    }
}