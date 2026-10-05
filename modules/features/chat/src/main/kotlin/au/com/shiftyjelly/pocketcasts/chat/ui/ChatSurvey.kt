package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatSurvey(
    theme: ChatTheme,
    onClickNotReally: () -> Unit,
    onClickYes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(theme.background)
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(4.dp)
                .background(theme.secondaryText, RoundedCornerShape(2.dp)),
        )
        Text(
            text = stringResource(LR.string.chat_survey_title),
            color = theme.primaryText,
            fontSize = 20.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 48.dp, bottom = 40.dp),
        )
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SurveyAnswer(
                emoji = NOT_REALLY_EMOJI,
                label = stringResource(LR.string.chat_survey_not_really),
                onClick = onClickNotReally,
                theme = theme,
            )
            SurveyAnswer(
                emoji = YES_EMOJI,
                label = stringResource(LR.string.chat_survey_yes),
                onClick = onClickYes,
                theme = theme,
            )
        }
    }
}

@Composable
private fun SurveyAnswer(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = emoji,
            fontSize = 56.sp,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
            text = label,
            color = theme.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private const val NOT_REALLY_EMOJI = "\uD83D\uDE14"
private const val YES_EMOJI = "\uD83E\uDD70"

@Preview
@Composable
private fun ChatSurveyPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            ChatSurvey(
                theme = rememberChatTheme(),
                onClickNotReally = {},
                onClickYes = {},
            )
        }
    }
}
