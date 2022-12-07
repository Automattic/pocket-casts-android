package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastItem
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingRecommendationsSearchPage(
    onBackPressed: () -> Unit,
) {
    val viewModel = hiltViewModel<OnboardingRecommendationsSearchViewModel>()
    val state by viewModel.state.collectAsState()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler {
        onBackPressed()
    }

    Column(Modifier.fillMaxHeight()) {
        ThemedTopAppBar(
            title = stringResource(LR.string.onboarding_find_podcasts),
            onNavigationClick = onBackPressed
        )

        SearchBar(
            text = state.searchQuery,
            placeholder = stringResource(LR.string.search),
            onTextChanged = viewModel::updateSearchQuery,
            onSearch = { viewModel.queryImmediately() },
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
