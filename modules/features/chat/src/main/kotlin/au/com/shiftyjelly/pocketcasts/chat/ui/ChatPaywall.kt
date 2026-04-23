package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.chat.ChatPaywallUiState
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.layout.verticalNavigationBars
import au.com.shiftyjelly.pocketcasts.compose.plusGoldDark
import au.com.shiftyjelly.pocketcasts.compose.plusGoldLight
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChatPaywallPage(
    uiState: ChatPaywallUiState,
    onClickClose: () -> Unit,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val theme = rememberChatTheme()
    val startPadding = contentPadding.calculateStartPadding(LocalLayoutDirection.current)
    val endPadding = contentPadding.calculateEndPadding(LocalLayoutDirection.current)
    val bottomPadding = contentPadding.calculateBottomPadding()
    val topPadding = contentPadding.calculateTopPadding()

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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding, start = startPadding, end = endPadding),
        ) {
            SubscriptionBadge(
                iconRes = R.drawable.ic_plus,
                shortNameRes = LR.string.pocket_casts_plus_short,
                contentDescriptionRes = LR.string.pocket_casts_plus_badge,
                fontSize = 16.sp,
                padding = 6.dp,
                textColor = Color.Black,
                iconColor = Color.Black,
                backgroundBrush = Brush.horizontalGradient(
                    0f to Color.plusGoldLight,
                    1f to Color.plusGoldDark,
                ),
            )
            Spacer(
                modifier = Modifier.height(24.dp),
            )
            TextH20(
                text = stringResource(LR.string.chat_paywall_title),
                color = theme.primaryText,
                textAlign = TextAlign.Center,
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            TextH50(
                text = stringResource(LR.string.transcript_generated_paywall_description),
                color = theme.secondaryText,
                textAlign = TextAlign.Center,
            )
        }

        val gradientBrush = remember(theme.background) {
            val colorsStops = arrayOf(
                0.0f to theme.background,
                0.05f to theme.background,
                0.25f to Color.Transparent,
                0.9f to Color.Transparent,
                1f to theme.background,
            )
            Brush.verticalGradient(colorStops = colorsStops)
        }
        SampleChatPreview(
            theme = theme,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(gradientBrush),
        )

        CompositionLocalProvider(
            LocalRippleConfiguration provides RippleConfiguration(color = Color.Black),
        ) {
            RowButton(
                text = if (uiState.isFreeTrialAvailable) {
                    stringResource(LR.string.profile_start_free_trial)
                } else {
                    stringResource(LR.string.onboarding_subscribe_to_plus)
                },
                textColor = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.plusGoldLight,
                ),
                includePadding = false,
                onClick = onClickSubscribe,
                modifier = Modifier
                    .background(theme.background)
                    .padding(bottom = bottomPadding, start = startPadding, end = endPadding)
                    .windowInsetsPadding(WindowInsets.verticalNavigationBars),
            )
        }
    }
}

@Composable
private fun SampleChatPreview(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.roundToPx()
    }

    val itemCount = 5
    val offsets = remember {
        List(itemCount) { Animatable(screenHeightPx.toFloat()) }
    }
    val alphas = remember {
        List(itemCount) { Animatable(0f) }
    }

    LaunchedEffect(Unit) {
        for (i in 0 until itemCount) {
            delay(400L)
            launch {
                offsets[i].animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                )
            }
            launch {
                alphas[i].animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 400),
                )
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        AnimatedBubble(offset = offsets[0], alpha = alphas[0]) {
            ChatBubble(
                text = stringResource(LR.string.chat_preview_ai_1),
                isUser = false,
                theme = theme,
            )
        }
        AnimatedBubble(offset = offsets[1], alpha = alphas[1]) {
            ChatBubble(
                text = stringResource(LR.string.chat_preview_user_1),
                isUser = true,
                theme = theme,
            )
        }
        AnimatedBubble(offset = offsets[2], alpha = alphas[2]) {
            ChatBubble(
                text = stringResource(LR.string.chat_preview_ai_2),
                isUser = false,
                theme = theme,
            )
        }
        AnimatedBubble(offset = offsets[3], alpha = alphas[3]) {
            ChatBubble(
                text = stringResource(LR.string.chat_preview_user_2),
                isUser = true,
                theme = theme,
            )
        }
        AnimatedBubble(offset = offsets[4], alpha = alphas[4]) {
            TypingIndicator(theme = theme)
        }
    }
}

@Composable
private fun AnimatedBubble(
    offset: Animatable<Float, AnimationVector1D>,
    alpha: Animatable<Float, AnimationVector1D>,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = offset.value
                this.alpha = alpha.value
            },
    ) {
        content()
    }
}

@Composable
private fun ChatBubble(
    text: String,
    isUser: Boolean,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (isUser) theme.userBubble else theme.aiBubble
    val textColor = if (isUser) theme.userBubbleText else theme.aiBubbleText
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp,
    )
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        horizontalAlignment = alignment,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TypingIndicator(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val cycleDuration = 1200
    val transition = rememberInfiniteTransition(label = "typing")

    fun dotKeyframes(delayMs: Int) = infiniteRepeatable<Float>(
        animation = keyframes {
            durationMillis = cycleDuration
            0.3f at 0
            0.3f at delayMs
            1f at delayMs + 300
            0.3f at delayMs + 600
            0.3f at cycleDuration
        },
    )

    val dot1Alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.3f,
        animationSpec = dotKeyframes(0),
        label = "dot1",
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.3f,
        animationSpec = dotKeyframes(200),
        label = "dot2",
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.3f,
        animationSpec = dotKeyframes(400),
        label = "dot3",
    )

    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(theme.aiBubble, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(dot1Alpha)
                .background(theme.aiBubbleText, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(dot2Alpha)
                .background(theme.aiBubbleText, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(dot3Alpha)
                .background(theme.aiBubbleText, CircleShape),
        )
    }
}

@Preview
@Composable
private fun ChatPaywallPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        ChatPaywallPage(
            uiState = ChatPaywallUiState(),
            onClickClose = {},
            onClickSubscribe = {},
        )
    }
}
