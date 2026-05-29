package au.com.shiftyjelly.pocketcasts.profile.blogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.profile.blogs.AddBlogViewModel.UiState
import au.com.shiftyjelly.pocketcasts.profile.extensions.displayHref
import au.com.shiftyjelly.pocketcasts.servers.webfeeds.WebFeed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AddBlogPage(
    state: UiState,
    url: String,
    onUrlChange: (url: String) -> Unit,
    onBackPress: () -> Unit,
    onFindFeeds: (url: String) -> Unit,
    onRetry: (url: String) -> Unit,
    onFeedClick: (webFeed: WebFeed) -> Unit,
    onGoToPodcast: (podcastUuid: String) -> Unit,
    onEditUrl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPress)
    val colors = MaterialTheme.theme.colors
    Column(
        modifier = modifier.background(colors.primaryUi02),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.blogs_add_button),
            onNavigationClick = onBackPress,
        )
        AddBlogContent(
            state = state,
            url = url,
            onUrlChange = onUrlChange,
            onFindFeeds = onFindFeeds,
            onRetry = onRetry,
            onFeedClick = onFeedClick,
            onGoToPodcast = onGoToPodcast,
            onEditUrl = onEditUrl,
        )
    }
}

@Composable
private fun AddBlogContent(
    state: UiState,
    url: String,
    onUrlChange: (url: String) -> Unit,
    onFindFeeds: (url: String) -> Unit,
    onRetry: (url: String) -> Unit,
    onFeedClick: (WebFeed) -> Unit,
    onGoToPodcast: (String) -> Unit,
    onEditUrl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val isEnabled = state is UiState.Start || state is UiState.Error
    LaunchedEffect(isEnabled) {
        if (isEnabled) focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .widthIn(max = 320.dp)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BlogUrlField(
            value = url,
            onValueChange = onUrlChange,
            enabled = isEnabled,
            placeholder = stringResource(LR.string.blogs_empty_url_placeholder),
            onKeyboardAction = { onFindFeeds(url) },
            trailingIcon = when (state) {
                is UiState.Loading -> {
                    { LoadingDot() }
                }

                is UiState.Pick, is UiState.Found -> {
                    { DoneDot() }
                }

                else -> null
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .then(if (!isEnabled) Modifier.clickable(onClick = onEditUrl) else Modifier),
        )

        Spacer(Modifier.height(8.dp))

        when (state) {
            is UiState.Start -> FormContent(onContinueClick = { onFindFeeds(url) })

            is UiState.Loading -> LoadingContent()

            is UiState.Pick -> PickContent(feeds = state.feeds, onFeedClick = onFeedClick)

            is UiState.Found -> FoundContent(
                webFeed = state.webFeed,
                podcastUuid = state.podcastUuid,
                episodeCount = state.episodeCount,
                onGoToPodcastClick = { onGoToPodcast(state.podcastUuid) },
            )

            is UiState.Error -> ErrorContent(reason = state.reason, onRetry = { onRetry(url) })
        }
    }
}

@Composable
private fun FormContent(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors

    Column(modifier = modifier) {
        TextP60(
            text = stringResource(LR.string.blogs_add_url_hint),
            color = colors.primaryText02,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
        )

        Spacer(Modifier.height(12.dp))

        RowButton(
            text = stringResource(LR.string.navigation_continue),
            onClick = { onContinueClick() },
            includePadding = false,
            textColor = colors.primaryInteractive02,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colors.primaryInteractive01,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
) {
    TextP60(
        text = stringResource(LR.string.blogs_looking_up_feed),
        color = MaterialTheme.theme.colors.primaryInteractive01,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
    )
}

@Composable
private fun PickContent(
    feeds: List<WebFeed>,
    onFeedClick: (WebFeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    var selectedFeed by remember(feeds) { mutableStateOf(feeds.firstOrNull()) }

    Column(modifier = modifier) {
        TextP60(
            text = stringResource(LR.string.blogs_found_on_this_site, feeds.size),
            color = colors.support02,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
        )

        Spacer(Modifier.height(32.dp))

        TextP60(
            text = stringResource(LR.string.blogs_pick_a_feed),
            color = colors.primaryText02,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp),
        )

        Spacer(Modifier.height(10.dp))

        Column(modifier = Modifier.selectableGroup()) {
            feeds.forEach { feed ->
                FeedChoiceRow(
                    feed = feed,
                    isSelected = feed == selectedFeed,
                    onClick = { selectedFeed = feed },
                )

                Spacer(Modifier.height(10.dp))
            }
        }

        RowButton(
            text = stringResource(LR.string.navigation_continue),
            onClick = { selectedFeed?.let { onFeedClick(it) } },
            enabled = selectedFeed != null,
            includePadding = false,
            textColor = colors.primaryInteractive02,
            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primaryInteractive01),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FeedChoiceRow(
    feed: WebFeed,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    val borderColor = if (isSelected) colors.primaryInteractive01 else colors.primaryField03
    val backgroundColor = if (isSelected) colors.primaryInteractive01.copy(alpha = 0.06f) else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // Title and URL
            Column(modifier = Modifier.weight(1f)) {
                TextP50(
                    text = feed.title,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText01,
                    lineHeight = 18.sp,
                    maxLines = 2,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = feed.displayHref,
                    color = colors.primaryText02,
                    maxLines = 1,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,

                )
            }

            Spacer(Modifier.width(8.dp))

            // Selection circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .then(
                        if (isSelected) {
                            Modifier.background(colors.primaryInteractive01, CircleShape)
                        } else {
                            Modifier.border(2.dp, colors.primaryField03, CircleShape)
                        },
                    ),
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_check_black_24dp),
                        contentDescription = null,
                        tint = colors.primaryUi01,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FoundContent(
    webFeed: WebFeed,
    podcastUuid: String,
    episodeCount: Int,
    onGoToPodcastClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors

    Column(modifier = modifier) {
        TextP60(
            text = stringResource(LR.string.blogs_found_feed_detected),
            color = colors.support02,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
        )

        Spacer(Modifier.height(22.dp))

        FoundFeedCard(
            webFeed = webFeed,
            podcastUuid = podcastUuid,
            episodeCount = episodeCount,
        )

        Spacer(Modifier.height(18.dp))

        RowButton(
            text = stringResource(LR.string.blogs_found_go_to_podcast),
            onClick = onGoToPodcastClick,
            includePadding = false,
            textColor = colors.primaryInteractive02,
            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primaryInteractive01),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FoundFeedCard(
    webFeed: WebFeed,
    podcastUuid: String,
    episodeCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    val greenColor = colors.support02
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, greenColor, RoundedCornerShape(14.dp))
            .background(greenColor.copy(alpha = 0.08f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(16.dp)
                    .background(greenColor, CircleShape),
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_check_black_24dp),
                    contentDescription = null,
                    tint = colors.primaryUi01,
                    modifier = Modifier.size(10.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(LR.string.blogs_found_followed_badge),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = greenColor,
                letterSpacing = 1.1.sp,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            PodcastImage(
                uuid = podcastUuid,
                imageSize = 56.dp,
                cornerSize = 8.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                TextP50(
                    text = webFeed.title,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText01,
                    lineHeight = 18.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                TextP60(
                    text = webFeed.displayHref,
                    color = colors.primaryText02,
                    maxLines = 1,
                )
            }
        }

        if (episodeCount == 0) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(greenColor.copy(alpha = 0.2f)),
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.primaryInteractive01,
                    )
                    Spacer(Modifier.width(12.dp))
                    TextP40(
                        text = stringResource(LR.string.blogs_found_headline),
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText01,
                    )
                }
                TextP40(
                    text = stringResource(LR.string.blogs_found_description),
                    color = colors.primaryText02,
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    reason: AddBlogViewModel.ErrorReason,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    val message = stringResource(
        when (reason) {
            AddBlogViewModel.ErrorReason.NoInternet -> LR.string.blogs_error_no_internet
            AddBlogViewModel.ErrorReason.NoFeedsFound -> LR.string.blogs_error_no_feeds_found
            AddBlogViewModel.ErrorReason.Generic -> LR.string.blogs_error_generic
        },
    )
    Column(modifier = modifier) {
        TextP50(
            text = message,
            color = colors.support05,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        RowButton(
            text = stringResource(LR.string.retry),
            onClick = onRetry,
            includePadding = false,
            textColor = colors.primaryInteractive02,
            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primaryInteractive01),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BlogUrlField(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit = {},
    enabled: Boolean = true,
    placeholder: String? = null,
    onKeyboardAction: () -> Unit = {},
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.theme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(LR.string.blogs_empty_url_label)) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go,
        ),
        keyboardActions = KeyboardActions(
            onGo = { onKeyboardAction() },
        ),
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = colors.primaryText01,
            placeholderColor = colors.primaryText02,
            focusedBorderColor = colors.primaryInteractive01,
            unfocusedBorderColor = colors.primaryField03,
            focusedLabelColor = colors.primaryInteractive01,
            unfocusedLabelColor = colors.primaryText02,
            cursorColor = colors.primaryInteractive01,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun LoadingDot() {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.theme.colors.primaryInteractive01,
    )
}

@Composable
private fun DoneDot() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(22.dp)
            .background(color = MaterialTheme.theme.colors.support02, shape = CircleShape),
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_check_black_24dp),
            contentDescription = null,
            tint = MaterialTheme.theme.colors.primaryUi01,
            modifier = Modifier.size(14.dp),
        )
    }
}

private val previewWebFeed = WebFeed(
    title = "pocketcasts.com",
    href = "https://pocketcasts.com/feed/",
    type = "application/rss+xml",
)

@Preview
@Composable
private fun AddBlogPageStartPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AddBlogPage(
            state = UiState.Start,
            url = "",
            onBackPress = {},
            onFindFeeds = { _ -> },
            onRetry = { _ -> },
            onFeedClick = { _ -> },
            onGoToPodcast = { _ -> },
            onEditUrl = {},
            onUrlChange = {},
        )
    }
}

@Preview
@Composable
private fun AddBlogPageLoadingPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AddBlogPage(
            state = UiState.Loading,
            url = "",
            onBackPress = {},
            onFindFeeds = { _ -> },
            onRetry = { _ -> },
            onFeedClick = { _ -> },
            onGoToPodcast = { _ -> },
            onEditUrl = {},
            onUrlChange = {},
        )
    }
}

@Preview
@Composable
private fun AddBlogPagePickPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AddBlogPage(
            state = UiState.Pick(
                listOf(
                    WebFeed(title = "tonysbologna : Honest. Satirical. Observations » Feed", href = "https://tonysbologna.com/feed/", type = "application/rss+xml"),
                    WebFeed(title = "tonysbologna : Honest. Satirical. Observations » Comments Feed", href = "https://tonysbologna.com/comments/feed/", type = "application/rss+xml"),
                ),
            ),
            url = "",
            onBackPress = {},
            onFindFeeds = { _ -> },
            onRetry = { _ -> },
            onFeedClick = { _ -> },
            onGoToPodcast = { _ -> },
            onEditUrl = {},
            onUrlChange = {},
        )
    }
}

@Preview
@Composable
private fun AddBlogPageFoundEpisodesPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AddBlogPage(
            state = UiState.Found(
                webFeed = previewWebFeed,
                podcastUuid = "00000000-0000-0000-0000-000000000000",
                episodeCount = 1,
            ),
            url = "https://pocketcasts.com",
            onBackPress = {},
            onFindFeeds = { _ -> },
            onRetry = { _ -> },
            onFeedClick = { _ -> },
            onGoToPodcast = { _ -> },
            onEditUrl = {},
            onUrlChange = {},
        )
    }
}

@Preview
@Composable
private fun AddBlogPageFoundNoEpisodesPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AddBlogPage(
            state = UiState.Found(
                webFeed = previewWebFeed,
                podcastUuid = "00000000-0000-0000-0000-000000000000",
                episodeCount = 0,
            ),
            url = "https://pocketcasts.com",
            onBackPress = {},
            onFindFeeds = { _ -> },
            onRetry = { _ -> },
            onFeedClick = { _ -> },
            onGoToPodcast = { _ -> },
            onEditUrl = {},
            onUrlChange = {},
        )
    }
}

@Preview
@Composable
private fun AddBlogPageErrorPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AddBlogPage(
            state = UiState.Error(AddBlogViewModel.ErrorReason.Generic),
            url = "",
            onBackPress = {},
            onFindFeeds = { _ -> },
            onRetry = { _ -> },
            onFeedClick = { _ -> },
            onGoToPodcast = { _ -> },
            onEditUrl = {},
            onUrlChange = {},
        )
    }
}
