package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBarButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingRecommendationsStartPage(
    onShown: () -> Unit,
    onSearch: () -> Unit,
    onBackPressed: () -> Unit,
    onComplete: () -> Unit,
) {

    LaunchedEffect(Unit) { onShown() }
    BackHandler { onBackPressed() }

    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(all = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)

        ) {
            TextH30(stringResource(LR.string.onboarding_recommendations_import))
        }

        TextH10(
            text = stringResource(LR.string.onboarding_recommendations_find_favorite_podcasts),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextP40(
            text = stringResource(LR.string.onboarding_recommendations_make_pocket_casts_yours),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SearchBarButton(
            text = stringResource(LR.string.search),
            onClick = onSearch,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .aspectRatio(1.2f)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(Color.Gray),
        ) {
            TextP40("PLACEHOLDER")
        }

        Spacer(Modifier.weight(1f))

        RowButton(
            text = stringResource(LR.string.not_now),
            includePadding = false,
            onClick = onComplete,
        )
    }
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingRecommendationsStartPage(
            onSearch = {},
            onShown = {},
            onBackPressed = {},
            onComplete = {},
        )
    }
}
