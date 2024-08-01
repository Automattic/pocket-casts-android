package au.com.shiftyjelly.pocketcasts.kids

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun KidsSendFeedbackDialog(
    modifier: Modifier = Modifier,
    onSubmitFeedback: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var feedbackText by remember { mutableStateOf(TextFieldValue(text = "")) }
    val focusRequester = remember { FocusRequester() }

    val onFeedbackTapped = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSubmitFeedback()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .imePadding(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                painterResource(R.drawable.swipe_affordance),
                contentDescription = stringResource(LR.string.swipe_affordance_icon),
                modifier = modifier
                    .width(56.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
            )
            TextH30(
                text = stringResource(LR.string.send_feedback_title),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W600,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = {
                        feedbackText = it
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        placeholderColor = MaterialTheme.theme.colors.primaryText02,
                        backgroundColor = MaterialTheme.theme.colors.primaryUi01,
                        focusedBorderColor = MaterialTheme.theme.colors.primaryUi05,
                        unfocusedLabelColor = MaterialTheme.theme.colors.primaryUi05,
                        unfocusedBorderColor = MaterialTheme.theme.colors.primaryUi05,
                    ),
                    placeholder = {
                        TextP60(
                            text = stringResource(LR.string.feedback_form_placeholder),
                            fontWeight = FontWeight.W400,
                            color = MaterialTheme.theme.colors.primaryText02,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(200.dp)
                        .focusRequester(focusRequester),
                )

                RowButton(
                    text = stringResource(LR.string.send_feedback),
                    contentDescription = stringResource(LR.string.send_feedback),
                    onClick = { onFeedbackTapped() },
                    includePadding = false,
                    textColor = MaterialTheme.theme.colors.primaryInteractive02,
                    modifier = Modifier.padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewKidsSendFeedbackDialog(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        KidsSendFeedbackDialog(
            onSubmitFeedback = {},
        )
    }
}
