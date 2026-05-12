package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isConnected: Boolean,
    canSend: Boolean,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(theme.background)
            .navigationBarsPadding(),
    ) {
        Divider(color = theme.divider, thickness = 0.5.dp)
        if (!isConnected) {
            Text(
                text = stringResource(LR.string.error_check_your_internet_connection),
                color = theme.secondaryText,
                fontSize = 13.sp,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 8.dp,
                ),
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = isConnected,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    onSend()
                    keyboardController?.hide()
                }),
                textStyle = TextStyle(
                    color = theme.inputText,
                    fontSize = 16.sp,
                ),
                cursorBrush = SolidColor(theme.sendButton),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.inputBackground, InputShape)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = stringResource(LR.string.chat_input_hint),
                            color = if (text.isEmpty()) theme.inputHint else theme.inputBackground,
                            fontSize = 16.sp,
                        )
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    onSend()
                    keyboardController?.hide()
                },
                enabled = canSend,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) theme.sendButton else theme.divider,
                        CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(LR.string.chat_send),
                    tint = if (canSend) theme.sendButtonIcon else theme.inputHint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private val InputShape = RoundedCornerShape(24.dp)
