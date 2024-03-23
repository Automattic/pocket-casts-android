package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun VerticalLogoPlus(modifier: Modifier = Modifier) {
    val pocketCastsPlusString = stringResource(LR.string.pocket_casts_plus)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = pocketCastsPlusString
        },
    ) {
        VerticalLogo()
        Image(
            painter = painterResource(IR.drawable.plus_logo),
            contentDescription = stringResource(LR.string.pocket_casts),
            modifier = Modifier.padding(top = 15.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerticalLogoPlusLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        VerticalLogoPlus()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VerticalLogoPlusDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        VerticalLogoPlus()
    }
}
