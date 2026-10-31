package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.TextPaint
import android.widget.ProgressBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.use
import androidx.core.graphics.withMatrix
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.profile.R
import kotlin.math.ceil
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

data class AddFileColour(val tintColorIndex: Int, val color: Int, val lockable: Boolean)

@Composable
internal fun AddFileImageButton(text: String, isLockVisible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val lockGap = with(density) { 4.dp.roundToPx() }
    val textSizePx = with(density) { 14.sp.roundToPx() }
    val textWidth = remember(text, textSizePx) {
        val paint = TextPaint().apply {
            textSize = textSizePx.toFloat()
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.0892857143f
        }
        ceil(android.text.Layout.getDesiredWidth(text, paint)).toInt()
    }
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Box(
                contentAlignment = TextViewAlignment.Center,
                modifier = Modifier
                    .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                TextViewText(
                    text = text,
                    fontSize = 14.sp,
                    color = MaterialTheme.theme.colors.primaryInteractive01,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.0892857143.em,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.width(with(density) { textWidth.toDp() }),
                )
            }
            if (isLockVisible) {
                DrawableImage(R.drawable.ic_locked_small, contentDescription = stringResource(LR.string.profile_file_locked))
            }
        },
    ) { measurables, constraints ->
        val button = measurables[0].measure(constraints.copy(minWidth = 0))
        val lock = measurables.getOrNull(1)?.measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(constraints.maxWidth, button.height) {
            val buttonX = (constraints.maxWidth - button.width + 1) / 2
            button.place(buttonX, 0)
            lock?.place(buttonX + button.width + lockGap, (button.height - lock.height + 1) / 2)
        }
    }
}

@Composable
internal fun AddFileColourPicker(
    colours: List<AddFileColour>,
    imageItem: Bitmap?,
    selectedIndex: Int,
    isLocked: Boolean,
    onColourClick: (Int) -> Unit,
    onLockedColourClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedRadius = with(LocalDensity.current) { 15.dp.roundToPx().toFloat() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 30.dp),
    ) {
        val items = listOf<AddFileColour?>(null) + colours
        items.forEachIndexed { index, colour ->
            val isLocked = colour != null && colour.lockable && isLocked
            val isHidden = colour == null && imageItem == null
            Box(
                modifier = Modifier
                    .padding(7.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(
                        when {
                            isHidden -> Modifier
                            isLocked -> Modifier.clickable(interactionSource = null, indication = null, onClick = onLockedColourClick)
                            else -> Modifier.clickable(interactionSource = null, indication = null) { onColourClick(index) }
                        },
                    ),
            ) {
                if (!isHidden) {
                    Box(Modifier.fillMaxSize().background(colour?.let { Color(it.color) } ?: MaterialTheme.theme.colors.support05, CircleShape))
                    if (colour == null && imageItem != null) {
                        BitmapImage(imageItem, crop = true, modifier = Modifier.fillMaxSize())
                    }
                    if (isLocked) {
                        DrawableImage(IR.drawable.ic_locked, contentDescription = null, tint = MaterialTheme.theme.colors.contrast01, modifier = Modifier.fillMaxSize())
                    }
                    if (index == selectedIndex) {
                        Box(
                            Modifier
                                .padding(7.dp)
                                .fillMaxSize()
                                .background(Color(0xAAFFFFFF), RoundedCornerShape(selectedRadius)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddFileUpgradeCallout(onClick: () -> Unit, onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.theme.colors
    val context = LocalContext.current
    val logo = remember(context) { context.obtainStyledAttributes(intArrayOf(UR.attr.plus_logo)).use { it.getResourceId(0, 0) } }
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.primaryUi05))
            Image(
                painter = painterResource(logo),
                contentDescription = stringResource(LR.string.pocket_casts_plus),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(232.dp, 32.dp),
            )
            TextViewText(
                text = stringResource(LR.string.profile_get_plus),
                fontSize = 15.sp,
                color = colors.primaryText02,
                fontWeight = FontWeight.Medium,
                lineSpacingMultiplier = 1.1f,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 64.dp, top = 16.dp, end = 64.dp)
                    .fillMaxWidth(),
            )
            TextViewText(
                text = stringResource(LR.string.plus_learn_more_about_plus),
                fontSize = 14.sp,
                color = colors.primaryInteractive01,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 64.dp, top = 16.dp, end = 64.dp)
                    .fillMaxWidth(),
            )
        }
        DrawableImage(
            drawableId = IR.drawable.ic_close,
            contentDescription = null,
            tint = colors.primaryText02,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
        )
        val closeDescription = stringResource(LR.string.settings_close_upgrade_offer)
        Spacer(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .layout { measurable, constraints ->
                    val logoWidth = with(density) { 232.dp.roundToPx() }
                    val width = constraints.maxWidth - (constraints.maxWidth - logoWidth + 1) / 2 - logoWidth - with(density) { 8.dp.roundToPx() }
                    val height = with(density) { 1.dp.roundToPx() + 16.dp.roundToPx() + 32.dp.roundToPx() + 16.dp.roundToPx() - 8.dp.roundToPx() }
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height))
                    layout(width, height) { placeable.place(0, 0) }
                }
                .semantics { contentDescription = closeDescription }
                .clickable(interactionSource = null, indication = null, onClick = onCloseClick),
        )
    }
}

@Composable
internal fun BitmapImage(bitmap: Bitmap, crop: Boolean, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    val drawable = remember(bitmap, resources) { BitmapDrawable(resources, bitmap) }
    Spacer(
        modifier = modifier.drawBehind {
            val width = drawable.intrinsicWidth.toFloat()
            val height = drawable.intrinsicHeight.toFloat()
            val matrix = Matrix()
            if (crop) {
                val scale = maxOf(size.width / width, size.height / height)
                matrix.setScale(scale, scale)
                matrix.postTranslate(Math.round((size.width - width * scale) * 0.5f).toFloat(), Math.round((size.height - height * scale) * 0.5f).toFloat())
            } else {
                matrix.setRectToRect(RectF(0f, 0f, width, height), RectF(0f, 0f, size.width, size.height), Matrix.ScaleToFit.CENTER)
            }
            drawable.setBounds(0, 0, width.toInt(), height.toInt())
            drawIntoCanvas { canvas -> canvas.nativeCanvas.withMatrix(matrix) { drawable.draw(this) } }
        },
    )
}

@Composable
internal fun AddFileImage(
    fileTint: Int?,
    isArtworkVisible: Boolean,
    artwork: Bitmap?,
    isLoading: Boolean,
    onImageClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(LR.string.profile_file)
    Box(contentAlignment = TextViewAlignment.Center, modifier = modifier.size(160.dp)) {
        DrawableImage(
            drawableId = IR.drawable.ic_uploadedfile,
            contentDescription = description,
            tint = fileTint?.let(::Color),
            modifier = Modifier
                .size(160.dp)
                .then(if (onImageClick != null) Modifier.clickable(interactionSource = null, indication = null, onClick = onImageClick) else Modifier),
        )
        if (isArtworkVisible) {
            if (artwork != null) {
                BitmapImage(artwork, crop = false, modifier = Modifier.size(160.dp).semantics { contentDescription = description })
            } else {
                DrawableImage(IR.drawable.defaultartwork, contentDescription = description, modifier = Modifier.size(160.dp))
            }
        }
        if (isLoading) {
            AndroidView(factory = ::ProgressBar)
        }
    }
}
