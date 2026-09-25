package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewContent
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessageType
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewPage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewPoll
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewResearch
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewTargeting
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.time.Instant

@Composable
internal fun WhatsNewMessagePage(
    message: WhatsNewMessage,
    bottomInset: Dp,
    onBackPress: () -> Unit,
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
                is WhatsNewContent.Research -> stringResource(message.type.labelId)
            },
            onNavigationClick = onBackPress,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 32.dp, bottom = bottomInset + 24.dp),
        ) {
            when (val content = message.content) {
                is WhatsNewContent.Pages -> {
                    val page = content.pages.first()
                    WhatsNewMessageText(heading = page.heading, description = page.description)
                }

                is WhatsNewContent.Research -> WhatsNewMessageText(
                    heading = content.research.poll.question,
                    description = content.research.description,
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
            modifier = Modifier.semantics { heading() },
        )
        if (description != null) {
            TextP40(
                text = description,
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
                content = WhatsNewContent.Pages(
                    listOf(
                        WhatsNewPage(
                            image = null,
                            heading = "You already trust the name",
                            description = "Browse networks in Discover to see every podcast they make.",
                            action = null,
                        ),
                    ),
                ),
            ),
            bottomInset = 0.dp,
            onBackPress = {},
        )
    }
}

@Preview
@Composable
private fun WhatsNewMessagePageResearchPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        WhatsNewMessagePage(
            message = previewMessage(
                type = WhatsNewMessageType.Research,
                content = WhatsNewContent.Research(
                    WhatsNewResearch(
                        description = "One question, ten seconds.",
                        poll = WhatsNewPoll(
                            pollId = "poll",
                            pollKey = "poll",
                            question = "How do you use Up Next?",
                            options = listOf(WhatsNewPoll.Option(id = "a", pollOptionKey = "a", label = "As a queue")),
                        ),
                    ),
                ),
            ),
            bottomInset = 0.dp,
            onBackPress = {},
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
