package au.com.shiftyjelly.pocketcasts.compose.bars

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText

@Composable
fun AppCompatTopAppBar(
    title: String,
    navigationButton: NavigationButton,
    onNavigationClick: () -> Unit,
    iconColor: Color,
    titleColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    ThemedTopAppBar(
        title = {
            Box(
                contentAlignment = TextViewAlignment.CenterStart,
                modifier = Modifier.fillMaxHeight(),
            ) {
                TextViewText(
                    text = title,
                    fontSize = 20.sp,
                    color = titleColor,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.0125.em,
                    maxLines = 1,
                )
            }
        },
        navigationButton = navigationButton,
        onNavigationClick = onNavigationClick,
        iconColor = iconColor,
        backgroundColor = backgroundColor,
        bottomShadow = true,
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top),
        modifier = modifier,
    )
}
