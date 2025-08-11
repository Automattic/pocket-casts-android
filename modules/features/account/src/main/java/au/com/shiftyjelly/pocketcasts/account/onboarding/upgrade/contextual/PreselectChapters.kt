package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PreselectChaptersAnimation(modifier: Modifier = Modifier) {
    val animationTriggers = remember {
        List(predefinedChapters.size) {
            mutableStateOf(AnimatedState.HIDDEN)
        }
    }

    LaunchedEffect("animations") {
        (predefinedChapters.size - 1 downTo 0).forEach {
            launch {
                delay(it * 150L)
                animationTriggers[it].value = AnimatedState.VISIBLE
            }
            launch {
                delay(1_500 + it * 100L)
                if (it != predefinedChapters.lastIndex) {
                    animationTriggers[it].value = AnimatedState.DISABLED
                }
            }
        }
    }

    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) { role = Role.Image }
            .focusable(false),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        predefinedChapters.forEachIndexed { index, item ->
            ChapterRow(
                modifier = Modifier.fillMaxWidth(),
                chapterData = item,
                animatedState = animationTriggers[index].value,
            )
            if (index != predefinedChapters.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

private data class ChapterData(
    val index: Int,
    val title: String,
)

private val predefinedTitles = listOf(
    "Intro",
    "A word from our sponsor",
    "Who will win the Oscars?",
)

private val predefinedChapters = (0 until 3).map {
    ChapterData(
        index = it + 1,
        title = predefinedTitles[it],
    )
}.toList()

private enum class AnimatedState {
    HIDDEN,
    VISIBLE,
    DISABLED,
}

@Composable
private fun ChapterRow(
    chapterData: ChapterData,
    modifier: Modifier = Modifier,
    animatedState: AnimatedState = AnimatedState.VISIBLE,
    floatInDistance: Dp = 12.dp,
) {
    val floatInDistancePx = LocalDensity.current.run {
        floatInDistance.roundToPx()
    }
    val transition = updateTransition(animatedState)
    val alphaAnim by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 600)
        },
    ) { state ->
        when (state) {
            AnimatedState.HIDDEN -> 0f
            AnimatedState.VISIBLE -> 1f
            AnimatedState.DISABLED -> .3f
        }
    }
    val offsetYAnim by transition.animateInt(
        transitionSpec = {
            tween(durationMillis = 700)
        },
    ) { state -> if (state == AnimatedState.HIDDEN) floatInDistancePx else 0 }

    Card(
        modifier = modifier
            .offset { IntOffset(x = 0, y = offsetYAnim) }
            .graphicsLayer {
                alpha = alphaAnim
                shadowElevation = alphaAnim * 4.dp.toPx()
                shape = RoundedCornerShape(4.dp)
                clip = false
            },
        shape = RoundedCornerShape(4.dp),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.theme.colors.primaryUi04,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_check_black_24dp),
                contentDescription = "",
                tint = MaterialTheme.theme.colors.primaryUi04,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.theme.colors.primaryIcon02, shape = CircleShape)
                    .padding(4.dp),
            )
            Column {
                TextP60(
                    fontSize = 10.sp,
                    text = stringResource(LR.string.onboarding_preselect_chapters_chapter_title, chapterData.index).uppercase(),
                    color = MaterialTheme.theme.colors.primaryText02,
                    disableAutoScale = true,
                )
                TextP60(
                    fontSize = 13.sp,
                    text = chapterData.title,
                    color = MaterialTheme.theme.colors.primaryText01,
                    disableAutoScale = true,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewChapterItem(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Column {
        ChapterRow(
            chapterData = predefinedChapters[0],
        )
    }
}
