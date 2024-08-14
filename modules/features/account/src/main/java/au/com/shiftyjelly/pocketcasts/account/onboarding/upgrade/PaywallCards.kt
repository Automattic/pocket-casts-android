package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun DesktopWebAppCard() {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(394.dp)
            .width(313.dp)
            .background(Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(UR.color.coolgrey_90)),
        ) {
            Image(
                painter = painterResource(IR.drawable.about_logo_pocketcasts),
                contentDescription = stringResource(LR.string.paywall_desktop_and_web_content_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .padding(top = 57.dp)
                    .align(Alignment.CenterHorizontally),
            )
            TextH30(
                text = stringResource(LR.string.paywall_desktop_and_web_title),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.W600,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 38.dp, start = 24.dp, end = 24.dp)
                    .align(Alignment.Start),
            )
            TextP50(
                text = stringResource(LR.string.paywall_desktop_and_web_description),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.W400,
                color = colorResource(UR.color.coolgrey_50),
                modifier = Modifier
                    .padding(top = 4.dp, start = 24.dp, end = 24.dp, bottom = 48.dp)
                    .align(Alignment.Start),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DesktopWebAppCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        DesktopWebAppCard()
    }
}
