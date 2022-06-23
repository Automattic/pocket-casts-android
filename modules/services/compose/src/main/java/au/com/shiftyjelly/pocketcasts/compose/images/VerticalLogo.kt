package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.compose.foundation.Image
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun VerticalLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(if (MaterialTheme.theme.isLight) IR.drawable.ic_logo_title_ver_light else IR.drawable.ic_logo_title_ver_dark),
        contentDescription = stringResource(LR.string.pocket_casts),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun VerticalLogoLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        VerticalLogo()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun VerticalLogoDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        VerticalLogo()
    }
}
