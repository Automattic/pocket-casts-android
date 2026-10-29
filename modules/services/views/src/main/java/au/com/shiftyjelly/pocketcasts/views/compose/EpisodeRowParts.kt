package au.com.shiftyjelly.pocketcasts.views.compose

import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Precision
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun EpisodeRowArtwork(
    episode: BaseEpisode,
    useEpisodeArtwork: Boolean,
    imageRequestFactory: PocketCastsImageRequestFactory,
    isGreyedOut: Boolean,
) {
    val request = remember(episode, useEpisodeArtwork, imageRequestFactory) { imageRequestFactory.create(episode, useEpisodeArtwork) }
    EpisodeRowArtwork(request, alpha = if (isGreyedOut) 0.5f else 1f, elevation = if (isGreyedOut) 0f else 2f)
}

@Composable
internal fun EpisodeRowArtwork(
    request: ImageRequest,
    alpha: Float,
    elevation: Float,
    modifier: Modifier = Modifier,
    cardAlpha: Float = 1f,
) {
    val context = LocalContext.current
    val cardBackground = remember(context) { cardBackgroundColor(context) }
    val sizePx = with(LocalDensity.current) { 56.dp.roundToPx() }
    val sizedRequest = remember(request, sizePx) {
        if (request.defined.sizeResolver == null) request.newBuilder().size(sizePx).precision(Precision.INEXACT).build() else request
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = cardAlpha
                shadowElevation = (elevation * density).toInt().toFloat()
                shape = RoundedCornerShape(4.dp)
                clip = true
            }
            .background(cardBackground),
    ) {
        Image(
            painter = rememberAsyncImagePainter(sizedRequest, contentScale = ContentScale.Fit),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .imageViewAlpha(alpha),
        )
    }
}

private fun cardBackgroundColor(context: android.content.Context): Color {
    val attributes = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
    val themeBackground = attributes.getColor(0, 0)
    attributes.recycle()
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(themeBackground, hsv)
    return if (hsv[2] > 0.5f) Color(0xFFFFFFFF) else Color(0xFF424242)
}

@Composable
internal fun EpisodeRowHeader(
    date: Spanned,
    color: Color,
    showVideoIcon: Boolean,
    colors: EpisodeRowColors,
) {
    Row(
        verticalAlignment = TruncatedCenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
    ) {
        if (showVideoIcon) {
            DrawableIcon(
                drawableId = IR.drawable.ic_video_small_fill,
                tint = colors.theme.support01,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp),
            )
        }
        TextViewLine(
            text = date.toUpperCaseAnnotatedString(),
            size = 11.sp,
            color = color,
            isMedium = true,
            letterSpacing = 0.07.em,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Top),
        )
    }
}

@Composable
internal fun EpisodeRowTitle(
    title: String,
    color: Color,
    starTint: Color?,
) {
    Layout(
        contents = listOf(
            { TextViewLine(AnnotatedString(title), 16.sp, color, maxLines = 2, modifier = Modifier.clearAndSetSemantics {}) },
            { if (starTint != null) FitCenterDrawableIcon(IR.drawable.ic_starred_row, starTint, Modifier.width(16.dp)) },
        ),
    ) { (titles, stars), constraints ->
        val starWidth = stars.firstOrNull()?.maxIntrinsicWidth(0) ?: 0
        val titleWidth = (constraints.maxWidth - starWidth).coerceAtLeast(0)
        val titlePlaceable = titles.single().measure(Constraints(minWidth = titleWidth, maxWidth = titleWidth))
        val starPlaceable = stars.firstOrNull()?.measure(Constraints.fixed(starWidth, titlePlaceable.height))
        layout(constraints.maxWidth, titlePlaceable.height) {
            titlePlaceable.place(0, 0)
            starPlaceable?.place(titleWidth, 0)
        }
    }
}

internal fun Spanned.toUpperCaseAnnotatedString(): AnnotatedString {
    val locale = Locale.getDefault()
    val spans = getSpans(0, length, ForegroundColorSpan::class.java)
    val boundaries = (spans.flatMap { listOf(getSpanStart(it), getSpanEnd(it)) } + listOf(0, length)).distinct().sorted()
    val offsets = mutableMapOf(0 to 0)
    val text = StringBuilder()
    boundaries.zipWithNext { start, end ->
        text.append(substring(start, end).uppercase(locale))
        offsets[end] = text.length
    }
    return buildAnnotatedString {
        append(text)
        spans.forEach { span ->
            addStyle(SpanStyle(color = Color(span.foregroundColor)), offsets.getValue(getSpanStart(span)), offsets.getValue(getSpanEnd(span)))
        }
    }
}
