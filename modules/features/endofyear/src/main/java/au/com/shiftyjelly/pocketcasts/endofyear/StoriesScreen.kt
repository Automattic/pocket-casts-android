package au.com.shiftyjelly.pocketcasts.endofyear

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesViewModel.State
import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryFake1
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryFake2
import au.com.shiftyjelly.pocketcasts.endofyear.storyviews.StoryFake1View
import au.com.shiftyjelly.pocketcasts.endofyear.storyviews.StoryFake2View
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val ShareButtonStrokeWidth = 2.dp
private val StoryViewCornerSize = 10.dp

@Composable
fun StoriesScreen(
    viewModel: StoriesViewModel,
    onCloseClicked: () -> Unit,
) {
    val state: State by viewModel.state.collectAsState()
    val progress: Float by viewModel.progress.collectAsState()
    when (state) {
        is State.Loaded -> StoriesView(
            state = state as State.Loaded,
            progress = progress,
            onSkipPrevious = { viewModel.skipPrevious() },
            onSkipNext = { viewModel.skipNext() },
            onPause = { viewModel.pause() },
            onStart = { viewModel.start() },
            onCloseClicked = onCloseClicked
        )
        State.Loading -> StoriesLoadingView(onCloseClicked)
        State.Error -> StoriesErrorView(onCloseClicked)
    }
}

@Composable
private fun StoriesView(
    state: State.Loaded,
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onPause: () -> Unit,
    onStart: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var screenWidth by remember { mutableStateOf(1) }
    var isPaused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black)
            .onGloballyPositioned {
                screenWidth = it.size.width
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isPaused) {
                            if (it.x > screenWidth / 2) {
                                onSkipNext()
                            } else {
                                onSkipPrevious()
                            }
                        }
                    },
                    onLongPress = {
                        isPaused = true
                        onPause()
                    },
                    onPress = {
                        awaitRelease()
                        if (isPaused) {
                            onStart()
                            isPaused = false
                        }
                    }
                )
            }
    ) {
        state.currentStory?.let {
            StoryView(it)
        }
        SegmentedProgressIndicator(
            progress = progress,
            numberOfSegments = state.numberOfStories,
            modifier = modifier
                .padding(8.dp)
                .fillMaxWidth(),
        )
        CloseButtonView(onCloseClicked)
    }
}

@Composable
private fun StoryView(
    story: Story,
    modifier: Modifier = Modifier,
) {
    Column {
        Box(
            modifier = modifier
                .fillMaxSize()
                .weight(weight = 1f, fill = true)
                .clip(RoundedCornerShape(StoryViewCornerSize))
                .background(color = story.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            when (story) {
                is StoryFake1 -> StoryFake1View(story)
                is StoryFake2 -> StoryFake2View(story)
            }
        }
        ShareButton()
    }
}

@Composable
private fun ShareButton() {
    RowOutlinedButton(
        text = stringResource(id = LR.string.share),
        border = BorderStroke(ShareButtonStrokeWidth, Color.White),
        colors = ButtonDefaults
            .outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White,
            ),
        iconImage = Icons.Default.Share,
        onClick = {}
    )
}

@Composable
private fun CloseButtonView(
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(
            onClick = onCloseClicked
        ) {
            Icon(
                imageVector = NavigationButton.Close.image,
                contentDescription = stringResource(NavigationButton.Close.contentDescription),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun StoriesLoadingView(
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StoriesEmptyView(
        content = { CircularProgressIndicator(color = Color.White) },
        onCloseClicked = onCloseClicked,
        modifier = modifier
    )
}

@Composable
private fun StoriesErrorView(
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StoriesEmptyView(
        content = {
            TextP50(
                text = "Failed to load stories.", // TODO: replace hardcoded text
                color = Color.White,
            )
        },
        onCloseClicked = onCloseClicked,
        modifier = modifier
    )
}

@Composable
private fun StoriesEmptyView(
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
        CloseButtonView(onCloseClicked)
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StoriesScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        StoriesView(
            state = State.Loaded(currentStory = StoryFake1(listOf(Podcast())), numberOfStories = 2),
            progress = 0.5f,
            onSkipPrevious = {},
            onSkipNext = {},
            onPause = {},
            onStart = {},
            onCloseClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StoriesLoadingViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        StoriesLoadingView(
            onCloseClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StoriesErrorViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        StoriesErrorView(
            onCloseClicked = {}
        )
    }
}
