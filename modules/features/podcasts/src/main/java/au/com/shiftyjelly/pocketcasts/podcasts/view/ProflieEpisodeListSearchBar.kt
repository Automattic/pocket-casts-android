package au.com.shiftyjelly.pocketcasts.podcasts.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun ProfileEpisodeListSearchBar(
    activeTheme: Theme.ThemeType,
    viewModel: ProfileEpisodeListViewModel = hiltViewModel<ProfileEpisodeListViewModel>(),
) {
    AppTheme(activeTheme) {
        val searchQueryFlow by viewModel.searchQueryFlow.collectAsStateWithLifecycle()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val focusRequester = remember { FocusRequester() }
        if (state.showSearchBar) {
            SearchBar(
                text = searchQueryFlow,
                placeholder = stringResource(R.string.search),
                onTextChanged = { viewModel.onSearchQueryChanged(it) },
                onSearch = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
            )
        }
    }
}
