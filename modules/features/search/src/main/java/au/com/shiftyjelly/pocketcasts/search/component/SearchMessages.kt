package au.com.shiftyjelly.pocketcasts.search.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.search.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun NoResultsView() {
    MessageView(
        imageResId = R.drawable.search,
        titleResId = LR.string.search_no_podcasts_found,
        summaryResId = LR.string.search_no_podcasts_found_summary,
    )
}

@Composable
fun NoSuggestionsView() {
    MessageView(
        imageResId = R.drawable.search,
        titleResId = LR.string.search_suggestions_no_results_title,
        summaryResId = LR.string.search_suggestions_no_results_message,
    )
}

@Composable
fun SearchFailedView() {
    MessageView(
        imageResId = IR.drawable.search_failed,
        titleResId = LR.string.error_search_failed,
        summaryResId = LR.string.error_check_your_internet_connection,
    )
}

@Composable
private fun MessageView(
    @DrawableRes imageResId: Int,
    @StringRes titleResId: Int,
    @StringRes summaryResId: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
            modifier = Modifier
                .size(96.dp)
                .padding(top = 32.dp, bottom = 16.dp),
        )
        TextH20(
            text = stringResource(titleResId),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        TextP50(
            text = stringResource(summaryResId),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            color = MaterialTheme.theme.colors.primaryText02,
        )
    }
}

@Preview
@Composable
private fun NoResultsViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        NoResultsView()
    }
}

@Preview
@Composable
private fun SearchFailedViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SearchFailedView()
    }
}
