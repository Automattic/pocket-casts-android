package au.com.shiftyjelly.pocketcasts.profile.champion

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChampionDialog(
    modifier: Modifier = Modifier,
    onRateClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painterResource(IR.drawable.ic_rounded_gray),
                contentDescription = stringResource(LR.string.pocket_casts_logo),
                contentScale = ContentScale.Fit,
                modifier = modifier
                    .size(88.dp)
                    .padding(bottom = 16.dp),
            )

            TextH20(
                text = stringResource(LR.string.pocket_casts_champion_dialog_title),
                textAlign = TextAlign.Center,
                modifier = modifier.padding(bottom = 8.dp),
            )

            TextP40(
                text = stringResource(LR.string.pocket_casts_champion_dialog_description),
                fontSize = 14.sp,
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = modifier.padding(bottom = 16.dp),
            )

            RowButton(
                text = stringResource(LR.string.rate_pocket_casts),
                contentDescription = stringResource(LR.string.rate_pocket_casts),
                onClick = { onRateClick() },
                includePadding = false,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.theme.colors.primaryText01,
                    disabledBackgroundColor = MaterialTheme.theme.colors.primaryInteractive03,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChampionDialog(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        ChampionDialog(
            onRateClick = {},
        )
    }
}
