package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingRecommendationsStartPageViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.RecommendationPodcast
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBarButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.textH60FontSize
import au.com.shiftyjelly.pocketcasts.compose.extensions.header
import au.com.shiftyjelly.pocketcasts.compose.podcast.PodcastSubscribeImage
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingRecommendationsStartPage(
    onShown: () -> Unit,
    onImportClicked: () -> Unit,
    onSearch: () -> Unit,
    onBackPressed: () -> Unit,
    onComplete: () -> Unit,
) {

    LaunchedEffect(Unit) { onShown() }
    BackHandler { onBackPressed() }

    val viewModel = hiltViewModel<OnboardingRecommendationsStartPageViewModel>()
    val trendingPodcasts by viewModel.trendingPodcasts.collectAsState()

    Content(
        trendingPodcasts = trendingPodcasts,
        onImportClicked = onImportClicked,
        onSubscribeTap = viewModel::updateSubscribed,
        onSearch = onSearch,
        onComplete = onComplete
    )
}

@Composable
private fun Content(
    trendingPodcasts: List<RecommendationPodcast>,
    onImportClicked: () -> Unit,
    onSubscribeTap: (RecommendationPodcast) -> Unit,
    onSearch: () -> Unit,
    onComplete: () -> Unit,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)

        ) {
            TextH30(
                text = stringResource(LR.string.onboarding_recommendations_import),
                modifier = Modifier
                    .clickable { onImportClicked() }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.weight(1f)
        ) {

            header {
                Column {

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
                        modifier = Modifier.padding(bottom = 25.dp)
                    )

                    TextH20(
                        text = stringResource(LR.string.discover_trending),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            items(items = trendingPodcasts) {

                // Simulate minLines = 2
                // This is a bit of a hack based on https://stackoverflow.com/a/66401128/1910286
                // Google is working on adding a minLines capability though: https://issuetracker.google.com/issues/122476634
                val twoLines = with(LocalDensity.current) {
                    val pixelsInAPoint = 4 / 3
                    val lineHeight = textH60FontSize * pixelsInAPoint
                    val twoLines = lineHeight * 2
                    twoLines.toDp()
                }

                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    PodcastSubscribeImage(
                        podcastUuid = it.uuid,
                        podcastTitle = it.title,
                        podcastSubscribed = it.isSubscribed,
                        onSubscribeClick = { onSubscribeTap(it) },
                    )

                    Spacer(Modifier.height(8.dp))

                    TextH60(
                        text = it.title,
                        maxLines = 2,
                        modifier = Modifier
                            .heightIn(min = twoLines)
                            .clearAndSetSemantics {}
                    )
                }
            }
        }

        RowButton(
            text = stringResource(LR.string.not_now),
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
        Content(
            trendingPodcasts = listOf(
                RecommendationPodcast(
                    uuid = "e7a6f7d0-02f2-0133-1c51-059c869cc4eb",
                    title = "Short title",
                    isSubscribed = false,
                ),
                RecommendationPodcast(
                    uuid = "e7a6f7d0-02f2-0133-1c51-059c869cc4eb",
                    title = "A very very long title that is longer than will fit on two lines",
                    isSubscribed = true,
                )
            ),
            onImportClicked = {},
            onSubscribeTap = {},
            onSearch = {},
        ) {}
    }
}
