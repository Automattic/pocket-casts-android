package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.images.R as IR

sealed class TimePlayButtonStyle {
    object Solid : TimePlayButtonStyle()
    object Outlined : TimePlayButtonStyle()
}

sealed class TimePlayButtonColors {
    @Composable
    abstract fun backgroundColor(): Color

    @Composable
    abstract fun textColor(): Color

    @Composable
    abstract fun borderColor(): Color

    data class Player(
        val textColor: Color,
    ) : TimePlayButtonColors() {
        @Composable
        override fun backgroundColor() = MaterialTheme.theme.colors.playerContrast01

        @Composable
        override fun textColor() = textColor

        @Composable
        override fun borderColor() = textColor
    }

    object Default : TimePlayButtonColors() {
        @Composable
        override fun backgroundColor() = Color.Transparent

        @Composable
        override fun textColor() = MaterialTheme.theme.colors.primaryText01

        @Composable
        override fun borderColor() = MaterialTheme.theme.colors.primaryText01
    }
}

@Composable
fun TimePlayButton(
    timeSecs: Int,
    @StringRes contentDescriptionId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: TimePlayButtonColors = TimePlayButtonColors.Default,
    buttonStyle: TimePlayButtonStyle = TimePlayButtonStyle.Outlined,
) {
    val timeText by remember {
        mutableStateOf(TimeHelper.formattedSeconds(timeSecs.toDouble()))
    }
    val description = stringResource(contentDescriptionId, timeText)
    val border = when (buttonStyle) {
        is TimePlayButtonStyle.Outlined -> BorderStroke(2.dp, colors.borderColor())
        is TimePlayButtonStyle.Solid -> null
    }
    OutlinedButton(
        onClick = onClick,
        border = border,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = colors.backgroundColor(),
        ),
        shape = RoundedCornerShape(50),
        modifier = modifier
            .semantics {
                contentDescription = description
            }
    ) {
        TextH40(
            text = timeText,
            color = colors.textColor(),
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Icon(
            painter = painterResource(IR.drawable.ic_play),
            contentDescription = null,
            tint = colors.textColor(),
            modifier = Modifier.size(10.dp, 13.dp)
        )
    }
}

@ShowkaseComposable(
    name = "TimePlayButton",
    group = "Button",
    styleName = "Outline - Light",
    defaultStyle = true
)
@Preview(name = "Light")
@Composable
fun TimePlayButtonLightPreview() {
    TimePlayButtonPreview(Theme.ThemeType.LIGHT)
}

@ShowkaseComposable(name = "TimePlayButton", group = "Button", styleName = "Outline - Dark")
@Preview(name = "Dark")
@Composable
fun TimePlayButtonDarkPreview() {
    TimePlayButtonPreview(Theme.ThemeType.DARK)
}

@ShowkaseComposable(name = "TimePlayButton", group = "Button", styleName = "Outline - Rose")
@Preview(name = "Rose")
@Composable
fun TimePlayButtonRosePreview() {
    TimePlayButtonPreview(Theme.ThemeType.ROSE)
}

@Composable
private fun TimePlayButtonPreview(themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        TimePlayButton(
            timeSecs = 121,
            contentDescriptionId = R.string.bookmark_play,
            onClick = {}
        )
    }
}

@ShowkaseComposable(name = "TimePlayButton", group = "Button", styleName = "Solid - Player")
@Preview(name = "Solid style - Player")
@Composable
fun TimePlayButtonFilledPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        TimePlayButton(
            timeSecs = 121,
            contentDescriptionId = R.string.bookmark_play,
            colors = TimePlayButtonColors.Player(
                textColor = Color.Black
            ),
            buttonStyle = TimePlayButtonStyle.Solid,
            onClick = {}
        )
    }
}
