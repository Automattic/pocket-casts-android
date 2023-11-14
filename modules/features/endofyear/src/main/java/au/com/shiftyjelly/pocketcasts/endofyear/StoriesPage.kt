package au.com.shiftyjelly.pocketcasts.endofyear

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowCloseButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.ShareableTextProvider.ShareTextData
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesViewModel.State
import au.com.shiftyjelly.pocketcasts.endofyear.components.PaidStoryWallView
import au.com.shiftyjelly.pocketcasts.endofyear.utils.waitForUpOrCancelInitial
import au.com.shiftyjelly.pocketcasts.endofyear.views.SegmentedProgressIndicator
import au.com.shiftyjelly.pocketcasts.endofyear.views.convertibleToBitmap
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryCompletionRateView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryEpilogueView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryIntroView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryListenedCategoriesView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryListenedNumbersView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryListeningTimeView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryLongestEpisodeView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryTopFivePodcastsView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryTopListenedCategoriesView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryTopPodcastView
import au.com.shiftyjelly.pocketcasts.endofyear.views.stories.StoryYearOverYearView
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryCompletionRate
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryEpilogue
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryIntro
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedCategories
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedNumbers
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListeningTime
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryLongestEpisode
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopFivePodcasts
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopListenedCategories
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopPodcast
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryYearOverYear
import au.com.shiftyjelly.pocketcasts.repositories.subscription.FreeTrial
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import com.mxalbert.sharedelements.SharedElementsRoot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private val StoryViewCornerSize = 10.dp
private val StoriesViewMaxSize = 700.dp
private const val MaxHeightPercentFactor = 0.9f
private const val LongPressThresholdTimeInMs = 250
private val StoriesViewBlurRadius = 50.dp
const val StoriesViewAspectRatioForTablet = 2f

@Composable
fun StoriesPage(
    modifier: Modifier = Modifier,
    viewModel: StoriesViewModel,
    onCloseClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    onUpsellClicked: () -> Unit,
) {
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        /* Share activity dismissed, start paused story */
        if (it.resultCode == Activity.RESULT_OK) {
            viewModel.trackStoryShared()
        }
        viewModel.start()
    }

    val context = LocalContext.current

    val state: State by viewModel.state.collectAsState()
    val dialogSize = remember { getDialogSize(context) }
    Box(modifier = modifier.size(dialogSize)) {
        when (state) {
            is State.Loaded -> {
                viewModel.trackStoryOrUpsellShown()
                StoriesView(
                    state = state as State.Loaded,
                    progress = viewModel.progress,
                    shouldShowUpsell = { viewModel.shouldShowUpsell() },
                    onSkipPrevious = { viewModel.skipPrevious() },
                    onSkipNext = { viewModel.skipNext() },
                    onPause = { viewModel.pause() },
                    onStart = { viewModel.start() },
                    onCloseClicked = onCloseClicked,
                    onReplayClicked = { viewModel.replay() },
                    onShareClicked = {
                        val currentStory = requireNotNull((state as State.Loaded).currentStory)
                        viewModel.onShareClicked(it, context) { file, shareTextData ->
                            showShareForFile(
                                context,
                                file,
                                currentStory.identifier,
                                shareLauncher,
                                shareTextData
                            )
                        }
                    },
                    onUpsellClicked = onUpsellClicked,
                )
            }
            is State.Loading -> StoriesLoadingView(
                progress = (state as State.Loading).progress,
                onCloseClicked = onCloseClicked
            )
            State.Error -> {
                viewModel.trackStoryFailedToLoad()
                StoriesErrorView(onCloseClicked, onRetryClicked)
            }
        }
    }
}

@Composable
private fun StoriesView(
    state: State.Loaded,
    progress: StateFlow<Float>,
    shouldShowUpsell: () -> Boolean,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onPause: () -> Unit,
    onStart: () -> Unit,
    onCloseClicked: () -> Unit,
    onShareClicked: (() -> Bitmap) -> Unit,
    onReplayClicked: () -> Unit,
    onUpsellClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(color = Color.Black)) {
        state.currentStory?.let { story ->
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(StoryViewCornerSize))
            ) {
                val onCaptureBitmap =
                    convertibleToBitmap(content = {
                        StorySharableContent(
                            story,
                            state.paused,
                            state.userTier,
                            state.freeTrial,
                            shouldShowUpsell,
                            onSkipPrevious,
                            onSkipNext,
                            onPause,
                            onStart,
                            onReplayClicked,
                            onUpsellClicked,
                            modifier
                        )
                    })
                Column {
                    SegmentedProgressIndicator(
                        progressFlow = progress,
                        segmentsData = state.segmentsData,
                        modifier = modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                    )

                    RowCloseButton(onCloseClicked)

                    Spacer(modifier = Modifier.weight(1f))

                    if (state.currentStory.shareable && !shouldShowUpsell()) {
                        ShareButton(
                            onClick = { onShareClicked.invoke(onCaptureBitmap) },
                        )
                    }
                }
                if (state.preparingShareText) {
                    LoadingOverContentView()
                }
            }
        }
    }
}

@Composable
private fun LoadingOverContentView() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun StorySharableContent(
    story: Story,
    paused: Boolean,
    userTier: UserTier,
    freeTrial: FreeTrial,
    shouldShowUpsell: () -> Boolean,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onPause: () -> Unit,
    onStart: () -> Unit,
    onReplayClicked: () -> Unit,
    onUpsellClicked: () -> Unit,
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
                .background(color = story.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            val storyModifier = modifier.then(
                if (userTier == UserTier.Free && story.plusOnly) {
                    Modifier.blur(StoriesViewBlurRadius)
                } else {
                    Modifier
                }
            )
            SharedElementsRoot {
                when (story) {
                    is StoryIntro -> StoryIntroView(storyModifier)
                    is StoryListeningTime -> StoryListeningTimeView(story, storyModifier)
                    is StoryListenedCategories -> StoryListenedCategoriesView(story, storyModifier)
                    is StoryTopListenedCategories -> StoryTopListenedCategoriesView(story, storyModifier)
                    is StoryListenedNumbers -> StoryListenedNumbersView(story, paused, storyModifier)
                    is StoryTopPodcast -> StoryTopPodcastView(story, storyModifier)
                    is StoryTopFivePodcasts -> StoryTopFivePodcastsView(story, storyModifier)
                    is StoryLongestEpisode -> StoryLongestEpisodeView(story, paused, storyModifier)
                    is StoryYearOverYear -> StoryYearOverYearView(
                        story = story,
                        userTier = userTier,
                        modifier = storyModifier,
                    )

                    is StoryCompletionRate -> StoryCompletionRateView(
                        story = story,
                        userTier = userTier,
                        modifier = storyModifier,
                    )

                    is StoryEpilogue -> StoryEpilogueView(
                        story = story,
                        userTier = userTier,
                        onReplayClicked = onReplayClicked,
                        modifier = storyModifier,
                    )
                }
                if (shouldShowUpsell()) {
                    PaidStoryWallView(
                        freeTrial = freeTrial,
                        onUpsellClicked = onUpsellClicked
                    )
                    onPause()
                }
            }
        }
    }
}

@Composable
private fun ShareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RowOutlinedButton(
        text = stringResource(id = LR.string.end_of_year_share_story),
        border = null,
        colors = ButtonDefaults
            .outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White,
            ),
        disableScale = true,
        fontSize = 14.sp,
        fontFamily = FontFamily(listOf(Font(UR.font.dm_sans))),
        textIcon = rememberVectorPainter(Icons.Default.Share),
        onClick = {
            onClick.invoke()
        },
        modifier = modifier

    )
}

@Composable
private fun StoriesLoadingView(
    progress: Float,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StoriesEmptyView(
        content = {
            LinearProgressIndicator(
                progress = progress,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        },
        onCloseClicked = onCloseClicked,
        modifier = modifier
    )
}

@Composable
private fun StoriesErrorView(
    onCloseClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StoriesEmptyView(
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextH30(
                    text = stringResource(id = LR.string.end_of_year_stories_failed),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = modifier.padding(horizontal = 40.dp)
                )
                Button(
                    onClick = { onRetryClicked.invoke() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults
                        .buttonColors(
                            backgroundColor = Color.White,
                        ),
                    modifier = modifier.padding(top = 20.dp),
                ) {
                    TextP40(
                        text = stringResource(id = LR.string.retry),
                        color = Color.Black,
                    )
                }
            }
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
        RowCloseButton(onCloseClicked)
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                screenWidth = it.size.width
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        awaitPointerEventScope {
                            val pressStartTime = System.currentTimeMillis()
                            onPause()
                            val upOrCancel = waitForUpOrCancelInitial()
                            if (upOrCancel == null) {
                                onStart()
                            } else {
                                val pressEndTime = System.currentTimeMillis()
                                val diffPressTime = pressEndTime - pressStartTime
                                if (diffPressTime < LongPressThresholdTimeInMs) {
                                    if (it.x > screenWidth / 2) {
                                        onSkipNext()
                                    } else {
                                        onSkipPrevious()
                                    }
                                } else {
                                    onStart()
                                }
                            }
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
    storyIdentifier: String,
    shareLauncher: ActivityResultLauncher<Intent>,
    shareTextData: ShareTextData,
) {
    try {
        val uri = FileUtil.getUriForFile(context, file)
        var shareText = "${shareTextData.textWithLink} ${shareTextData.hashTags}"
        if (shareTextData.showShortURLAtEnd) {
            shareText += " ${Settings.SERVER_SHORT_URL}"
        }

        val shareIntent = ShareCompat.IntentBuilder(context)
            .setType("image/png")
            .addStream(uri)
            .setText(shareText)
            .setChooserTitle(LR.string.end_of_year_share_via)
            .intent

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, ShareResultReceiver::class.java).apply {
                putExtra(ShareResultReceiver.EXTRA_STORY_ID, storyIdentifier)
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val chooserIntent = Intent.createChooser(shareIntent, null, pendingIntent.intentSender)
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
        dialogHeight =
            (screenHeightInDp * MaxHeightPercentFactor).coerceAtMost(StoriesViewMaxSize.value)
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
                currentStory = StoryListenedNumbers(
                    ListenedNumbers(
                        numberOfEpisodes = 1,
                        numberOfPodcasts = 1
                    ),
                    topPodcasts = emptyList()
                ),
                segmentsData = State.Loaded.SegmentsData(
                    xStartOffsets = listOf(0.0f, 0.28f),
                    widths = listOf(0.25f, 0.75f)
                ),
                userTier = UserTier.Free,
                freeTrial = FreeTrial(
                    subscriptionTier = Subscription.SubscriptionTier.PLUS,
                    exists = false,
                ),
            ),
            progress = MutableStateFlow(0.75f),
            shouldShowUpsell = { false },
            onSkipPrevious = {},
            onSkipNext = {},
            onPause = {},
            onStart = {},
            onCloseClicked = {},
            onShareClicked = {},
            onReplayClicked = {},
            onUpsellClicked = {},
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
            progress = 0.5f,
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
            onCloseClicked = {},
            onRetryClicked = {},
        )
    }
}
