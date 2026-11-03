package au.com.shiftyjelly.pocketcasts.profile.sonos

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.AppCompatTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.ViewMaterialButton
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class SonosTextColors(
    val title: Color,
    val explanation: Color,
    val button: Color,
)

@Composable
internal fun SonosAppLinkPage(
    @DrawableRes imageRes: Int,
    explanation: String,
    buttonText: String,
    textColors: SonosTextColors,
    onCloseClick: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppCompatTopAppBar(
            title = stringResource(LR.string.profile_sonos_connect_to),
            navigationButton = NavigationButton.Drawable(IR.drawable.ic_cancel, LR.string.close),
            onNavigationClick = onCloseClick,
            iconColor = MaterialTheme.theme.colors.secondaryIcon01,
            titleColor = MaterialTheme.theme.colors.secondaryText01,
            backgroundColor = MaterialTheme.theme.colors.secondaryUi01,
            modifier = toolbarModifier,
        )
        DrawableImage(
            drawableId = imageRes,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 60.dp)
                .align(BiasAlignment.Horizontal(-0.1f)),
        )
        TextViewText(
            text = stringResource(LR.string.profile_sonos_title),
            fontSize = 22.sp,
            color = textColors.title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 20.dp, top = 48.dp, end = 20.dp)
                .align(Alignment.CenterHorizontally),
        )
        TextViewText(
            text = explanation,
            fontSize = 16.sp,
            color = textColors.explanation,
            letterSpacing = (-0.01).em,
            lineSpacingMultiplier = 1.3f,
            modifier = Modifier
                .padding(start = 20.dp, top = 32.dp, end = 20.dp)
                .fillMaxWidth(),
        )
        ViewMaterialButton(
            text = buttonText,
            onClick = onConnectClick,
            backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
            textColor = textColors.button,
            modifier = Modifier
                .padding(start = 20.dp, top = 56.dp, end = 20.dp)
                .fillMaxWidth()
                .height(56.dp),
        )
    }
}

@Preview
@Composable
private fun SonosAppLinkPagePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SonosAppLinkPage(
            imageRes = IR.drawable.sonos_light,
            explanation = stringResource(LR.string.profile_sonos_need_account),
            buttonText = stringResource(LR.string.profile_sonos_setup_account),
            textColors = SonosTextColors(title = Color.Black, explanation = Color.Black, button = Color.White),
            onCloseClick = {},
            onConnectClick = {},
        )
    }
}
