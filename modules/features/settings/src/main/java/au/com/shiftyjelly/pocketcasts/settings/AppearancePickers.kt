package au.com.shiftyjelly.pocketcasts.settings

import android.graphics.drawable.GradientDrawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import kotlin.math.min
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class AppearanceOption(
    @DrawableRes val imageResId: Int,
    val label: String,
    val imageContentDescription: String,
    @DrawableRes val badgeResId: Int?,
    val isSelected: Boolean,
    val isLocked: Boolean,
)

@Composable
internal fun AppearancePicker(
    options: List<AppearanceOption>,
    isTall: Boolean,
    endPadding: Dp,
    scrollRequest: Int,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val selectedIndex = options.indexOfFirst { it.isSelected }.coerceAtLeast(0)
    LaunchedEffect(options.isNotEmpty(), scrollRequest) {
        if (options.isNotEmpty()) {
            listState.scrollToItem(selectedIndex)
        }
    }
    val cellWidth = rememberCellWidth()
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(start = 72.dp, end = endPadding),
        modifier = modifier,
    ) {
        itemsIndexed(options) { index, option ->
            AppearanceCell(
                option = option,
                cellWidth = cellWidth,
                isTall = isTall,
                onClick = { onClick(index) },
            )
        }
    }
}

@Composable
private fun rememberCellWidth(): Int {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    return remember(context, density, screenWidth) {
        val mainWidth = context.resources.displayMetrics.widthPixels
        val maxCellWidth = (124 * density.density).toInt().toFloat()
        val peekWidth = (30 * density.density).toInt().toFloat()
        min(maxCellWidth, (mainWidth - peekWidth - 4) / 3).toInt()
    }
}

@Composable
private fun AppearanceCell(
    option: AppearanceOption,
    cellWidth: Int,
    isTall: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.theme.colors
    val density = LocalDensity.current
    val margin = with(density) { 8.dp.roundToPx() }
    val strokeWidth = (4 * density.density).toInt()
    val context = LocalContext.current
    val panelDrawable = remember(context) {
        requireNotNull(AppCompatResources.getDrawable(context, R.drawable.background_selectable_panel)).mutate() as GradientDrawable
    }
    val cardBackground = rememberCardBackground()
    Layout(
        content = {
            val strokeColor = if (option.isSelected) colors.support01 else colors.primaryUi04
            Box(
                modifier = Modifier.drawBehind {
                    panelDrawable.setStroke(strokeWidth, strokeColor.toArgb())
                    panelDrawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                    panelDrawable.draw(drawContext.canvas.nativeCanvas)
                },
            )
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .background(cardBackground),
            ) {
                DrawableImage(
                    drawableId = option.imageResId,
                    contentDescription = option.imageContentDescription,
                    modifier = Modifier.fillMaxSize(),
                )
                if (option.badgeResId != null) {
                    DrawableImage(
                        drawableId = option.badgeResId,
                        contentDescription = stringResource(if (option.isSelected) LR.string.on else LR.string.off),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = if (isTall) 8.dp else 0.dp, bottom = if (isTall) 8.dp else 0.dp)
                            .size(40.dp),
                    )
                }
            }
            if (option.isLocked) {
                DrawableImage(
                    drawableId = IR.drawable.ic_locked,
                    contentDescription = stringResource(LR.string.settings_lock_image),
                    tint = colors.primaryText02,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Box(Modifier)
            }
            TextViewText(
                text = option.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.primaryText01,
            )
        },
        modifier = Modifier
            .background(colors.primaryUi04)
            .alpha(if (option.isLocked) 0.65f else 1f)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) { measurables, _ ->
        val panelHeight = (cellWidth * if (isTall) 41f / 31f else 1f).roundToInt()
        val cardWidth = cellWidth - margin * 2
        val cardHeight = (cardWidth * if (isTall) 37f / 27f else 1f).roundToInt()
        val panel = measurables[0].measure(Constraints.fixed(cellWidth, panelHeight))
        val card = measurables[1].measure(Constraints.fixed(cardWidth, cardHeight))
        val lock = measurables[2].measure(Constraints())
        val titleStart = if (option.isLocked) margin + lock.width + margin else margin
        val title = measurables[3].measure(Constraints.fixedWidth(cellWidth - titleStart - with(density) { 4.dp.roundToPx() }))
        val titleTop = panelHeight + margin
        val bottomMargin = if (isTall) margin else with(density) { 16.dp.roundToPx() }
        val height = if (isTall) (cellWidth * (210f / 124f)).toInt() else titleTop + title.height + bottomMargin
        val titleY = if (isTall) titleTop + (height - bottomMargin - titleTop - title.height) / 2 else titleTop
        layout(cellWidth, height) {
            panel.place(0, 0)
            card.place(margin, margin)
            lock.place(margin, titleY + (title.height - lock.height) / 2)
            title.place(titleStart, titleY)
        }
    }
}

@Composable
private fun rememberCardBackground(): Color {
    val context = LocalContext.current
    return remember(context) {
        val windowBackground = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground)).use { it.getColor(0, 0) }
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(windowBackground, it) }
        if (hsv[2] > 0.5f) Color(0xFFFFFFFF) else Color(0xFF424242)
    }
}
