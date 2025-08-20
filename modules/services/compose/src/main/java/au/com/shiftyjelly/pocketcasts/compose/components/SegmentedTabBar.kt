package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SegmentedTabBar(
    items: List<String>,
    onSelectItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    colors: SegmentedTabBarColors = SegmentedTabBarDefaults.colors,
    cornerRadius: Dp = SegmentedTabBarDefaults.cornerRadius,
    border: BorderStroke = BorderStroke(SegmentedTabBarDefaults.borderThickness, colors.borderColor),
    textStyle: TextStyle = SegmentedTabBarDefaults.textStyle,
) {
    Surface(
        shape = RoundedCornerShape(cornerRadius),
        border = border,
        color = Color.Transparent,
        modifier = modifier
            .height(SegmentedTabBarDefaults.height),
    ) {
        Row(
            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
        ) {
            items.forEachIndexed { index, text ->
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentEnforcement provides false,
                ) {
                    SegmentedTab(
                        isSelected = selectedIndex == index,
                        text = text,
                        textStyle = textStyle,
                        colors = colors,
                        onSelectItem = {
                            onSelectItem(index)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.SegmentedTab(
    isSelected: Boolean,
    text: String,
    textStyle: TextStyle,
    colors: SegmentedTabBarColors,
    onSelectItem: () -> Unit,
) {
    OutlinedButton(
        shape = RectangleShape,
        border = null,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) colors.selectedTabBackgroundColor else colors.unSelectedTabBackgroundColor,
        ),
        onClick = onSelectItem,
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .defaultMinSize(minWidth = SegmentedTabBarDefaults.tabMinSize)
            .semantics { role = Role.Tab },
    ) {
        Text(
            text = text,
            color = if (isSelected) colors.selectedTabTextColor else colors.unSelectedTabTextColor,
            style = MaterialTheme.typography.button,
            fontSize = textStyle.fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

object SegmentedTabBarDefaults {
    val colors = SegmentedTabBarColors()
    val cornerRadius: Dp = 24.dp
    val borderThickness = 1.0.dp
    val textStyle = TextStyle(
        fontSize = 15.sp,
    )
    val height: Dp = 43.dp
    val tabMinSize: Dp = 150.dp
}

data class SegmentedTabBarColors(
    val selectedTabBackgroundColor: Color = Color.White,
    val selectedTabTextColor: Color = Color.Black,
    val selectedTabIconColor: Color = Color.White,
    val unSelectedTabBackgroundColor: Color = Color.Transparent,
    val unSelectedTabTextColor: Color = Color.White,
    val unSelectedTabIconColor: Color = Color.White,
    val borderColor: Color = Color.White.copy(alpha = .4f),
)

@Preview
@Composable
private fun SegmentedTabBarPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SegmentedTabBar(
            items = listOf(stringResource(LR.string.plus_yearly), stringResource(LR.string.plus_monthly)),
            onSelectItem = {},
            modifier = Modifier.width(IntrinsicSize.Max),
        )
    }
}
