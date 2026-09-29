package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.PagerDotIndicator
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.profile.whatsnew.WhatsNewMessageViewModel.Action
import au.com.shiftyjelly.pocketcasts.profile.whatsnew.WhatsNewMessageViewModel.Page
import au.com.shiftyjelly.pocketcasts.profile.whatsnew.WhatsNewMessageViewModel.PollState
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewContent
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewImage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessageType
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewPoll
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewResearch
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewTargeting
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.AsyncImage
import java.time.Instant
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val HorizontalPadding = 20.dp

@Composable
internal fun WhatsNewMessagePage(
    message: WhatsNewMessage,
    pages: List<Page>,
    poll: PollState?,
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onActionClick: (WhatsNewActionEvent) -> Unit,
    onOptionClick: (String) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi01),
    ) {
        ThemedTopAppBar(
            title = when (message.content) {
                is WhatsNewContent.Pages -> message.title
                is WhatsNewContent.Research -> stringResource(LR.string.whats_new_research_title)
            },
            onNavigationClick = onBackPress,
        )
        if (poll != null) {
            WhatsNewPoll(
                poll = poll,
                bottomInset = bottomInset,
                onOptionClick = onOptionClick,
                onSubmitClick = onSubmitClick,
                modifier = Modifier.weight(1f),
            )
        } else {
            WhatsNewPages(
                pages = pages,
                bottomInset = bottomInset,
                onActionClick = onActionClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WhatsNewPages(
    pages: List<Page>,
    bottomInset: Dp,
    onActionClick: (WhatsNewActionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column(
        modifier = modifier.padding(bottom = bottomInset),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { index ->
            WhatsNewPageContent(
                page = pages[index],
                onActionClick = onActionClick,
            )
        }
        if (pages.size > 1) {
            val progress = stringResource(
                LR.string.whats_new_message_page_progress,
                pagerState.currentPage + 1,
                pages.size,
            )
            PagerDotIndicator(
                state = pagerState,
                activeDotColor = MaterialTheme.theme.colors.primaryText01,
                inactiveDotColor = MaterialTheme.theme.colors.primaryIcon02,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 20.dp)
                    .clearAndSetSemantics {
                        contentDescription = progress
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
    }
}

@Composable
private fun WhatsNewPageContent(
    page: Page,
    onActionClick: (WhatsNewActionEvent) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val pageHeight = maxHeight
        val contentWidth = maxWidth - HorizontalPadding * 2
        var hasImageFailed by remember(page.image?.url) { mutableStateOf(false) }
        val showsImage = page.image != null && !hasImageFailed
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HorizontalPadding)
                    .padding(bottom = 24.dp),
            ) {
                if (page.image != null && showsImage) {
                    WhatsNewPageImage(
                        image = page.image,
                        contentWidth = contentWidth,
                        pageHeight = pageHeight,
                        onError = { hasImageFailed = true },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 32.dp),
                    )
                }
                WhatsNewMessageText(
                    heading = page.heading,
                    description = page.description,
                    modifier = Modifier.padding(top = if (showsImage) 40.dp else 32.dp),
                )
            }
            if (page.action != null) {
                WhatsNewPageAction(
                    action = page.action,
                    onClick = { onActionClick(page.action.event) },
                )
            }
        }
    }
}

@Composable
internal fun WhatsNewMessageLoadingPage(
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi01),
    ) {
        ThemedTopAppBar(
            onNavigationClick = onBackPress,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.theme.colors.primaryInteractive01,
            )
        }
    }
}

@Composable
private fun WhatsNewPageImage(
    image: WhatsNewImage,
    contentWidth: Dp,
    pageHeight: Dp,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var loadedAspectRatio by remember(image.url) { mutableStateOf<Float?>(null) }
    val aspectRatio = image.aspectRatio ?: loadedAspectRatio
    val sizeModifier = if (aspectRatio != null) {
        Modifier.size(WhatsNewImageLayout.size(aspectRatio, contentWidth, pageHeight))
    } else {
        Modifier
            .fillMaxWidth()
            .heightIn(max = WhatsNewImageLayout.maximumHeight(pageHeight))
    }
    AsyncImage(
        model = image.url,
        contentDescription = image.alt,
        contentScale = ContentScale.Fit,
        onSuccess = { state ->
            val size = state.painter.intrinsicSize
            if (size.width > 0f && size.height > 0f) {
                loadedAspectRatio = size.width / size.height
            }
        },
        onError = { onError() },
        modifier = modifier
            .then(sizeModifier)
            .clip(RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun WhatsNewPageAction(
    action: Action,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.theme.colors.primaryUi01)
            .padding(16.dp),
    ) {
        RowButton(
            text = action.label,
            onClick = onClick,
            includePadding = false,
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
            textVerticalPadding = 9.dp,
        )
    }
}

@Composable
private fun WhatsNewPoll(
    poll: PollState,
    bottomInset: Dp,
    onOptionClick: (String) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(bottom = bottomInset),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HorizontalPadding)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            TextH20(
                text = poll.research.poll.question,
                lineHeight = 28.sp,
                modifier = Modifier.semantics { heading() },
            )
            val description = poll.research.description
            if (description != null) {
                TextP40(
                    text = description,
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .selectableGroup()
                    .padding(top = 24.dp),
            ) {
                poll.research.poll.options.forEach { option ->
                    WhatsNewPollOption(
                        label = option.label,
                        isSelected = option.id == poll.selectedOptionId,
                        isEnabled = !poll.hasResponded,
                        onClick = { onOptionClick(option.id) },
                    )
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.theme.colors.primaryUi01)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(16.dp),
        ) {
            if (poll.hasResponded) {
                TextP40(
                    text = stringResource(LR.string.whats_new_poll_answered),
                    color = MaterialTheme.theme.colors.primaryText02,
                    textAlign = TextAlign.Center,
                )
            } else {
                RowButton(
                    text = stringResource(LR.string.navigation_continue),
                    onClick = onSubmitClick,
                    enabled = poll.canSubmit,
                    includePadding = false,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W600,
                    textVerticalPadding = 9.dp,
                )
            }
        }
    }
}

@Composable
private fun WhatsNewPollOption(
    label: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.theme.colors
    val shape = RoundedCornerShape(8.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .alpha(if (isEnabled || isSelected) 1f else 0.6f)
            .clip(shape)
            .background(colors.primaryUi01Active)
            .then(if (isSelected) Modifier.border(2.dp, colors.primaryField03Active, shape) else Modifier)
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .background(colors.primaryField03Active, CircleShape),
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_check),
                    contentDescription = null,
                    tint = colors.primaryInteractive02,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(1.5.dp, colors.primaryIcon02, CircleShape),
            )
        }
        TextH30(
            text = label,
        )
    }
}

@Composable
private fun WhatsNewMessageText(
    heading: String,
    description: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        TextH30(
            text = heading,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            modifier = Modifier.semantics { heading() },
        )
        if (description != null) {
            TextP40(
                text = description,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Preview
@Composable
private fun WhatsNewMessagePagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        WhatsNewMessagePage(
            message = previewMessage(
                type = WhatsNewMessageType.NewFeature,
                content = WhatsNewContent.Pages(emptyList()),
            ),
            pages = listOf(
                Page(
                    image = null,
                    heading = "You already trust the name",
                    description = "Browse networks in Discover to see every podcast they make.",
                    action = Action(label = "Open Discover", event = WhatsNewActionEvent.OpenDiscover),
                ),
                Page(
                    image = null,
                    heading = "Follow a whole network",
                    description = "Tap a network to see everything it publishes.",
                    action = null,
                ),
            ),
            poll = null,
            bottomInset = 0.dp,
            onBackPress = {},
            onActionClick = {},
            onOptionClick = {},
            onSubmitClick = {},
        )
    }
}

@Preview
@Composable
private fun WhatsNewMessagePageResearchPreview() {
    val research = WhatsNewResearch(
        description = "One question, ten seconds.",
        poll = WhatsNewPoll(
            pollId = "poll",
            pollKey = "poll",
            question = "How do you use Up Next?",
            options = listOf(
                WhatsNewPoll.Option(id = "a", pollOptionKey = "a", label = "As a queue for what's next"),
                WhatsNewPoll.Option(id = "b", pollOptionKey = "b", label = "As a long-term playlist"),
            ),
        ),
    )
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        WhatsNewMessagePage(
            message = previewMessage(
                type = WhatsNewMessageType.Research,
                content = WhatsNewContent.Research(research),
            ),
            pages = emptyList(),
            poll = PollState(research = research, selectedOptionId = "a", hasResponded = false),
            bottomInset = 0.dp,
            onBackPress = {},
            onActionClick = {},
            onOptionClick = {},
            onSubmitClick = {},
        )
    }
}

@Preview
@Composable
private fun WhatsNewMessageLoadingPagePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        WhatsNewMessageLoadingPage(
            onBackPress = {},
        )
    }
}

private fun previewMessage(type: WhatsNewMessageType, content: WhatsNewContent) = WhatsNewMessage(
    id = "preview",
    type = type,
    publishedAt = Instant.now(),
    expiresAt = null,
    targeting = WhatsNewTargeting(audiences = emptyList(), minimumAppVersion = null),
    title = "Browse by network",
    content = content,
)
