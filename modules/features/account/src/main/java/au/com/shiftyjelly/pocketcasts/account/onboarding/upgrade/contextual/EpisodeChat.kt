package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val messages = listOf(
    LR.string.onboarding_episode_chat_question_1 to true,
    LR.string.onboarding_episode_chat_answer to false,
    LR.string.onboarding_episode_chat_question_2 to true,
)

private const val MESSAGE_DELAY = 400L
private const val MESSAGE_DURATION = 500
private const val MESSAGE_OFFSET = 48f

@Composable
fun EpisodeChatAnimation(
    modifier: Modifier = Modifier,
    initiallyRevealed: Boolean = false,
) {
    val progress = remember { messages.map { Animatable(if (initiallyRevealed) 1f else 0f) } }

    LaunchedEffect(Unit) {
        progress.forEach { animatable ->
            delay(MESSAGE_DELAY)
            launch {
                animatable.animateTo(1f, tween(durationMillis = MESSAGE_DURATION, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        messages.forEachIndexed { index, (textRes, isUser) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val value = progress[index].value
                        alpha = value
                        translationY = (1f - value) * MESSAGE_OFFSET * density
                    },
            ) {
                MessageBubble(textRes = textRes, isUser = isUser)
            }
        }
    }
}

@Composable
private fun MessageBubble(
    @StringRes textRes: Int,
    isUser: Boolean,
) {
    val colors = MaterialTheme.theme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(textRes),
            color = if (isUser) colors.primaryUi01 else colors.primaryText01,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier
                .widthIn(max = 250.dp)
                .then(if (isUser) Modifier else Modifier.shadow(4.dp, shape))
                .background(if (isUser) colors.primaryText01 else colors.primaryUi03, shape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Preview
@Composable
private fun EpisodeChatAnimationPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Box(modifier = Modifier.padding(24.dp)) {
        EpisodeChatAnimation(initiallyRevealed = true)
    }
}
