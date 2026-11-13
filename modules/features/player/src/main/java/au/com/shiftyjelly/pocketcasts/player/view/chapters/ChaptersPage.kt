package au.com.shiftyjelly.pocketcasts.player.view.chapters

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.models.to.Chapter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChaptersPage(
    lazyListState: LazyListState,
    chapters: List<ChaptersViewModel.ChapterState>,
    showHeader: Boolean,
    hasGeneratedChapters: Boolean,
    totalChaptersCount: Int,
    onSelectionChange: (Boolean, Chapter) -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onUrlClick: (Chapter) -> Unit,
    onSkipChaptersClick: (Boolean) -> Unit,
    onTurnOffGeneratedClick: () -> Unit,
    emptyState: ChaptersViewModel.EmptyState?,
    episodeUuid: String?,
    onTurnOnGeneratedClick: () -> Unit,
    onGenerateClick: () -> Unit,
    isTogglingChapters: Boolean,
    showSubscriptionIcon: Boolean,
    modifier: Modifier = Modifier,
    resolvingChapterIndex: Int? = null,
) {
    val density = LocalDensity.current
    val reveal = rememberGeneratedChaptersReveal(emptyState, hasGeneratedChapters)
    AnimatedContent(
        targetState = emptyState,
        transitionSpec = { chaptersContentTransform(density, hasGeneratedChapters) },
        label = "chaptersContent",
        modifier = modifier.background(LocalChaptersTheme.current.background),
    ) { state ->
        when (state) {
            ChaptersViewModel.EmptyState.GeneratedChaptersOff -> ChaptersOffState(
                onTurnOnClick = onTurnOnGeneratedClick,
            )

            ChaptersViewModel.EmptyState.NoChaptersYet -> ChaptersNoChaptersYet(
                onGenerateClick = onGenerateClick,
            )

            ChaptersViewModel.EmptyState.GeneratingChapters -> ChaptersGenerating(
                episodeUuid = episodeUuid,
                onTurnOffGeneratedClick = onTurnOffGeneratedClick,
                skeletonModifier = Modifier.animateEnterExit(enter = stateRise(density), exit = ExitTransition.None),
            )

            ChaptersViewModel.EmptyState.NoGeneratedChapters -> ChaptersNoGeneratedChapters()

            null -> ChaptersList(
                lazyListState = lazyListState,
                chapters = chapters,
                showHeader = showHeader,
                hasGeneratedChapters = hasGeneratedChapters,
                totalChaptersCount = totalChaptersCount,
                onSelectionChange = onSelectionChange,
                onChapterClick = onChapterClick,
                onUrlClick = onUrlClick,
                onSkipChaptersClick = onSkipChaptersClick,
                onTurnOffGeneratedClick = onTurnOffGeneratedClick,
                isTogglingChapters = isTogglingChapters,
                showSubscriptionIcon = showSubscriptionIcon,
                resolvingChapterIndex = resolvingChapterIndex,
                reveal = reveal,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChaptersList(
    lazyListState: LazyListState,
    chapters: List<ChaptersViewModel.ChapterState>,
    showHeader: Boolean,
    hasGeneratedChapters: Boolean,
    totalChaptersCount: Int,
    onSelectionChange: (Boolean, Chapter) -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onUrlClick: (Chapter) -> Unit,
    onSkipChaptersClick: (Boolean) -> Unit,
    onTurnOffGeneratedClick: () -> Unit,
    isTogglingChapters: Boolean,
    showSubscriptionIcon: Boolean,
    resolvingChapterIndex: Int?,
    reveal: Animatable<Float, AnimationVector1D>?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = if (Build.VERSION.SDK_INT > 29) {
            WindowInsets.navigationBars.asPaddingValues()
        } else {
            PaddingValues(bottom = 56.dp)
        },
        modifier = modifier
            .background(LocalChaptersTheme.current.background)
            .fillMaxSize()
            .padding(top = if (showHeader) 0.dp else 16.dp),
    ) {
        val selectedCount = chapters.count { it.chapter.selected }
        if (showHeader) {
            item {
                ChaptersHeader(
                    totalChaptersCount = totalChaptersCount,
                    hiddenChaptersCount = totalChaptersCount - selectedCount,
                    onSkipChaptersClick = onSkipChaptersClick,
                    isTogglingChapters = isTogglingChapters,
                    showSubscriptionIcon = showSubscriptionIcon,
                    isGenerated = hasGeneratedChapters,
                    onTurnOffGeneratedClick = onTurnOffGeneratedClick,
                    modifier = Modifier.revealed(reveal, delayMillis = REVEAL_START_MS, durationMillis = REVEAL_HEADER_MS),
                )
            }
        }
        itemsIndexed(chapters, key = { _, state -> state.chapter.index }) { index, state ->
            val rowReveal = Modifier.revealed(
                reveal = reveal,
                delayMillis = REVEAL_START_MS + index.coerceAtMost(REVEAL_STAGGERED_ROWS - 1) * REVEAL_STAGGER_MS,
                durationMillis = REVEAL_ROW_MS,
                shift = STATE_SHIFT,
            )
            ChapterRow(
                state = state,
                isTogglingChapters = isTogglingChapters,
                selectedCount = selectedCount,
                onSelectionChange = onSelectionChange,
                onClick = { onChapterClick(state.chapter) },
                onUrlClick = { onUrlClick(state.chapter) },
                isResolving = state.chapter.index == resolvingChapterIndex,
                modifier = Modifier
                    .animateItem()
                    .then(rowReveal),
            )
            if (index < chapters.lastIndex) {
                Divider(
                    color = LocalChaptersTheme.current.divider,
                    thickness = 1.dp,
                    modifier = rowReveal.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

private fun AnimatedContentTransitionScope<ChaptersViewModel.EmptyState?>.chaptersContentTransform(
    density: Density,
    hasGeneratedChapters: Boolean,
): ContentTransform {
    val shift = with(density) { STATE_SHIFT.roundToPx() }
    val isGeneratedChaptersReady = initialState == ChaptersViewModel.EmptyState.GeneratingChapters &&
        targetState == null &&
        hasGeneratedChapters
    return if (isGeneratedChaptersReady) {
        val exit = fadeOut(tween(durationMillis = GENERATING_EXIT_MS, easing = EaseIn))
        (EnterTransition.None togetherWith exit using null).apply { targetContentZIndex = -1f }
    } else {
        val fade = fadeIn(tween(durationMillis = STATE_ENTER_MS, delayMillis = STATE_ENTER_DELAY_MS, easing = EaseOut))
        val enter = if (targetState == ChaptersViewModel.EmptyState.GeneratingChapters) fade else fade + stateRise(density)
        val exit = fadeOut(tween(durationMillis = STATE_EXIT_MS, easing = EaseIn)) +
            slideOutVertically(tween(durationMillis = STATE_EXIT_MS, easing = EaseIn)) { -shift }
        enter togetherWith exit using null
    }
}

private fun stateRise(density: Density): EnterTransition {
    val shift = with(density) { STATE_SHIFT.roundToPx() }
    return slideInVertically(tween(durationMillis = STATE_ENTER_MS, delayMillis = STATE_ENTER_DELAY_MS, easing = EaseOut)) { shift }
}

@Composable
private fun rememberGeneratedChaptersReveal(
    emptyState: ChaptersViewModel.EmptyState?,
    hasGeneratedChapters: Boolean,
): Animatable<Float, AnimationVector1D>? {
    var previousEmptyState by remember { mutableStateOf(emptyState) }
    val reveal = remember(emptyState) {
        val isGeneratedChaptersReady = previousEmptyState == ChaptersViewModel.EmptyState.GeneratingChapters &&
            emptyState == null &&
            hasGeneratedChapters
        if (isGeneratedChaptersReady) Animatable(0f) else null
    }
    SideEffect { previousEmptyState = emptyState }
    LaunchedEffect(reveal) {
        reveal?.animateTo(REVEAL_END_MS.toFloat(), tween(durationMillis = REVEAL_END_MS, easing = LinearEasing))
    }
    return reveal
}

private fun Modifier.revealed(
    reveal: Animatable<Float, AnimationVector1D>?,
    delayMillis: Int,
    durationMillis: Int,
    shift: Dp = 0.dp,
): Modifier = if (reveal == null) {
    this
} else {
    graphicsLayer {
        val progress = EaseOut.transform(((reveal.value - delayMillis) / durationMillis).coerceIn(0f, 1f))
        alpha = progress
        translationY = (1f - progress) * shift.toPx()
    }
}

private val STATE_SHIFT = 8.dp
private const val STATE_ENTER_MS = 300
private const val STATE_ENTER_DELAY_MS = 150
private const val STATE_EXIT_MS = 250
private const val GENERATING_EXIT_MS = 170
private const val REVEAL_START_MS = 150
private const val REVEAL_HEADER_MS = 200
private const val REVEAL_ROW_MS = 350
private const val REVEAL_STAGGER_MS = 70
private const val REVEAL_STAGGERED_ROWS = 10
private const val REVEAL_END_MS = REVEAL_START_MS + (REVEAL_STAGGERED_ROWS - 1) * REVEAL_STAGGER_MS + REVEAL_ROW_MS
