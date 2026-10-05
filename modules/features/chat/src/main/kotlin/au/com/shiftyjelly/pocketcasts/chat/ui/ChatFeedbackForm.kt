package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatFeedback
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatFeedbackForm(
    reason: ChatFeedback.Reason?,
    details: String,
    showDetails: Boolean,
    canSubmit: Boolean,
    onSelectReason: (ChatFeedback.Reason) -> Unit,
    onDetailsChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClickLearnMore: () -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(theme.background)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(4.dp)
                .background(theme.secondaryText, RoundedCornerShape(2.dp)),
        )
        Text(
            text = stringResource(LR.string.chat_feedback_title).uppercase(),
            color = theme.secondaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
        )
        Column(modifier = Modifier.selectableGroup()) {
            ChatFeedback.Reason.entries.forEach { option ->
                ReasonRow(
                    label = option.labelRes,
                    isSelected = option == reason,
                    onClick = { onSelectReason(option) },
                    theme = theme,
                )
            }
        }
        if (showDetails) {
            DetailsField(
                details = details,
                onDetailsChange = onDetailsChange,
                theme = theme,
            )
        }
        Button(
            onClick = onSubmit,
            enabled = canSubmit,
            shape = RoundedCornerShape(12.dp),
            elevation = null,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = theme.primaryText,
                contentColor = theme.background,
                disabledBackgroundColor = theme.primaryText.copy(alpha = 0.4f),
                disabledContentColor = theme.background,
            ),
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                text = stringResource(LR.string.chat_feedback_submit),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LegalNote(
            onClickLearnMore = onClickLearnMore,
            theme = theme,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ReasonRow(
    @StringRes label: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: ChatTheme,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = stringResource(label),
            color = theme.primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = theme.quoteTimestamp,
                unselectedColor = theme.primaryText,
            ),
        )
    }
}

@Composable
private fun DetailsField(
    details: String,
    onDetailsChange: (String) -> Unit,
    theme: ChatTheme,
) {
    val hint = stringResource(LR.string.chat_feedback_details_hint)
    BasicTextField(
        value = details,
        onValueChange = onDetailsChange,
        textStyle = TextStyle(color = theme.inputText, fontSize = 15.sp, lineHeight = 20.sp),
        cursorBrush = SolidColor(theme.primaryText),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .background(theme.inputBackground, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (details.isEmpty()) {
                    Text(
                        text = hint,
                        color = theme.inputHint,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                    )
                }
                innerTextField()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .semantics { contentDescription = hint },
    )
}

@Composable
private fun LegalNote(
    onClickLearnMore: () -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val note = stringResource(LR.string.chat_feedback_legal)
    val learnMore = stringResource(LR.string.chat_feedback_learn_more)
    val text = buildAnnotatedString {
        append(note)
        append(" ")
        withLink(
            LinkAnnotation.Clickable(
                tag = LEARN_MORE_TAG,
                styles = TextLinkStyles(SpanStyle(color = theme.quoteTimestamp)),
                linkInteractionListener = { onClickLearnMore() },
            ),
        ) {
            append(learnMore)
        }
    }
    Text(
        text = text,
        color = theme.secondaryText,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

private val ChatFeedback.Reason.labelRes
    get() = when (this) {
        ChatFeedback.Reason.NotInteresting -> LR.string.chat_feedback_reason_not_interesting
        ChatFeedback.Reason.WrongFacts -> LR.string.chat_feedback_reason_wrong_facts
        ChatFeedback.Reason.OutOfDate -> LR.string.chat_feedback_reason_out_of_date
        ChatFeedback.Reason.Offensive -> LR.string.chat_feedback_reason_offensive
        ChatFeedback.Reason.Other -> LR.string.chat_feedback_reason_other
    }

private const val LEARN_MORE_TAG = "learn_more"

@Preview
@Composable
private fun ChatFeedbackFormPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            ChatFeedbackForm(
                reason = ChatFeedback.Reason.Other,
                details = "",
                showDetails = true,
                canSubmit = true,
                onSelectReason = {},
                onDetailsChange = {},
                onSubmit = {},
                onClickLearnMore = {},
                theme = rememberChatTheme(),
            )
        }
    }
}
