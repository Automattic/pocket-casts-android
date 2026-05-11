package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextC50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.math.abs
import kotlin.math.sin
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val HeroAccent = Color(0xFF03A9F4)
private val HeroAccentDark = Color(0xFF0288C7)
private val BlogCardLabel = Color(0xFF7A7468)
private val BlogCardInk = Color(0xFF1A1814)
private val BlogCardHairline = Color(0xFFE6E2DA)
private val ThumbDarkStart = Color(0xFF1A1A1A)
private val ThumbDarkEnd = Color(0xFF2A2A2A)

private const val WAVEFORM_BAR_COUNT = 26
private const val WAVEFORM_PLAYED_BARS = 9
private val WAVEFORM_HEIGHT = 22.dp

@Composable
fun BlogsUpsellHeader(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) { role = Role.Image }
            .focusable(false)
            .height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        BlogPostPeekTile(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .graphicsLayer { rotationZ = -8f },
        )
        AudioThumbPeekTile(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .graphicsLayer { rotationZ = 10f },
        )
        HeroAudioTile(
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun HeroAudioTile(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(180.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(HeroAccent, HeroAccentDark),
                ),
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        TextH40(
            text = stringResource(LR.string.onboarding_blogs_upsell_sample_title),
            color = Color.White,
            maxLines = 2,
            disableAutoScale = true,
            fontWeight = FontWeight.W800,
            lineHeight = 18.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Waveform()
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "1:42",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.W500,
            )
            Text(
                text = "5:12",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.W500,
            )
        }
    }
}

@Composable
private fun Waveform() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(WAVEFORM_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(WAVEFORM_BAR_COUNT) { i ->
            val seed = abs(sin(i * 1.3 + 0.5)).toFloat()
            val fraction = (0.25f + seed * 0.75f).coerceIn(0.25f, 1f)
            val barHeight = (WAVEFORM_HEIGHT.value * fraction).dp
            WaveformBar(
                heightDp = barHeight,
                color = if (i < WAVEFORM_PLAYED_BARS) Color.White else Color.White.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun RowScope.WaveformBar(
    heightDp: androidx.compose.ui.unit.Dp,
    color: Color,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(heightDp)
            .clip(RoundedCornerShape(1.dp))
            .background(color),
    )
}

@Composable
private fun BlogPostPeekTile(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(110.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, BlogCardHairline, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        TextC50(
            text = "Blog · Apr 24",
            color = BlogCardLabel,
            disableAutoScale = true,
            fontWeight = FontWeight.W700,
        )
        Spacer(modifier = Modifier.height(6.dp))
        TextP40(
            text = stringResource(LR.string.onboarding_blogs_upsell_sample_title),
            color = BlogCardInk,
            disableAutoScale = true,
            fontWeight = FontWeight.W700,
            maxLines = 3,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index == 4) 0.5f else 1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BlogCardHairline),
                )
            }
        }
    }
}

@Composable
private fun AudioThumbPeekTile(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(90.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(ThumbDarkStart, ThumbDarkEnd),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val barHeights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.85f, 0.45f, 0.75f, 0.55f)
            barHeights.forEach { fraction ->
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height((28 * fraction).dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(HeroAccent),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewBlogsUpsellHeader(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.theme.colors.primaryUi01)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        BlogsUpsellHeader(modifier = Modifier.fillMaxWidth())
    }
}

@Preview
@Composable
private fun PreviewHeroTile(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.theme.colors.primaryUi01)
            .padding(24.dp),
    ) {
        HeroAudioTile()
    }
}
