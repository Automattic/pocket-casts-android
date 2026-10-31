package au.com.shiftyjelly.pocketcasts.player.view

import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.TextPaint
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.player.R
import kotlin.math.ceil
import au.com.shiftyjelly.pocketcasts.images.R as IR

internal data class NotesHeader(
    val title: String,
    val date: String,
    val duration: String,
    val durationDescription: String,
)

internal data class NotesColors(
    val background: Color,
    val contrast01: Color,
    val contrast02: Color,
    val contrast05: Color,
    val progressIndicator: Color,
    val progressTrack: Color,
)

@Composable
internal fun NotesPage(
    header: NotesHeader?,
    colors: NotesColors,
    isLoading: Boolean,
    showNotesView: View?,
    errorText: String?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .platformVerticalScrollbar(scrollState)
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(scrollState),
    ) {
        TextViewText(
            text = header?.title.orEmpty(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = colors.contrast01,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            DrawableImage(
                drawableId = IR.drawable.ic_calendar,
                contentDescription = null,
                tint = colors.contrast02,
                modifier = Modifier.size(width = 17.dp, height = 18.dp),
            )
            Spacer(Modifier.width(8.dp))
            val date = header?.date.orEmpty()
            TextViewText(
                text = date,
                fontSize = 15.sp,
                letterSpacing = (-0.01).em,
                color = colors.contrast02,
                modifier = Modifier.width(rememberTextViewWidth(date, 15.sp, -0.01f)),
            )
            Spacer(Modifier.width(8.dp))
            DrawableImage(
                drawableId = R.drawable.ic_duration,
                contentDescription = null,
                tint = colors.contrast02,
            )
            Spacer(Modifier.width(8.dp))
            TextViewText(
                text = header?.duration.orEmpty(),
                fontSize = 15.sp,
                letterSpacing = (-0.01).em,
                color = colors.contrast02,
                modifier = Modifier.semantics { contentDescription = header?.durationDescription.orEmpty() },
            )
        }
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.contrast05),
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    color = colors.progressIndicator,
                    backgroundColor = colors.progressTrack,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (showNotesView != null) {
            key(showNotesView) {
                AndroidView(
                    factory = { showNotesView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .padding(horizontal = 16.dp),
                )
            }
        }
        if (errorText != null) {
            TextViewText(
                text = errorText,
                fontSize = 14.sp,
                letterSpacing = 0.02.em,
                lineSpacingExtra = 4.sp,
                color = Color(0xB3FFFFFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}

@Suppress("ktlint:compose:modifier-composed-check")
private fun Modifier.platformVerticalScrollbar(scrollState: ScrollState) = composed {
    val context = LocalContext.current
    val thumb = remember(context) {
        context.obtainStyledAttributes(intArrayOf(android.R.attr.scrollbarThumbVertical)).use { it.getDrawable(0) }
    }
    val thickness = remember(context) {
        context.obtainStyledAttributes(intArrayOf(android.R.attr.scrollbarSize)).use { it.getDimensionPixelSize(0, 0) }
    }
    val alpha by animateFloatAsState(
        targetValue = if (scrollState.isScrollInProgress) 1f else 0f,
        animationSpec = if (scrollState.isScrollInProgress) tween(0) else tween(durationMillis = 250, delayMillis = 300),
        label = "scrollbar",
    )
    drawWithContent {
        drawContent()
        val range = scrollState.maxValue
        if (thumb == null || range <= 0 || alpha <= 0f) return@drawWithContent
        val extent = size.height
        val total = extent + range
        val length = maxOf(thickness * 2, (extent * extent / total).toInt())
        val top = ((extent - length) * scrollState.value / range).toInt()
        thumb.setBounds(size.width.toInt() - thickness, top, size.width.toInt(), top + length)
        thumb.alpha = (alpha * 255).toInt()
        thumb.draw(drawContext.canvas.nativeCanvas)
    }
}

@Composable
private fun rememberTextViewWidth(text: String, fontSize: TextUnit, letterSpacing: Float): Dp {
    val density = LocalDensity.current
    return remember(text, fontSize, letterSpacing, density) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = with(density) { fontSize.roundToPx().toFloat() }
            this.letterSpacing = letterSpacing
            typeface = Typeface.SANS_SERIF
        }
        with(density) { ceil(Layout.getDesiredWidth(text, paint)).toInt().toDp() }
    }
}
