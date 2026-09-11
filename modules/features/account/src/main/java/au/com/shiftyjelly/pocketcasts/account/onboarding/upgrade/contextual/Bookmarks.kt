package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val frontGradient = listOf(Color(0xFF0202FE), Color(0xFF27D9E9))
private val middleGradient = listOf(Color(0xFFEC4034), Color(0xFFFF9D00))
private val backGradient = listOf(Color(0xFFE8A92C), Color(0xFFE4D820))

private const val REVEAL_DURATION = 500
private const val CARD_CORNER = 12

@Composable
fun BookmarksAnimation(modifier: Modifier = Modifier) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .semantics(mergeDescendants = true) { role = Role.Image },
        contentAlignment = Alignment.TopCenter,
    ) {
        StackedCard(gradient = backGradient, inset = 34.dp, revealedOffset = (-22).dp, revealed = revealed, delayMillis = 100)
        StackedCard(gradient = middleGradient, inset = 17.dp, revealedOffset = (-11).dp, revealed = revealed, delayMillis = 300)

        val offset by animateDpAsState(
            targetValue = if (revealed) 0.dp else 24.dp,
            animationSpec = tween(REVEAL_DURATION, delayMillis = 500, easing = LinearOutSlowInEasing),
            label = "frontOffset",
        )
        val alpha by animateFloatAsState(
            targetValue = if (revealed) 1f else 0f,
            animationSpec = tween(REVEAL_DURATION, delayMillis = 500, easing = LinearOutSlowInEasing),
            label = "frontAlpha",
        )
        BookmarkUpgradeCard(
            modifier = Modifier
                .offset(y = offset)
                .alpha(alpha),
        )
    }
}

@Composable
private fun BoxScope.StackedCard(
    gradient: List<Color>,
    inset: Dp,
    revealedOffset: Dp,
    revealed: Boolean,
    delayMillis: Int,
) {
    val offset by animateDpAsState(
        targetValue = if (revealed) revealedOffset else 0.dp,
        animationSpec = tween(REVEAL_DURATION, delayMillis = delayMillis, easing = LinearOutSlowInEasing),
        label = "stackedOffset",
    )
    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(REVEAL_DURATION, delayMillis = delayMillis, easing = LinearOutSlowInEasing),
        label = "stackedAlpha",
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .padding(horizontal = inset)
            .offset(y = offset)
            .alpha(alpha)
            .clip(RoundedCornerShape(CARD_CORNER.dp))
            .background(Brush.linearGradient(gradient)),
    )
}

@Composable
private fun BookmarkUpgradeCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(CARD_CORNER.dp))
            .clip(RoundedCornerShape(CARD_CORNER.dp))
            .background(Brush.linearGradient(frontGradient))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(IR.drawable.artwork_10),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(LR.string.bookmarks_upgrade_example_title),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(LR.string.bookmarks_upgrade_example_passage),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TimestampPill()
    }
}

@Composable
private fun TimestampPill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "6:45",
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
        )
        Icon(
            painter = painterResource(IR.drawable.ic_play),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Preview
@Composable
private fun BookmarksAnimationPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Box(modifier = Modifier.padding(vertical = 40.dp)) {
        BookmarkUpgradeCard()
    }
}
