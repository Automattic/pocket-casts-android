package au.com.shiftyjelly.pocketcasts.account.deviceapprove

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowLoadingButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.components.UserAvatar
import au.com.shiftyjelly.pocketcasts.compose.components.UserAvatarConfig
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun DeviceApprovePage(
    state: DeviceApproveUiState,
    onConnect: () -> Unit,
    onSetUpAccount: () -> Unit,
    onSwitchAccount: () -> Unit,
    onDone: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        Pill()
        when (state.status) {
            DeviceApproveStatus.Approved -> ResultContent(
                title = stringResource(LR.string.device_approve_success_title),
                message = stringResource(LR.string.device_approve_success_message),
                buttonText = stringResource(LR.string.ok),
                onButtonClick = onDone,
            )

            DeviceApproveStatus.ExpiredError -> ResultContent(
                title = stringResource(LR.string.device_approve_expired_title),
                message = stringResource(LR.string.device_approve_expired_message),
                buttonText = stringResource(LR.string.ok),
                onButtonClick = onClose,
            )

            DeviceApproveStatus.GenericError -> ResultContent(
                title = stringResource(LR.string.device_approve_error_title),
                message = stringResource(LR.string.device_approve_error_message),
                buttonText = stringResource(LR.string.try_again),
                onButtonClick = onConnect,
            )

            DeviceApproveStatus.Idle, DeviceApproveStatus.Submitting -> ApproveContent(
                state = state,
                onConnect = onConnect,
                onSetUpAccount = onSetUpAccount,
                onSwitchAccount = onSwitchAccount,
            )
        }
    }
}

@Composable
private fun ColumnScope.ApproveContent(
    state: DeviceApproveUiState,
    onConnect: () -> Unit,
    onSetUpAccount: () -> Unit,
    onSwitchAccount: () -> Unit,
) {
    PairingBody(
        title = stringResource(LR.string.device_approve_title),
        message = stringResource(
            if (state.isLoggedIn) LR.string.device_approve_description else LR.string.device_approve_login_required,
        ),
    ) {
        if (state.isLoggedIn) {
            Spacer(Modifier.height(24.dp))
            AccountCard(email = state.email.orEmpty())
            Spacer(Modifier.height(16.dp))
            CodeChip(code = state.userCode)
            Spacer(Modifier.height(16.dp))
            SwitchAccountLink(
                onClick = onSwitchAccount,
                enabled = state.status == DeviceApproveStatus.Idle,
            )
        }
    }
    if (state.isLoggedIn) {
        RowLoadingButton(
            text = stringResource(LR.string.device_approve_connect),
            isLoading = state.status == DeviceApproveStatus.Submitting,
            onClick = onConnect,
        )
    } else {
        RowButton(
            text = stringResource(LR.string.profile_set_up_account),
            onClick = onSetUpAccount,
        )
    }
}

@Composable
private fun ColumnScope.PairingBody(
    title: String,
    message: String,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        PairingBadges()
        Spacer(Modifier.height(24.dp))
        TextH20(text = title, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        TextP40(
            text = message,
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
        )
        content()
    }
}

@Composable
private fun SwitchAccountLink(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 12.dp),
    ) {
        TextP40(
            text = stringResource(LR.string.device_approve_switch_account),
            color = MaterialTheme.theme.colors.primaryInteractive01.copy(alpha = if (enabled) 1f else 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PairingBadges(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        LogoBadge {
            Image(
                painter = painterResource(IR.drawable.ic_pocket_casts_logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        BadgeDots(Modifier.padding(horizontal = 16.dp))
        LogoBadge {
            Image(
                painter = painterResource(IR.drawable.ic_android_robot),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun LogoBadge(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF010609), Color(0xFF0C4A6F)))),
        content = { content() },
    )
}

@Composable
private fun BadgeDots(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier,
    ) {
        repeat(3) {
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.theme.colors.primaryUi05),
            )
        }
    }
}

@Composable
private fun AccountCard(email: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.theme.colors.primaryUi02)
            .border(1.dp, MaterialTheme.theme.colors.primaryUi05, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        UserAvatar(
            imageUrl = remember(email) { Gravatar.getUrl(email) },
            subscriptionTier = null,
            config = UserAvatarConfig(imageSize = 48.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            TextP50(
                text = stringResource(LR.string.device_approve_signing_in_as),
                color = MaterialTheme.theme.colors.primaryText02,
            )
            TextH40(text = email)
        }
    }
}

@Composable
private fun CodeChip(code: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.theme.colors.primaryUi02)
            .border(1.dp, MaterialTheme.theme.colors.primaryUi05, RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp),
    ) {
        TextH20(text = code, letterSpacing = 4.sp)
    }
}

@Composable
private fun ColumnScope.ResultContent(
    title: String,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit,
) {
    PairingBody(title = title, message = message)
    RowButton(text = buttonText, onClick = onButtonClick)
}

@Preview
@Composable
private fun DeviceApprovePageLoggedInPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        DeviceApprovePage(
            state = DeviceApproveUiState(userCode = "ABCD12", isLoggedIn = true, email = "user@example.com"),
            onConnect = {},
            onSetUpAccount = {},
            onSwitchAccount = {},
            onDone = {},
            onClose = {},
        )
    }
}

@Preview
@Composable
private fun DeviceApprovePageLoggedOutPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        DeviceApprovePage(
            state = DeviceApproveUiState(userCode = "ABCD12", isLoggedIn = false),
            onConnect = {},
            onSetUpAccount = {},
            onSwitchAccount = {},
            onDone = {},
            onClose = {},
        )
    }
}
