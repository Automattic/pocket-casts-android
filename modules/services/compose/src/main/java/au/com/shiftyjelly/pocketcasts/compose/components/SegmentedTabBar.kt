package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SegmentedTabBar(
    items: List<String>,
    modifier: Modifier = Modifier,
    defaultSelectedItemIndex: Int = 0,
    colors: SegmentedTabBarColors = SegmentedTabBarDefaults.colors,
    cornerRadius: Dp = SegmentedTabBarDefaults.cornerRadius,
    outerPadding: Dp = SegmentedTabBarDefaults.outerPadding,
    tabContentPadding: PaddingValues = SegmentedTabBarDefaults.tabContentPadding,
    textStyle: TextStyle = SegmentedTabBarDefaults.textStyle,
    onItemSelected: (selectedItemIndex: Int) -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(defaultSelectedItemIndex) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.baseBackgroundColor)
            .padding(outerPadding),
    ) {
        Row(
            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
        ) {
            items.forEachIndexed { index, item ->
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentEnforcement provides false,
                ) {
                    OutlinedButton(
                        shape = RoundedCornerShape(cornerRadius),
                        border = null,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (selectedIndex == index) colors.selectedTabBackgroundColor else colors.unSelectedTabBackgroundColor,
                        ),
                        contentPadding = tabContentPadding,
                        onClick = {
                            selectedIndex = index
                            onItemSelected(selectedIndex)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                            .semantics { role = Role.Tab },
                    ) {
                        Text(
                            text = item,
                            color = if (selectedIndex == index) colors.selectedTabTextColor else colors.unSelectedTabTextColor,
                            fontSize = textStyle.fontSize,
                            letterSpacing = textStyle.letterSpacing,
                            lineHeight = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.W500,
                        )
                    }
                }
            }
        }
    }
}

object SegmentedTabBarDefaults {
    val colors = SegmentedTabBarColors()
    val cornerRadius: Dp = 24.dp
    val outerPadding: Dp = 4.dp
    val tabContentPadding: PaddingValues = ButtonDefaults.ContentPadding
    val textStyle = TextStyle(
        fontSize = 15.sp,
        letterSpacing = -(0.08).sp,
    )
}

data class SegmentedTabBarColors(
    val baseBackgroundColor: Color = Color.White.copy(alpha = .1f),
    val selectedTabBackgroundColor: Color = Color.White,
    val selectedTabTextColor: Color = Color.Black,
    val unSelectedTabBackgroundColor: Color = Color.Transparent,
    val unSelectedTabTextColor: Color = Color.White,
)

@ShowkaseComposable(name = "SegmentedTabBar", group = "TabBar", styleName = "Compact", defaultStyle = true)
@Preview
@Composable
fun SegmentedTabBarCompactPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SegmentedTabBar(
            items = listOf(stringResource(LR.string.plus_yearly), stringResource(LR.string.plus_monthly)),
            onItemSelected = {},
            modifier = Modifier.width(IntrinsicSize.Max),
        )
    }
}

@ShowkaseComposable(name = "SegmentedTabBar", group = "TabBar")
@Preview
@Composable
fun SegmentedTabBarFillWidthPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SegmentedTabBar(
            items = listOf(stringResource(LR.string.plus_yearly), stringResource(LR.string.plus_monthly)),
            onItemSelected = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
