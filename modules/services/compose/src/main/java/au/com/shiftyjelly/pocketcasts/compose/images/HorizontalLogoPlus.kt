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
fun HorizontalLogoPlus(modifier: Modifier = Modifier) {

    val resourceId = if (MaterialTheme.theme.isLight) {
        IR.drawable.plus_logo_horizontal_light
    } else {
        IR.drawable.plus_logo_horizontal_dark
    }

    Image(
        painter = painterResource(resourceId),
        contentDescription = stringResource(LR.string.pocket_casts_plus),
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HorizontalLogoPlusDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        HorizontalLogoPlus()
    }
}

@Preview(showBackground = true)
@Composable
private fun VerticalLogoPlusLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        HorizontalLogoPlus()
    }
}
