package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.chat.ChatHistoryGroup
import au.com.shiftyjelly.pocketcasts.chat.ChatHistorySection
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRow
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowActionDefaults
import au.com.shiftyjelly.pocketcasts.compose.swipe.rememberSwipeRowState
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatHistory(
    sections: List<ChatHistorySection>,
    onClickBack: () -> Unit,
    onClickChat: (EpisodeChatHistoryItem) -> Unit,
    onDeleteChat: (EpisodeChatHistoryItem) -> Unit,
    snackbarHostState: SnackbarHostState,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClickBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background),
    ) {
        IconButton(
            onClick = onClickBack,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_arrow_back),
                contentDescription = stringResource(LR.string.back),
                tint = theme.iconButton,
            )
        }
        Text(
            text = stringResource(LR.string.chat_history),
            color = theme.primaryText,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                .semantics { heading() },
        )
        Box(modifier = Modifier.weight(1f)) {
            if (sections.isEmpty()) {
                Text(
                    text = stringResource(LR.string.chat_history_empty),
                    color = theme.tertiaryText,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                sections.forEach { section ->
                    item(key = section.group.name) {
                        ChatHistorySectionTitle(
                            group = section.group,
                            theme = theme,
                        )
                    }
                    items(section.items, key = { it.episodeUuid }) { item ->
                        ChatHistoryRow(
                            item = item,
                            onClick = { onClickChat(item) },
                            onDelete = { onDeleteChat(item) },
                            theme = theme,
                        )
                    }
                }
            }
            ChatSnackbarHost(
                hostState = snackbarHostState,
                theme = theme,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ChatHistorySectionTitle(
    group: ChatHistoryGroup,
    theme: ChatTheme,
) {
    Text(
        text = stringResource(
            when (group) {
                ChatHistoryGroup.Today -> LR.string.today
                ChatHistoryGroup.Yesterday -> LR.string.yesterday
                ChatHistoryGroup.PreviousSevenDays -> LR.string.chat_history_previous_7_days
                ChatHistoryGroup.Older -> LR.string.chat_history_older
            },
        ),
        color = theme.tertiaryText,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun ChatHistoryRow(
    item: EpisodeChatHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    theme: ChatTheme,
) {
    SwipeRow(
        state = rememberSwipeRowState(item.episodeUuid),
        trailingAction = SwipeRowActionDefaults.delete(
            isFullSwipeEnabled = true,
            onClick = { onDelete() },
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.background)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 16.dp, end = 16.dp)
                .height(64.dp),
        ) {
            PodcastImage(
                uuid = item.podcastUuid,
                imageSize = 44.dp,
                cornerSize = 4.dp,
                elevation = null,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.episodeTitle,
                    color = theme.primaryText,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.podcastTitle,
                    color = theme.tertiaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(IR.drawable.ic_chevron_small_right),
                contentDescription = null,
                tint = theme.tertiaryText,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview
@Composable
private fun ChatHistoryEmptyPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        ChatHistory(
            sections = emptyList(),
            onClickBack = {},
            onClickChat = {},
            onDeleteChat = {},
            snackbarHostState = remember { SnackbarHostState() },
            theme = rememberChatTheme(),
        )
    }
}

@Preview
@Composable
private fun ChatHistoryPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(Theme.ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            ChatHistory(
                sections = listOf(
                    ChatHistorySection(
                        group = ChatHistoryGroup.Today,
                        items = listOf(previewItem("1", "Navigating Life Insurance Choices", "Smart Money")),
                    ),
                    ChatHistorySection(
                        group = ChatHistoryGroup.PreviousSevenDays,
                        items = listOf(
                            previewItem("2", "Lost in the Terminal", "99% Invisible"),
                            previewItem("3", "The Fight Over Phones in School", "The Daily"),
                        ),
                    ),
                ),
                onClickBack = {},
                onClickChat = {},
                onDeleteChat = {},
                snackbarHostState = remember { SnackbarHostState() },
                theme = rememberChatTheme(),
            )
        }
    }
}

private fun previewItem(uuid: String, episodeTitle: String, podcastTitle: String) = EpisodeChatHistoryItem(
    episodeUuid = uuid,
    episodeTitle = episodeTitle,
    podcastUuid = "podcast-$uuid",
    podcastTitle = podcastTitle,
    episodeDuration = 0.0,
    lastMessageAt = Date(),
)
