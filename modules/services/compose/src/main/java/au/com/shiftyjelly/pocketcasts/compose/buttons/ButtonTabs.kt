package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarDefaults.backgroundColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val ButtonHorizontalPadding = 12.dp
private val ButtonVerticalPadding = 8.dp

private val ButtonPaddingValues = PaddingValues(
    start = ButtonHorizontalPadding,
    top = ButtonVerticalPadding,
    end = ButtonHorizontalPadding,
    bottom = ButtonVerticalPadding
)

data class ButtonTab(
    @StringRes val labelResId: Int,
    val onClick: () -> Unit,
)

@Composable
fun ButtonTabs(
    tabs: List<ButtonTab>,
    selectedTab: ButtonTab,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        for (tab in tabs) {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val focused by interactionSource.collectIsFocusedAsState()
            val selected = selectedTab == tab
            val backgroundColor = if (selected) {
                // make the button background the same color as the text, black on the light theme and white on the dark theme
                MaterialTheme.theme.colors.primaryText01
            } else {
                if (pressed || focused) {
                    // the press and focus state is a lighter version of the button background color
                    MaterialTheme.theme.colors.primaryText01.copy(alpha = 0.1f)
                } else {
                    // the unselected button is the same color as the layout background
                    MaterialTheme.theme.colors.primaryUi02
                }
            }
            val textColor = if (selected) MaterialTheme.theme.colors.primaryUi02 else MaterialTheme.theme.colors.primaryText02
            Button(
                onClick = { tab.onClick() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = backgroundColor
                ),
                contentPadding = ButtonPaddingValues,
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp
                ),
                interactionSource = interactionSource,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = stringResource(tab.labelResId),
                    fontSize = 15.sp,
                    fontWeight = FontWeight(500),
                    letterSpacing = 0.5.sp,
                    color = textColor
                )
            }
        }
    }
}

@ShowkaseComposable(name = "ButtonTabs", group = "Button", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun ButtonTabsLightPreview() {
    ButtonTabsPreview(Theme.ThemeType.LIGHT)
}

@ShowkaseComposable(name = "ButtonTabs", group = "Button", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun ButtonTabsDarkPreview() {
    ButtonTabsPreview(Theme.ThemeType.DARK)
}

@Composable
private fun ButtonTabsPreview(themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        val episodesTab = ButtonTab(labelResId = LR.string.episodes, onClick = {})
        val bookmarksTab = ButtonTab(labelResId = LR.string.bookmarks, onClick = {})
        ButtonTabs(
            tabs = listOf(episodesTab, bookmarksTab),
            selectedTab = episodesTab,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
