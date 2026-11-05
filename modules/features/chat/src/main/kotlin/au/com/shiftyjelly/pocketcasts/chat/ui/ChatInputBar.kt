package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isAnswering: Boolean,
    onStop: () -> Unit,
    isConnected: Boolean,
    canSend: Boolean,
    showBetaNote: Boolean,
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
        if (!isConnected) {
            ChatOfflineNote(
                theme = theme,
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isConnected) 1f else DISABLED_ALPHA)
                .padding(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
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
                    if (canSend && !isAnswering) {
                        onSend()
                        keyboardController?.hide()
                    }
                }),
                textStyle = TextStyle(
                    color = theme.inputText,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                ),
                cursorBrush = SolidColor(theme.sendButton),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .background(theme.inputBackground, CircleShape)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(LR.string.chat_input_question_hint),
                                color = theme.inputHint,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            SendOrStopButton(
                isAnswering = isAnswering,
                canSend = canSend,
                onSend = {
                    onSend()
                    keyboardController?.hide()
                },
                onStop = onStop,
                theme = theme,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (showBetaNote) {
            Text(
                text = stringResource(LR.string.chat_beta_note),
                color = theme.inputHint,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun SendOrStopButton(
    isAnswering: Boolean,
    canSend: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val isEnabled = isAnswering || canSend
    val stopLabel = stringResource(LR.string.stop)
    IconButton(
        onClick = if (isAnswering) onStop else onSend,
        enabled = isEnabled,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isEnabled) theme.sendButton else theme.inputBackground, CircleShape),
    ) {
        Crossfade(
            targetState = isAnswering,
            animationSpec = if (rememberIsReduceMotionEnabled()) snap() else tween(SEND_STOP_CROSSFADE_MS),
            label = "sendOrStop",
        ) { showStop ->
            if (showStop) {
                Box(
                    modifier = Modifier
                        .size(StopIconSize)
                        .background(theme.sendButtonIcon, RoundedCornerShape(3.dp))
                        .semantics { contentDescription = stopLabel },
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(LR.string.chat_send),
                    tint = if (canSend) theme.sendButtonIcon else theme.inputHint,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatOfflineNote(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_wifi_off),
            contentDescription = null,
            tint = theme.offlineNote,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(LR.string.chat_offline_note),
            color = theme.offlineNote,
            fontSize = 13.sp,
        )
    }
}

internal const val DISABLED_ALPHA = 0.4f
private val StopIconSize = 14.dp
private const val SEND_STOP_CROSSFADE_MS = 150

@Preview
@Composable
private fun ChatInputBarPreview(
    @PreviewParameter(AnsweringParameterProvider::class) isAnswering: Boolean,
) {
    AppTheme(Theme.ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides PodcastColors.ForUserEpisode) {
            ChatInputBar(
                text = "",
                onTextChange = {},
                onSend = {},
                isAnswering = isAnswering,
                onStop = {},
                isConnected = true,
                canSend = false,
                showBetaNote = true,
                theme = rememberChatTheme(),
            )
        }
    }
}

private class AnsweringParameterProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}
