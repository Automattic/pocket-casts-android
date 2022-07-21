package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.extensions.header
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.podcasts.view.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * The second page when sharing a list of podcasts. Enter the list title and description.
 */
@Composable
fun ShareListCreateTitlePage(
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    viewModel: ShareListCreateViewModel,
    modifier: Modifier = Modifier
) {
    val state: ShareListCreateViewModel.State by viewModel.state.collectAsState()

    Column(modifier = modifier.background(MaterialTheme.theme.colors.primaryUi01)) {
        ThemedTopAppBar(
            title = stringResource(LR.string.podcasts_share_create_list),
            navigationButton = NavigationButton.Back,
            onNavigationClick = onBackClick,
            actions = {
                IconButton(onClick = onNextClick, enabled = state.title.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(LR.string.share)
                    )
                }
            }
        )
        ShareListCreateTitleContent(
            podcasts = state.selectedPodcastsOrdered,
            title = state.title,
            description = state.description,
            onTitleChange = { viewModel.changeTitle(it) },
            onDescriptionChange = { viewModel.changeDescription(it) },
            onDoneClick = onNextClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShareListCreateTitleContent(
    podcasts: List<Podcast>,
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val inLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val imageMinSize = if (inLandscape) 120.dp else 80.dp
    Column {
        if (!inLandscape) {
            TitleDescriptionFields(
                title = title,
                description = description,
                onTitleChange = onTitleChange,
                onDescriptionChange = onDescriptionChange,
                onDoneClick = onDoneClick,
                focusRequester = focusRequester,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        LazyVerticalGrid(
            cells = GridCells.Adaptive(minSize = imageMinSize),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
        ) {
            if (inLandscape) {
                header {
                    TitleDescriptionFields(
                        title = title,
                        description = description,
                        onTitleChange = onTitleChange,
                        onDescriptionChange = onDescriptionChange,
                        onDoneClick = onDoneClick,
                        focusRequester = focusRequester
                    )
                }
            }
            items(items = podcasts) { podcast ->
                PodcastImage(
                    uuid = podcast.uuid,
                    title = podcast.title,
                    showTitle = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun TitleDescriptionFields(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDoneClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier) {
        FormField(
            value = title,
            placeholder = stringResource(LR.string.podcasts_share_title),
            onValueChange = onTitleChange,
            imeAction = ImeAction.Next,
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.focusRequester(focusRequester)
        )
        Spacer(Modifier.height(8.dp))
        FormField(
            value = description,
            placeholder = stringResource(LR.string.podcasts_share_description),
            onValueChange = onDescriptionChange,
            onNext = onDoneClick,
            imeAction = ImeAction.Default,
            singleLine = false
        )
        Spacer(Modifier.height(16.dp))
    }
}
