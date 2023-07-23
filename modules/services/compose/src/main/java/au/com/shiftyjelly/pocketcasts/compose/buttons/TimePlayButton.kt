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

sealed class TimePlayButtonStyle {
    object Solid : TimePlayButtonStyle()
    object Outline : TimePlayButtonStyle()
}

@Composable
fun TimePlayButton(
    timeSecs: Int,
    @StringRes contentDescriptionId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.theme.colors.primaryText01,
    backgroundColor: Color = MaterialTheme.colors.surface,
    borderColor: Color = MaterialTheme.theme.colors.primaryText01,
    buttonStyle: TimePlayButtonStyle = TimePlayButtonStyle.Outline
) {
    val timeText by remember {
        mutableStateOf(TimeHelper.formattedSeconds(timeSecs.toDouble()))
    }
    val description = stringResource(contentDescriptionId, timeText)
    val border = when (buttonStyle) {
        TimePlayButtonStyle.Outline -> BorderStroke(2.dp, borderColor)
        TimePlayButtonStyle.Solid -> null
    }
    OutlinedButton(
        onClick = onClick,
        border = border,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = backgroundColor,
        ),
        shape = RoundedCornerShape(50),
        modifier = modifier
            .semantics {
                contentDescription = description
            }
    ) {
        TextH40(
            text = timeText,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Icon(
            painter = painterResource(au.com.shiftyjelly.pocketcasts.images.R.drawable.ic_play),
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(10.dp, 13.dp)
        )
    }
}

@ShowkaseComposable(name = "TimePlayButton", group = "Button", styleName = "Outline - Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun TimePlayButtonLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        TimePlayButton(
            timeSecs = 121,
            contentDescriptionId = R.string.bookmark_play,
            onClick = {}
        )
    }
}

@ShowkaseComposable(name = "TimePlayButton", group = "Button", styleName = "Outline - Dark")
@Preview(name = "Dark")
@Composable
fun TimePlayButtonDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        TimePlayButton(
            timeSecs = 121,
            contentDescriptionId = R.string.bookmark_play,
            onClick = {}
        )
    }
}

@ShowkaseComposable(name = "TimePlayButton", group = "Button", styleName = "Outline - Rose")
@Preview(name = "Rose")
@Composable
fun TimePlayButtonRosePreview() {
    AppThemeWithBackground(Theme.ThemeType.ROSE) {
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
            backgroundColor = MaterialTheme.theme.colors.playerContrast01,
            textColor = MaterialTheme.colors.surface,
            buttonStyle = TimePlayButtonStyle.Solid,
            onClick = {}
        )
    }
}
