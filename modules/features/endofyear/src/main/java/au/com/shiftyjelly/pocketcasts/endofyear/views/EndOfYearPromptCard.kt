package au.com.shiftyjelly.pocketcasts.endofyear.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val PromptCardSummaryTextAlpha = 0.8f
private val PromptCardCornerSize = 8.dp
private val PromptCardImageSize = 150.dp

private val LightThemeBackgroundColor = R.color.black_26
private val DarkThemeBackgroundColor = R.color.black_34

@Composable
fun EndOfYearPromptCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(color = MaterialTheme.theme.colors.primaryUi02)
    ) {
        Row(
            modifier = modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(PromptCardCornerSize))
                .background(
                    color = colorResource(
                        if (MaterialTheme.theme.isLight) LightThemeBackgroundColor else DarkThemeBackgroundColor
                    )
                )
                .clickable { onClick.invoke() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = modifier
                    .weight(1f)
                    .padding(top = 20.dp, bottom = 20.dp, start = 24.dp, end = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                TextH20(
                    text = stringResource(LR.string.end_of_year_prompt_card_title),
                    color = MaterialTheme.theme.colors.contrast01,
                    modifier = modifier
                        .padding(bottom = 8.dp)
                )
                TextH70(
                    text = stringResource(LR.string.end_of_year_prompt_card_summary),
                    color = MaterialTheme.theme.colors.contrast02,
                    modifier = modifier.alpha(PromptCardSummaryTextAlpha)
                )
            }
            Image(
                painter = painterResource(R.drawable.img_2022),
                contentDescription = null,
                modifier = modifier
                    .size(PromptCardImageSize)
                    .padding(top = 20.dp, bottom = 20.dp, end = 24.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EndOfYearPromptCardPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        EndOfYearPromptCard(onClick = {})
    }
}
