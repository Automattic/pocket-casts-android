package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectAction

@Composable
internal fun MultiSelectActionRow(
    action: MultiSelectAction,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val iconWidth = rememberDrawableWidth(action.iconRes)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        WrapContentDrawable(
            drawableId = action.iconRes,
            tint = MaterialTheme.theme.colors.primaryIcon01,
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.CenterStart),
        )
        TextViewLine(
            text = AnnotatedString(stringResource(action.title)),
            size = 15.sp,
            color = MaterialTheme.theme.colors.primaryText01,
            isMedium = true,
            maxLines = Int.MAX_VALUE,
            modifier = Modifier
                .padding(
                    start = with(density) { (16.dp.roundToPx() + iconWidth + 24.dp.roundToPx()).toDp() },
                    end = 24.dp,
                )
                .align(Alignment.CenterStart),
        )
    }
}

@Composable
private fun WrapContentDrawable(
    @DrawableRes drawableId: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val drawable = remember(drawableId, context) { AppCompatResources.getDrawable(context, drawableId) }
    val width = drawable?.intrinsicWidth ?: 0
    val height = drawable?.intrinsicHeight ?: 0
    DrawableIcon(
        drawableId = drawableId,
        tint = tint,
        modifier = modifier.layout { measurable, _ ->
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(width, height) { placeable.place(0, 0) }
        },
    )
}

@Composable
private fun rememberDrawableWidth(@DrawableRes drawableId: Int): Int {
    val context = LocalContext.current
    return remember(drawableId, context) { AppCompatResources.getDrawable(context, drawableId)?.intrinsicWidth ?: 0 }
}
