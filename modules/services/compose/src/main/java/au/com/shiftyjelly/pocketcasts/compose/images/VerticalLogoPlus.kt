package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun VerticalLogoPlus(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        VerticalLogo()
        Image(
            painter = painterResource(IR.drawable.plus_logo),
            contentDescription = stringResource(LR.string.pocket_casts),
            modifier = Modifier.padding(top = 15.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VerticalLogoPlusLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        VerticalLogoPlus()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun VerticalLogoPlusDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        VerticalLogoPlus()
    }
}
