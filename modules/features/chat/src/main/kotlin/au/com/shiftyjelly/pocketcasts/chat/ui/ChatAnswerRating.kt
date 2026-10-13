package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.chat.ChatAnswerRating
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ChatAnswerRatingRow(
    rating: ChatAnswerRating?,
    onRate: (ChatAnswerRating) -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            ChatAnswerRating.entries.forEach { option ->
                RatingButton(
                    rating = option,
                    isSelected = option == rating,
                    onClick = { onRate(option) },
                    theme = theme,
                )
            }
        }
    }
}

@Composable
private fun RatingButton(
    rating: ChatAnswerRating,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    IconToggleButton(
        checked = isSelected,
        onCheckedChange = { onClick() },
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(if (isSelected) IR.drawable.ic_chat_thumb_down_filled else IR.drawable.ic_chat_thumb_down),
            contentDescription = stringResource(rating.labelRes),
            tint = if (isSelected) theme.primaryText else theme.rating,
            modifier = Modifier
                .size(IconSize)
                .graphicsLayer {
                    when (rating) {
                        ChatAnswerRating.Positive -> scaleY = -1f
                        ChatAnswerRating.Negative -> scaleX = -1f
                    }
                },
        )
    }
}

private val IconSize = 20.dp

private val ChatAnswerRating.labelRes
    get() = when (this) {
        ChatAnswerRating.Positive -> LR.string.chat_rate_answer_good
        ChatAnswerRating.Negative -> LR.string.chat_rate_answer_bad
    }

@Preview
@Composable
private fun ChatAnswerRatingRowPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppThemeWithBackground(ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            ChatAnswerRatingRow(
                rating = ChatAnswerRating.Negative,
                onRate = {},
                theme = rememberChatTheme(),
            )
        }
    }
}
