package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChaptersGenerating(
    onTurnOffGeneratedClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSlowNote: Boolean = false,
) {
    var isSlowNoteVisible by rememberSaveable { mutableStateOf(showSlowNote) }
    LaunchedEffect(Unit) {
        delay(SLOW_NOTE_DELAY)
        isSlowNoteVisible = true
    }
    Column(
        modifier = modifier
            .background(LocalChaptersTheme.current.background)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        GeneratingHeader(onTurnOffGeneratedClick = onTurnOffGeneratedClick)
        SkeletonChapters()
        AnimatedVisibility(
            visible = isSlowNoteVisible,
            enter = fadeIn(tween(durationMillis = 400)),
        ) {
            TextP40(
                text = stringResource(LR.string.chapters_generated_generating_note),
                color = LocalChaptersTheme.current.headerTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun GeneratingHeader(
    onTurnOffGeneratedClick: () -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "sparklePulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sparklePulseProgress",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 20.dp, end = 4.dp),
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_ai),
            contentDescription = null,
            tint = LocalChaptersTheme.current.headerToggle,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    val scale = lerp(0.85f, 1.1f, pulse)
                    scaleX = scale
                    scaleY = scale
                    rotationZ = lerp(-12f, 12f, pulse)
                    alpha = lerp(0.6f, 1f, pulse)
                },
        )
        Spacer(Modifier.width(8.dp))
        TextH50(
            text = stringResource(LR.string.chapters_generated_generating),
            color = LocalChaptersTheme.current.headerToggle,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
        )
        Spacer(Modifier.width(12.dp))
        GeneratedChaptersMenu(
            showSubscriptionIcon = false,
            onPreselectClick = null,
            onTurnOffClick = onTurnOffGeneratedClick,
        )
    }
}

@Composable
private fun SkeletonChapters() {
    val chaptersTheme = LocalChaptersTheme.current
    val barColor = chaptersTheme.headerToggle.copy(alpha = 0.12f).compositeOver(chaptersTheme.background)
    val highlight = chaptersTheme.headerToggle.copy(alpha = 0.22f)
    val sweep by rememberInfiniteTransition(label = "skeletonShimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = SHIMMER_SWEEP_MS, delayMillis = SHIMMER_GAP_MS, easing = FastOutSlowInEasing),
        ),
        label = "skeletonShimmerProgress",
    )
    Column(
        modifier = Modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val halfBand = size.width * 0.3f
                val center = lerp(-halfBand, size.width + halfBand, sweep)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        startX = center - halfBand,
                        endX = center + halfBand,
                    ),
                    blendMode = BlendMode.SrcAtop,
                )
            },
    ) {
        SkeletonRows.forEachIndexed { index, row ->
            SkeletonChapterRow(row = row, color = barColor)
            if (index < SkeletonRows.lastIndex) {
                Divider(
                    color = chaptersTheme.divider,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 40.dp, end = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun SkeletonChapterRow(
    row: SkeletonRow,
    color: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBar(widthFraction = row.titleFraction, color = color)
            if (row.subtitleFraction != null) {
                Spacer(Modifier.height(10.dp))
                SkeletonBar(widthFraction = row.subtitleFraction, color = color)
            }
        }
        Spacer(Modifier.width(24.dp))
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 10.dp)
                .background(color, RoundedCornerShape(5.dp)),
        )
    }
}

@Composable
private fun SkeletonBar(
    widthFraction: Float,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(10.dp)
            .background(color, RoundedCornerShape(5.dp)),
    )
}

private data class SkeletonRow(
    val titleFraction: Float,
    val subtitleFraction: Float? = null,
)

private val SkeletonRows = listOf(
    SkeletonRow(titleFraction = 0.52f, subtitleFraction = 0.25f),
    SkeletonRow(titleFraction = 0.62f, subtitleFraction = 0.18f),
    SkeletonRow(titleFraction = 0.55f),
    SkeletonRow(titleFraction = 0.47f),
    SkeletonRow(titleFraction = 0.59f, subtitleFraction = 0.35f),
    SkeletonRow(titleFraction = 0.64f, subtitleFraction = 0.30f),
)

private val SLOW_NOTE_DELAY = 5.seconds
private const val SHIMMER_SWEEP_MS = 1100
private const val SHIMMER_GAP_MS = 100

@Preview(name = "Light")
@Composable
private fun ChaptersGeneratingLightPreview() = ChaptersGeneratingPreview(Theme.ThemeType.LIGHT)

@Preview(name = "Dark")
@Composable
private fun ChaptersGeneratingDarkPreview() = ChaptersGeneratingPreview(Theme.ThemeType.DARK)

@Composable
private fun ChaptersGeneratingPreview(theme: Theme.ThemeType) {
    AppThemeWithBackground(theme) {
        ChaptersTheme {
            ChaptersGenerating(
                onTurnOffGeneratedClick = {},
                showSlowNote = true,
            )
        }
    }
}
