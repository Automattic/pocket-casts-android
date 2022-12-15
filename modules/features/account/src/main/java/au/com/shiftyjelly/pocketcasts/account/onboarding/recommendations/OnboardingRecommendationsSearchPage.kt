package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastItem
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingRecommendationsSearchPage(
    theme: Theme.ThemeType,
    onBackPressed: () -> Unit,
) {

    val viewModel = hiltViewModel<OnboardingRecommendationsSearchViewModel>()
    val state by viewModel.state.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val systemUiController = rememberSystemUiController()
    val pocketCastsTheme = MaterialTheme.theme
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        systemUiController.apply {
            // Use secondaryUI01 so the status bar matches the ThemedTopAppBar
            setStatusBarColor(pocketCastsTheme.colors.secondaryUi01, darkIcons = !theme.defaultLightIcons)
            setNavigationBarColor(Color.Transparent, darkIcons = !theme.darkTheme)
        }
    }

    BackHandler {
        onBackPressed()
    }

    Column(
        Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.ime)
            .fillMaxHeight()
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.onboarding_find_podcasts),
            onNavigationClick = onBackPressed
        )

        SearchBar(
            text = state.searchQuery,
            placeholder = stringResource(LR.string.search),
            onTextChanged = viewModel::updateSearchQuery,
            onSearch = with(LocalContext.current) {
                { viewModel.queryImmediately(this) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester)
        )

        Box(Modifier.height(2.dp)) {
            Divider(color = MaterialTheme.theme.colors.secondaryUi02)
            if (state.loading && state.results.isNotEmpty()) {
                // Provide a subtle loading indicator when results are displayed, but being updated
                LinearProgressIndicator(
                    color = MaterialTheme.theme.colors.secondaryUi01,
                    backgroundColor = MaterialTheme.theme.colors.secondaryUi02,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        if (state.loading && state.results.isEmpty()) {
            // Provide an obvious loading indicator when loading and there no results yet
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.weight(5f))
        } else {
            LazyColumn {
                items(state.results) {
                    PodcastItem(
                        podcast = it.podcast,
                        subscribed = it.isSubscribed,
                        showSubscribed = true,
                        showPlusIfUnsubscribed = true,
                        onClick = { viewModel.toggleSubscribed(it) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun OnboardingRecommendationSearchPage_Preview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingRecommendationsSearchPage(
            theme = themeType,
            onBackPressed = {},
        )
    }
}
