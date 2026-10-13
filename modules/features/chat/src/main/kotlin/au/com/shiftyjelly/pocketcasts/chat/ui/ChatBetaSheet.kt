package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatBetaSheet(
    theme: ChatTheme,
    buttonText: String,
    onClickButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(theme.background)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
    ) {
        Pill(backgroundColor = theme.sheetHandle)
        Spacer(modifier = Modifier.height(36.dp))
        Icon(
            painter = painterResource(IR.drawable.ic_episode_chat),
            contentDescription = null,
            tint = theme.tertiaryText,
            modifier = Modifier.size(30.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(LR.string.chat_beta_sheet_title),
            color = theme.primaryText,
            fontSize = 22.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(LR.string.chat_beta_sheet_body),
            color = theme.tertiaryText,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, start = 25.dp, end = 25.dp),
        )
        Spacer(modifier = Modifier.height(48.dp))
        RowButton(
            text = buttonText,
            textColor = theme.onWhiteText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            colors = ButtonDefaults.buttonColors(backgroundColor = theme.primaryText),
            includePadding = false,
            onClick = onClickButton,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        )
    }
}

@Preview
@Composable
private fun ChatBetaSheetPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        ChatBetaSheet(
            theme = rememberChatTheme(),
            buttonText = stringResource(LR.string.got_it),
            onClickButton = {},
        )
    }
}
