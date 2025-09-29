package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import android.content.res.Configuration
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingRecommendationsStartPageViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingRecommendationsStartPageViewModel.Podcast
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingRecommendationsStartPageViewModel.Section
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingRecommendationsStartPageViewModel.SectionId
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.bars.singleAuto
import au.com.shiftyjelly.pocketcasts.compose.bars.transparent
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBarButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.textH60FontSize
import au.com.shiftyjelly.pocketcasts.compose.extensions.header
import au.com.shiftyjelly.pocketcasts.compose.podcast.PodcastSubscribeImage
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingRecommendationsStartPage(
    theme: Theme.ThemeType,
    onImportClick: () -> Unit,
    onSearch: () -> Unit,
    onBackPress: () -> Unit,
    onComplete: () -> Unit,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingRecommendationsStartPageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val pocketCastsTheme = MaterialTheme.theme

    CallOnce {
        viewModel.onShown()
    }

    LaunchedEffect(onUpdateSystemBars) {
        val statusBar = SystemBarStyle.singleAuto(pocketCastsTheme.colors.primaryUi01.copy(alpha = 0.9f)) { theme.darkTheme }
        val navigationBar = SystemBarStyle.transparent { theme.darkTheme }
        onUpdateSystemBars(SystemBarsStyles(statusBar, navigationBar))
    }
    BackHandler {
        viewModel.onBackPressed()
        onBackPress()
    }

    Content(
        state = state,
        buttonRes = state.buttonRes,
        onImportClick = {
            viewModel.onImportClick()
            onImportClick()
        },
        onSubscribeClick = viewModel::updateSubscribed,
        onSearch = {
            viewModel.onSearch()
            onSearch()
        },
        onComplete = {
            viewModel.onComplete()
            onComplete()
        },
        modifier = modifier,
        importColor = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS)) {
            MaterialTheme.theme.colors.primaryInteractive01
        } else {
            MaterialTheme.theme.colors.primaryText01
        },
        title = stringResource(
            if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS)) {
                LR.string.onboarding_recommendations_title
            } else {
                LR.string.onboarding_recommendations_find_favorite_podcasts
            },
        ),
        message = stringResource(
            if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS)) {
                LR.string.onboarding_recommendations_message
            } else {
                LR.string.onboarding_recommendations_make_pocket_casts_yours
            },
        ),
        showSectionLoadMore = !FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS),
    )
}

@Composable
private fun Content(
    state: OnboardingRecommendationsStartPageViewModel.State,
    buttonRes: Int,
    onImportClick: () -> Unit,
    onSubscribeClick: (Podcast) -> Unit,
    onSearch: () -> Unit,
    onComplete: () -> Unit,
    title: String,
    message: String,
    importColor: Color,
    showSectionLoadMore: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        val numToShowDefault = OnboardingRecommendationsStartPageViewModel.NUM_TO_SHOW_DEFAULT
        val numColumns = when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> numToShowDefault
            else -> numToShowDefault / 2
        }

        val size = with(LocalDensity.current) {
            64.dp.toPx()
        }
        val gridState = rememberLazyGridState()
        val backgroundColor = MaterialTheme.colors.background
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(numColumns),
            contentPadding = PaddingValues(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier
                .systemBarsPadding()
                .weight(1f)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    val height = gridState.layoutInfo.viewportSize.height
                    val endOffset = height - size
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.15f to Color.Transparent,
                            1f to backgroundColor,
                            startY = endOffset,
                            endY = Float.POSITIVE_INFINITY,
                        ),
                        size = Size(height = size, width = gridState.layoutInfo.viewportSize.width.toFloat()),
                        topLeft = Offset(x = 0f, y = endOffset),
                    )
                },
        ) {
            header {
                Column {
                    TextP40(
                        text = stringResource(LR.string.onboarding_recommendations_import),
                        color = importColor,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier
                            .align(alignment = Alignment.End)
                            .padding(
                                top = 11.dp,
                            )
                            .clickable { onImportClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TextH10(
                        text = title,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS)) TextAlign.Center else null,
                    )

                    TextP40(
                        text = message,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS)) TextAlign.Center else null,
                        color = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS)) MaterialTheme.theme.colors.primaryText02 else MaterialTheme.theme.colors.primaryText01,
                        fontWeight = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS)) FontWeight.W500 else null,
                    )

                    SearchBarButton(
                        text = stringResource(LR.string.search),
                        onClick = onSearch,
                        modifier = Modifier.padding(bottom = 25.dp),
                    )
                }
            }

            state.sections.forEach { section ->
                section(
                    section = section,
                    onSubscribeClick = onSubscribeClick,
                    showLoadMore = showSectionLoadMore,
                )
            }

            if (state.showLoadingSpinner) {
                header {
                    Row(horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(
                            Modifier
                                .progressSemantics()
                                .size(48.dp),
                        )
                    }
                }
            }
        }

        RowButton(
            text = stringResource(buttonRes),
            onClick = onComplete,
            includePadding = false,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        )
    }
}

private fun LazyGridScope.section(
    section: Section,
    onSubscribeClick: (Podcast) -> Unit,
    showLoadMore: Boolean = true,
) {
    if (section.visiblePodcasts.isEmpty()) return

    header {
        TextH20(
            text = section.title,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    items(items = section.visiblePodcasts) {
        // Simulate minLines = 2 since we can't do that directly
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
                onSubscribeClick = { onSubscribeClick(it) },
            )

            Spacer(Modifier.height(8.dp))

            TextH60(
                text = it.title,
                maxLines = 2,
                modifier = Modifier
                    .heightIn(min = twoLines)
                    .clearAndSetSemantics {},
            )
        }
    }

    header {
        if (showLoadMore) {
            RowOutlinedButton(
                text = stringResource(LR.string.onboarding_recommendations_more, section.title),
                includePadding = false,
                onClick = section::onShowMore,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        } else {
            Spacer(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    fun podcast(isSubscribed: Boolean = false) = Podcast(
        uuid = "5168e260-372e-013b-efad-0acc26574db2",
        title = "Why Do We Do That?",
        isSubscribed = isSubscribed,
    )

    AppThemeWithBackground(themeType) {
        Content(
            state = OnboardingRecommendationsStartPageViewModel.State(
                sections = listOf(
                    Section(
                        title = "A Very Special Section",
                        sectionId = SectionId(""),
                        numToShow = 6,
                        podcasts = listOf(
                            podcast(),
                            podcast(isSubscribed = true),
                            podcast(),
                            podcast(),
                            podcast(),
                            podcast(),
                            podcast(),
                        ),
                        onShowMoreFun = {},
                    ),
                ),
                showLoadingSpinner = true,
            ),
            buttonRes = LR.string.not_now,
            onImportClick = {},
            onSubscribeClick = {},
            onSearch = {},
            onComplete = {},
            title = "Screen title",
            message = "Screen message",
            importColor = MaterialTheme.theme.colors.primaryText01,
            showSectionLoadMore = true,
        )
    }
}

@Preview
@Composable
private fun PreviewNewOnboarding(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    fun podcast(isSubscribed: Boolean = false) = Podcast(
        uuid = "5168e260-372e-013b-efad-0acc26574db2",
        title = "Why Do We Do That?",
        isSubscribed = isSubscribed,
    )

    AppThemeWithBackground(themeType) {
        Content(
            state = OnboardingRecommendationsStartPageViewModel.State(
                sections = listOf(
                    Section(
                        title = "A Very Special Section",
                        sectionId = SectionId(""),
                        numToShow = 6,
                        podcasts = listOf(
                            podcast(),
                            podcast(isSubscribed = true),
                            podcast(),
                            podcast(),
                            podcast(),
                            podcast(),
                            podcast(),
                        ),
                        onShowMoreFun = {},
                    ),
                ),
                showLoadingSpinner = true,
            ),
            buttonRes = LR.string.navigation_continue,
            onImportClick = {},
            onSubscribeClick = {},
            onSearch = {},
            onComplete = {},
            title = stringResource(LR.string.onboarding_recommendations_title),
            message = stringResource(LR.string.onboarding_recommendations_message),
            importColor = MaterialTheme.theme.colors.primaryInteractive01,
            showSectionLoadMore = false,
        )
    }
}
