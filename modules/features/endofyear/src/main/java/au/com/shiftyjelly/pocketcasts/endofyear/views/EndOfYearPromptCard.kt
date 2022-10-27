package au.com.shiftyjelly.pocketcasts.endofyear.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val PromptCardCornerSize = 5.dp
private val PromptCardTextColor = Color.White

@Composable
fun EndOfYearPromptCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(color = MaterialTheme.theme.colors.primaryUi02)
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(PromptCardCornerSize))
                .background(color = Color.Black)
                .clickable { onClick.invoke() },
        ) {
            TextH30(
                text = stringResource(LR.string.end_of_year_prompt_card_title),
                color = PromptCardTextColor,
                modifier = modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            TextH70(
                text = stringResource(LR.string.end_of_year_prompt_card_summary),
                color = PromptCardTextColor,
                modifier = modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
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
