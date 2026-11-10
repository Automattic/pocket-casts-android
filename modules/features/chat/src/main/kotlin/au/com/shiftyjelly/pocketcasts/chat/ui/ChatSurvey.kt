package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.annotation.RawRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatSurvey(
    theme: ChatTheme,
    onClickNotReally: () -> Unit,
    onClickYes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProvideTextStyle(LocalTextStyle.current.copy(letterSpacing = 0.sp)) {
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
                    .background(theme.sheetHandle, RoundedCornerShape(2.dp)),
            )
            Text(
                text = stringResource(LR.string.chat_survey_title),
                color = theme.primaryText,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 48.dp, bottom = 46.dp),
            )
            var isAnimationRunning by remember { mutableStateOf(false) }
            val animationProgress by animateFloatAsState(
                targetValue = if (isAnimationRunning) 0.99f else 0.01f,
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
            )
            LaunchedEffect(Unit) {
                delay(450)
                isAnimationRunning = true
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(87.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SurveyAnswer(
                    emojiId = IR.raw.app_review_no_animation,
                    animationProgress = { animationProgress },
                    label = stringResource(LR.string.chat_survey_not_really),
                    onClick = onClickNotReally,
                    theme = theme,
                    modifier = Modifier.weight(1f, fill = false),
                )
                SurveyAnswer(
                    emojiId = IR.raw.app_review_yes_animation,
                    animationProgress = { animationProgress },
                    label = stringResource(LR.string.chat_survey_yes),
                    onClick = onClickYes,
                    theme = theme,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

@Composable
private fun SurveyAnswer(
    @RawRes emojiId: Int,
    animationProgress: () -> Float,
    label: String,
    onClick: () -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .widthIn(min = 88.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 8.dp),
    ) {
        val lottieComposition by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(emojiId),
        )
        LottieAnimation(
            composition = lottieComposition,
            progress = animationProgress,
            modifier = Modifier.size(68.dp),
        )
        Text(
            text = label,
            color = theme.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

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
