package au.com.shiftyjelly.pocketcasts.endofyear

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ProgressDurationMs = 5_000
private val ShareButtonStrokeWidth = 2.dp
private val StoryViewCornerSize = 10.dp
private const val NumberOfSegments = 2

@Composable
fun StoriesScreen(
    viewModel: StoriesViewModel,
    onCloseClicked: () -> Unit,
) {
    val state: State by viewModel.state.collectAsState()
    var running by remember { mutableStateOf(false) }
    val progress: Float by animateFloatAsState(
        if (running) 1f else 0f,
        animationSpec = tween(
            durationMillis = ProgressDurationMs,
            easing = LinearEasing
        )
    )
    when (state) {
        State.Loaded -> StoriesView(
            state = state as State.Loaded,
            progress = progress,
            onCloseClicked = onCloseClicked
        )
        State.Loading -> StoriesLoadingView(onCloseClicked)
        State.Error -> StoriesErrorView(onCloseClicked)
    }

    LaunchedEffect(Unit) {
        running = true
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun StoriesView(
    state: State,
    progress: Float,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(color = Color.Black)) {
        StoryView(color = Color.Gray)
        SegmentedProgressIndicator(
            progress = progress,
            numberOfSegments = NumberOfSegments,
            modifier = modifier
                .padding(8.dp)
                .fillMaxWidth(),
        )
        CloseButtonView(onCloseClicked)
    }
}

@Composable
private fun StoryView(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column {
        Box(
            modifier = modifier
                .fillMaxSize()
                .weight(weight = 1f, fill = true)
                .clip(RoundedCornerShape(StoryViewCornerSize))
                .background(color = color)
        ) {}
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
            state = State.Loaded,
            progress = 1f,
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
