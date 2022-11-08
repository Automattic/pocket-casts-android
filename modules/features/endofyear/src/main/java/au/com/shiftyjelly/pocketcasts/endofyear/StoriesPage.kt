package au.com.shiftyjelly.pocketcasts.endofyear

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ShareCompat
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesViewModel.State
import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryEpilogue
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryIntro
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryListenedCategories
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryListenedNumbers
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryListeningTime
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryLongestEpisode
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryTopFivePodcasts
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryTopListenedCategories
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryTopPodcast
import au.com.shiftyjelly.pocketcasts.endofyear.views.convertibleToBitmap
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryEpilogueView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryIntroView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryListenedCategoriesView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryListenedNumbersView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryListeningTimeView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryLongestEpisodeView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryTopFivePodcastsView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryTopListenedCategoriesView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryTopPodcastView
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Util
import timber.log.Timber
import java.io.File
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val ShareButtonStrokeWidth = 2.dp
private val StoryViewCornerSize = 10.dp
private val StoriesViewMaxSize = 700.dp
private const val MaxHeightPercentFactor = 0.9f
const val StoriesViewAspectRatioForTablet = 2f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StoriesPage(
    viewModel: StoriesViewModel,
    showDialog: Boolean,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (showDialog) {
        val shareLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            /* Share activity dismissed, start paused story */
            viewModel.start()
        }

        val context = LocalContext.current
        val dialogSize = remember { getDialogSize(context) }
        Dialog(
            onDismissRequest = { onCloseClicked.invoke() },
            properties = DialogProperties(usePlatformDefaultWidth = Util.isTablet(context)),
        ) {
            Box(modifier = modifier.size(dialogSize)) {
                DialogContent(viewModel, onCloseClicked) {
                    viewModel.onShareClicked(it, context) { file ->
                        showShareForFile(context, file, shareLauncher)
                    }
                }
            }
        }
    }
}

@Composable
fun DialogContent(
    viewModel: StoriesViewModel,
    onCloseClicked: () -> Unit,
    onShareClicked: (() -> Bitmap) -> Unit,
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
            onCloseClicked = onCloseClicked,
            onShareClicked = onShareClicked,
            onReplayClicked = { viewModel.replay() }
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
    onShareClicked: (() -> Bitmap) -> Unit,
    onReplayClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(color = Color.Black)) {
        var onCaptureBitmap: (() -> Bitmap)? = null
        state.currentStory?.let { story ->
            Box(modifier = modifier.weight(weight = 1f, fill = true)) {
                onCaptureBitmap =
                    convertibleToBitmap(content = {
                        StorySharableContent(
                            story,
                            onSkipPrevious,
                            onSkipNext,
                            onPause,
                            onStart,
                            onReplayClicked,
                            modifier
                        )
                    })
                SegmentedProgressIndicator(
                    progress = progress,
                    segmentsData = state.segmentsData,
                    modifier = modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                )
                CloseButtonView(onCloseClicked)
            }
        }
        requireNotNull(onCaptureBitmap).let {
            ShareButton(
                onClick = { onShareClicked.invoke(it) }
            )
        }
    }
}

@Composable
private fun StorySharableContent(
    story: Story,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onPause: () -> Unit,
    onStart: () -> Unit,
    onReplayClicked: () -> Unit,
    modifier: Modifier,
) {
    StorySwitcher(
        onSkipPrevious = onSkipPrevious,
        onSkipNext = onSkipNext,
        onPause = onPause,
        onStart = onStart,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(StoryViewCornerSize))
                .background(color = story.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            when (story) {
                is StoryIntro -> StoryIntroView(story)
                is StoryListeningTime -> StoryListeningTimeView(story)
                is StoryListenedCategories -> StoryListenedCategoriesView(story)
                is StoryTopListenedCategories -> StoryTopListenedCategoriesView(story)
                is StoryListenedNumbers -> StoryListenedNumbersView(story)
                is StoryTopPodcast -> StoryTopPodcastView(story)
                is StoryTopFivePodcasts -> StoryTopFivePodcastsView(story)
                is StoryLongestEpisode -> StoryLongestEpisodeView(story)
                is StoryEpilogue -> StoryEpilogueView(story, onReplayClicked)
            }
        }
    }
}

@Composable
private fun ShareButton(
    onClick: () -> Unit,
) {
    RowOutlinedButton(
        text = stringResource(id = LR.string.share),
        border = BorderStroke(ShareButtonStrokeWidth, Color.White),
        colors = ButtonDefaults
            .outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White,
            ),
        iconImage = Icons.Default.Share,
        onClick = {
            onClick.invoke()
        }
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

@Composable
private fun StorySwitcher(
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onPause: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)?,
) {
    var screenWidth by remember { mutableStateOf(1) }
    var isPaused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
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
        content?.invoke()
    }
}

private fun showShareForFile(
    context: Context,
    file: File,
    shareLauncher: ActivityResultLauncher<Intent>,
) {
    try {
        val uri = FileUtil.getUriForFile(context, file)

        val chooserIntent = ShareCompat.IntentBuilder(context)
            .setType("image/png")
            .addStream(uri)
            .setChooserTitle(LR.string.end_of_year_share_via)
            .createChooserIntent()

        shareLauncher.launch(chooserIntent)
    } catch (e: Exception) {
        Timber.e(e)
    }
}

private fun getDialogSize(context: Context): DpSize {
    val configuration = context.resources.configuration
    val screenHeightInDp = configuration.screenHeightDp
    val screenWidthInDp = configuration.screenWidthDp

    var dialogHeight = screenHeightInDp.toFloat()
    var dialogWidth = screenWidthInDp.toFloat()
    if (Util.isTablet(context)) {
        dialogHeight = (screenHeightInDp * MaxHeightPercentFactor).coerceAtMost(StoriesViewMaxSize.value)
        dialogWidth = dialogHeight / StoriesViewAspectRatioForTablet
    }

    return DpSize(dialogWidth.dp, dialogHeight.dp)
}

@Preview(showBackground = true)
@Composable
private fun StoriesScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        StoriesView(
            state = State.Loaded(
                currentStory = StoryListenedNumbers(ListenedNumbers(numberOfEpisodes = 1, numberOfPodcasts = 1)),
                segmentsData = State.Loaded.SegmentsData(
                    xStartOffsets = listOf(0.0f, 0.28f),
                    widths = listOf(0.25f, 0.75f)
                )
            ),
            progress = 0.75f,
            onSkipPrevious = {},
            onSkipNext = {},
            onPause = {},
            onStart = {},
            onCloseClicked = {},
            onShareClicked = {},
            onReplayClicked = {},
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
