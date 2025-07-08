package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerProgressingIndicator(
    state: PagerState,
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = activeColor.copy(alpha = 0.3f),
    isProgressedActive: Boolean = true,
) {
    if (state.pageCount <= 0) {
        return
    }
    val normalizedProgress = progress.coerceIn(0f, 1f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        repeat(state.pageCount) { index ->
            val brush = if (index == state.currentPage) {
                Brush.horizontalGradient(
                    0.0f to activeColor,
                    normalizedProgress to activeColor,
                    normalizedProgress to inactiveColor,
                    1f to inactiveColor,
                )
            } else if (index > state.currentPage) {
                SolidColor(inactiveColor)
            } else {
                SolidColor(if (isProgressedActive) activeColor else inactiveColor)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(brush, CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun PagerProgressingIndicatorPreview() = Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.background(Color.Black).padding(16.dp),
) {
    PagerProgressingIndicator(
        state = rememberPagerState(pageCount = { 0 }),
        progress = 0f,
    )
    PagerProgressingIndicator(
        state = rememberPagerState(pageCount = { 8 }),
        progress = 0f,
    )
    PagerProgressingIndicator(
        state = rememberPagerState(pageCount = { 6 }),
        progress = 0f,
    )
    PagerProgressingIndicator(
        state = rememberPagerState(initialPage = 4, pageCount = { 8 }),
        progress = 0f,
    )
    PagerProgressingIndicator(
        state = rememberPagerState(pageCount = { 8 }),
        progress = 0.5f,
    )
    PagerProgressingIndicator(
        state = rememberPagerState(initialPage = 1, pageCount = { 8 }),
        progress = 1f,
    )
    PagerProgressingIndicator(
        state = rememberPagerState(initialPage = 2, pageCount = { 4 }),
        progress = 0.75f,
    )
    PagerProgressingIndicator(
        state = rememberPagerState(initialPage = 2, pageCount = { 4 }),
        progress = 0.75f,
        isProgressedActive = false,
    )
}
