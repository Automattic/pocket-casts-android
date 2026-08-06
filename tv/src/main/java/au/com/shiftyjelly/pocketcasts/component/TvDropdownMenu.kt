package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun TvDropdownMenu(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    width: Dp = DefaultMenuWidth,
    alignment: Alignment = Alignment.TopEnd,
    offset: DpOffset = DpOffset(x = 0.dp, y = 48.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val intOffset = with(LocalDensity.current) {
        IntOffset(x = offset.x.roundToPx(), y = offset.y.roundToPx())
    }
    Popup(
        alignment = alignment,
        offset = intOffset,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        TvDropdownMenuSurface(
            title = title,
            width = width,
            modifier = modifier,
            content = content,
        )
    }
}

@Composable
internal fun TvDropdownMenuSurface(
    modifier: Modifier = Modifier,
    title: String? = null,
    width: Dp = DefaultMenuWidth,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .width(width)
            .clip(MenuShape)
            .background(TvOverlayContainerColor)
            .border(1.dp, TvOverlayBorderColor, MenuShape)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = TvTextStyles.PlaylistCardCaption,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            )
        }
        content()
    }
}

@Composable
fun TvDropdownMenuItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusModifier = if (isSelected) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = TvColors.LightGray,
            focusedContentColor = TvColors.Dark,
        ),
        border = TvButtonDefaults.borderlessButtonBorder(),
        modifier = modifier
            .fillMaxWidth()
            .then(focusModifier),
    ) {
        Box(modifier = Modifier.size(14.dp)) {
            if (isSelected) {
                Icon(
                    painter = painterResource(IR.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = label,
            modifier = Modifier.padding(start = 10.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

private val DefaultMenuWidth = 256.dp
private val MenuShape = RoundedCornerShape(20.dp)

@Preview
@Composable
private fun TvDropdownMenuPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvDropdownMenuSurface(title = "Sort by") {
                TvDropdownMenuItem(
                    label = "Newest to oldest",
                    isSelected = true,
                    onClick = {},
                )
                TvDropdownMenuItem(
                    label = "Oldest to newest",
                    isSelected = false,
                    onClick = {},
                )
            }
        }
    }
}
