package au.com.shiftyjelly.pocketcasts.profile.blogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.math.floor
import kotlin.math.sin
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun EmptyBlogsPage(
    onBackPress: () -> Unit,
    onAddBlogClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi02),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_navigation_blogs),
            onNavigationClick = onBackPress,
        )
        BlogsEmptyContent(
            onAddBlogClick = onAddBlogClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BlogsEmptyContent(
    onAddBlogClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .widthIn(max = 320.dp)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BlogsIllustration()

        Spacer(Modifier.height(24.dp))

        TextH30(
            text = stringResource(LR.string.blogs_empty_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
        )

        Spacer(Modifier.height(8.dp))

        TextP40(
            text = stringResource(LR.string.blogs_empty_description),
            color = colors.primaryText02,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(22.dp))

        RowButton(
            text = stringResource(LR.string.blogs_add_button),
            onClick = { onAddBlogClick() },
            includePadding = false,
            textColor = colors.primaryInteractive02,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colors.primaryInteractive01,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val BlogArtworkBackground = Color(0xFFF2B53A)
private val PodcastArtworkBackground = Color(0xFF0F0F0F)

@Composable
private fun BlogsIllustration(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(160.dp)
            .height(100.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(64.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .rotate(-6f)
                .clip(RoundedCornerShape(14.dp))
                .background(BlogArtworkBackground),
            contentAlignment = Alignment.Center,
        ) {
            RssGlyph(color = PodcastArtworkBackground, modifier = Modifier.size(30.dp))
        }

        Arrow(
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 30.dp, height = 14.dp),
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(64.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .rotate(6f)
                .clip(RoundedCornerShape(14.dp))
                .background(PodcastArtworkBackground),
            contentAlignment = Alignment.Center,
        ) {
            Waveform(
                color = MaterialTheme.theme.colors.primaryInteractive01,
                bars = 9,
                modifier = Modifier.size(width = 28.dp, height = 28.dp),
            )
        }
    }
}

@Composable
private fun RssGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension / 12.5f, cap = StrokeCap.Round)
        val origin = Offset(x = size.width * 0.23f, y = size.height * 0.73f)
        drawCircle(color = color, radius = size.minDimension / 12f, center = origin)
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(x = origin.x - size.width * 0.27f, y = origin.y - size.height * 0.27f),
            size = Size(size.width * 0.54f, size.height * 0.54f),
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(x = origin.x - size.width * 0.5f, y = origin.y - size.height * 0.5f),
            size = Size(size.width * 1.0f, size.height * 1.0f),
            style = stroke,
        )
    }
}

@Composable
private fun Arrow(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val midY = size.height / 2f
        val endX = size.width * 0.9f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        drawLine(
            color = color,
            start = Offset(x = 0f, y = midY),
            end = Offset(x = endX, y = midY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        val head = size.width * 0.18f
        val path = Path().apply {
            moveTo(endX - head, midY - head)
            lineTo(size.width, midY)
            lineTo(endX - head, midY + head)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

@Composable
private fun Waveform(
    color: Color,
    bars: Int,
    modifier: Modifier = Modifier,
) {
    val heights = remember(bars) {
        List(bars) { i ->
            val seed = sin(i * 12.9898 + 78.233) * 43758.5453
            val frac = seed - floor(seed)
            (0.35f + frac.toFloat() * 0.65f).coerceIn(0.2f, 1f)
        }
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(1.dp))
                    .background(color),
            )
        }
    }
}

@Preview
@Composable
private fun BlogsEmptyPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        EmptyBlogsPage(
            onBackPress = {},
            onAddBlogClick = {},
        )
    }
}
