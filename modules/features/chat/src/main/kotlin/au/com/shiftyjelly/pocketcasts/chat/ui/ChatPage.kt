package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.chat.ChatUiState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChatPage(
    uiState: ChatUiState,
    onClickClose: () -> Unit,
    onClickSubscribe: () -> Unit,
    onShowChat: () -> Unit,
    onShowChatPaywall: () -> Unit,
    modifier: Modifier = Modifier,
    paywallPadding: PaddingValues = PaddingValues(16.dp),
) {
    val theme = rememberChatTheme()

    Column(
        modifier = modifier.background(theme.background),
    ) {
        IconButton(
            onClick = onClickClose,
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
                .offset(x = -4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(LR.string.close),
                tint = theme.iconButton,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // TODO: Chat content goes here

            if (uiState.isPaywallVisible) {
                ChatPaywall(
                    isFreeTrialAvailable = uiState.isFreeTrialAvailable,
                    onClickSubscribe = onClickSubscribe,
                    theme = theme,
                    contentPadding = paywallPadding,
                )
            }
        }
    }

    ShowChatEffect(
        uiState = uiState,
        onShowChat = onShowChat,
        onShowChatPaywall = onShowChatPaywall,
    )
}

@Composable
private fun ShowChatEffect(
    uiState: ChatUiState,
    onShowChat: () -> Unit,
    onShowChatPaywall: () -> Unit,
) {
    val isPaywallVisible = uiState.isPaywallVisible
    LaunchedEffect(isPaywallVisible) {
        if (isPaywallVisible) {
            onShowChatPaywall()
        } else {
            onShowChat()
        }
    }
}
