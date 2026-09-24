package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.profile.whatsnew.WhatsNewFeedViewModel.LoadState
import au.com.shiftyjelly.pocketcasts.profile.whatsnew.WhatsNewFeedViewModel.UiState
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessageType
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.time.Instant
import java.time.temporal.ChronoUnit
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun WhatsNewFeedPage(
    state: UiState,
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onMessageClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi02),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_whats_new),
            onNavigationClick = onBackPress,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState, enabled = state.loadState == LoadState.Loaded),
        ) {
            if (state.items.isNotEmpty()) {
                WhatsNewFeedList(
                    items = state.items,
                    bottomInset = bottomInset,
                    onMessageClick = onMessageClick,
                )
            } else {
                WhatsNewFeedUnavailable(
                    loadState = state.loadState,
                    bottomInset = bottomInset,
                    onRetry = onRetry,
                )
            }
            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullRefreshState,
                backgroundColor = MaterialTheme.theme.colors.primaryUi01,
                contentColor = MaterialTheme.theme.colors.primaryInteractive01,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun WhatsNewFeedList(
    items: List<WhatsNewFeedItem>,
    bottomInset: Dp,
    onMessageClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { WhatsNewDateFormatter.create(context) }
    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomInset),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(items = items, key = { _, item -> item.id }) { index, item ->
            WhatsNewFeedRow(
                item = item,
                date = dateFormatter.format(item.publishedAt),
                onClick = { onMessageClick(item.id) },
            )
            if (index < items.lastIndex) {
                HorizontalDivider(startIndent = 16.dp)
            }
        }
    }
}

@Composable
private fun WhatsNewFeedRow(
    item: WhatsNewFeedItem,
    date: String,
    onClick: () -> Unit,
) {
    val unreadDescription = stringResource(LR.string.whats_new_feed_unread)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (item.isUnread) {
                    stateDescription = unreadDescription
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        WhatsNewFeedIcon(
            type = item.type,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextC70(
                    text = stringResource(item.type.labelId),
                    maxLines = 1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.weight(1f),
                )
                TextC70(
                    text = date,
                    isUpperCase = false,
                    maxLines = 1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W600,
                )
                if (item.isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.theme.colors.support05),
                    )
                }
            }
            TextH40(
                text = item.title,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun WhatsNewFeedIcon(
    type: WhatsNewMessageType,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.linearGradient(type.gradient)),
    ) {
        Icon(
            painter = painterResource(type.iconId),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun WhatsNewFeedUnavailable(
    loadState: LoadState,
    bottomInset: Dp,
    onRetry: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .padding(bottom = bottomInset),
        ) {
            WhatsNewFeedUnavailableContent(
                loadState = loadState,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun WhatsNewFeedUnavailableContent(
    loadState: LoadState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (loadState) {
        LoadState.Loading -> CircularProgressIndicator(
            color = MaterialTheme.theme.colors.primaryInteractive01,
            modifier = modifier,
        )

        LoadState.Loaded -> NoContentBanner(
            title = stringResource(LR.string.whats_new_feed_empty_title),
            body = stringResource(LR.string.whats_new_feed_empty_description),
            iconResourceId = IR.drawable.ic_mail,
            modifier = modifier,
        )

        LoadState.Failed -> NoContentBanner(
            title = stringResource(LR.string.whats_new_feed_unable_to_load),
            body = stringResource(LR.string.error_check_your_internet_connection),
            iconResourceId = IR.drawable.ic_cloud_off,
            primaryButtonText = stringResource(LR.string.try_again),
            onPrimaryButtonClick = onRetry,
            modifier = modifier,
        )
    }
}

@Preview
@Composable
private fun WhatsNewFeedPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    val now = Instant.now()
    AppThemeWithBackground(themeType) {
        WhatsNewFeedPage(
            state = UiState(
                items = WhatsNewMessageType.entries.mapIndexed { index, type ->
                    WhatsNewFeedItem(
                        id = type.key,
                        type = type,
                        title = "A message about something new in Pocket Casts that wraps onto a second line",
                        publishedAt = now.minus(index * 40L, ChronoUnit.DAYS),
                        isUnread = index < 2,
                    )
                },
                loadState = LoadState.Loaded,
            ),
            bottomInset = 0.dp,
            onBackPress = {},
            onMessageClick = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun WhatsNewFeedPageEmptyPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        WhatsNewFeedPage(
            state = UiState(loadState = LoadState.Loaded),
            bottomInset = 0.dp,
            onBackPress = {},
            onMessageClick = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun WhatsNewFeedPageFailedPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        WhatsNewFeedPage(
            state = UiState(loadState = LoadState.Failed),
            bottomInset = 0.dp,
            onBackPress = {},
            onMessageClick = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}
