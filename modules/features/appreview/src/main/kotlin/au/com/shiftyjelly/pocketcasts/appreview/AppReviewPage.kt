package au.com.shiftyjelly.pocketcasts.appreview

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AppReviewPage(
    onClickNotReally: () -> Unit,
    onClickYes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.theme.colors.primaryInteractive01),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(MaterialTheme.theme.colors.primaryUi05, CircleShape)
                    .size(56.dp, 4.dp),
            )
            TextH30(
                text = stringResource(LR.string.app_rating_title),
                modifier = Modifier.padding(top = 40.dp),
            )
            TextH50(
                text = stringResource(LR.string.app_rating_body),
                textAlign = TextAlign.Center,
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier.padding(top = 12.dp),
            )
            EmojiButtons(
                onClickNotReally = onClickNotReally,
                onClickYes = onClickYes,
                modifier = Modifier.padding(
                    top = 56.dp,
                    bottom = 64.dp,
                ),
            )
        }
    }
}

@Composable
private fun EmojiButtons(
    onClickNotReally: () -> Unit,
    onClickYes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        var isAnimationRunning by remember { mutableStateOf(false) }
        val animationProgress by animateFloatAsState(
            // Animating to the very start and end makes images not visible
            targetValue = if (isAnimationRunning) 0.99f else 0.01f,
            animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
        )
        LaunchedEffect(Unit) {
            delay(450)
            isAnimationRunning = true
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f),
        ) {
            EmojiButton(
                emojiId = R.raw.app_review_no_animation,
                text = stringResource(LR.string.app_rating_no_label),
                animationProgress = animationProgress,
                onClick = onClickNotReally,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f),
        ) {
            EmojiButton(
                emojiId = R.raw.app_review_yes_animation,
                text = stringResource(LR.string.app_rating_yes_label),
                animationProgress = animationProgress,
                onClick = onClickYes,
            )
        }
    }
}

@Composable
private fun EmojiButton(
    @RawRes emojiId: Int,
    text: String,
    animationProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .clip(ButtonShape)
            .clickable(
                onClick = onClick,
            )
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = text
                role = Role.Button
            },
    ) {
        val lottieComposition by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(emojiId),
        )
        LottieAnimation(
            composition = lottieComposition,
            progress = { animationProgress },
            modifier = Modifier.size(68.dp),
        )
        TextH30(
            text = text,
            disableAutoScale = true,
        )
    }
}

private val ButtonShape = RoundedCornerShape(8.dp)

@Preview
@Composable
private fun AppReviewPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AppReviewPage(
            onClickNotReally = {},
            onClickYes = {},
        )
    }
}
