package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.TransitionState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val SearchBarMaxWidth = 500.dp
private val SearchViewCornerRadius = 38.dp

@Composable
fun TranscriptPageWrapper(
    playerViewModel: PlayerViewModel,
    transcriptViewModel: TranscriptViewModel,
    theme: Theme,
) {
    AppTheme(Theme.ThemeType.LIGHT) {
        val transitionState = playerViewModel.transitionState.collectAsStateWithLifecycle(null)

        val configuration = LocalConfiguration.current
        val connection = remember { object : NestedScrollConnection {} }

        var expandSearch by remember { mutableStateOf(false) }
        val onSearchClicked = {
            expandSearch = true
        }
        val onSearchDoneClicked = {
            expandSearch = false
        }
        when (transitionState.value) {
            is TransitionState.CloseTranscript -> {
                if (expandSearch) expandSearch = false
            }

            else -> Unit
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(connection),
        ) {
            TranscriptPage(
                playerViewModel = playerViewModel,
                transcriptViewModel = transcriptViewModel,
                theme = theme,
                modifier = Modifier
                    .height(configuration.screenHeightDp.dp),
            )

            TranscriptToolbar(
                onCloseClick = { playerViewModel.closeTranscript(withTransition = true) },
                onSearchDoneClicked = onSearchDoneClicked,
                onSearchClicked = onSearchClicked,
                expandSearch = expandSearch,
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TranscriptToolbar(
    onCloseClick: () -> Unit,
    onSearchDoneClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    expandSearch: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var searchFieldValue by remember { mutableStateOf(TextFieldValue()) }

    val onTextChanged = { text: String ->
        searchFieldValue = TextFieldValue(text)
    }

    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        CloseButton(
            modifier = Modifier.align(Alignment.TopStart),
            onClick = onCloseClick,
        )
        val transition = updateTransition(expandSearch, label = "search transition")
        transition.AnimatedVisibility(
            visible = { !it },
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SearchButton(
                onClick = onSearchClicked,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        transition.AnimatedVisibility(
            visible = { it },
            enter = expandHorizontally(),
            exit = shrinkHorizontally(targetWidth = { 50 }) + fadeOut(),
        ) {
            SearchBar(
                text = searchFieldValue.text,
                leadingIcon = {
                    SearchBarLeadingIcons(onTextChanged, onSearchDoneClicked)
                },
                trailingIcon = {
                    SearchBarTrailingIcons(searchFieldValue.text, onTextChanged)
                },
                placeholder = stringResource(LR.string.search),
                onTextChanged = onTextChanged,
                onSearch = {},
                cornerRadius = SearchViewCornerRadius,
                modifier = Modifier
                    .width(SearchBarMaxWidth)
                    .focusRequester(focusRequester)
                    .padding(start = 56.dp, end = 16.dp),
            )
        }
    }

    LaunchedEffect(expandSearch) {
        if (expandSearch) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }
}

@Composable
private fun SearchButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(end = 16.dp)
            .defaultMinSize(
                minHeight = 48.dp,
            )
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(SearchViewCornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = Color.White, radius = SearchViewCornerRadius),
                onClick = onClick,
            ),
    ) {
        TextH50(
            text = stringResource(LR.string.search),
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SearchBarLeadingIcons(
    onTextChanged: (String) -> Unit,
    onDoneClicked: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                onTextChanged("")
                onDoneClicked()
            },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(LR.string.done),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
        )
    }
}

@Composable
private fun SearchBarTrailingIcons(text: String, onTextChanged: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "0/0",
        )
        if (text.isNotEmpty()) {
            IconButton(
                onClick = {
                    onTextChanged("")
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cancel),
                    contentDescription = stringResource(LR.string.cancel),
                )
            }
        }
        IconButton(
            onClick = {},
            enabled = text.isNotEmpty(),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(LR.string.go_to_top),
            )
        }
        IconButton(
            onClick = {},
            enabled = text.isNotEmpty(),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(LR.string.go_to_bottom),
            )
        }
    }
}

@Preview("Collapsed search bar", heightDp = 100)
@Composable
private fun TranscriptToolbarPreview() {
    TranscriptToolbar(
        onCloseClick = {},
        onSearchDoneClicked = {},
        onSearchClicked = {},
        expandSearch = false,
    )
}

@Preview("Expanded search bar", heightDp = 100)
@Composable
private fun TranscriptToolbarExpandedSearchPreview() {
    TranscriptToolbar(
        onCloseClick = {},
        onSearchDoneClicked = {},
        onSearchClicked = {},
        expandSearch = true,
    )
}
