package au.com.shiftyjelly.pocketcasts.podcasts.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.AppCompatTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.components.Banner
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListViewModel.State
import au.com.shiftyjelly.pocketcasts.views.compose.ToolbarButton
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class ProfileEpisodeListListener(
    val onNavigationClick: () -> Unit,
    val onMoreOptionsClick: () -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onCreateFreeAccountClick: () -> Unit,
    val onDismissFreeAccountBanner: () -> Unit,
    val onManageDownloadsShown: () -> Unit,
    val onManageDownloadsClick: () -> Unit,
    val onDismissManageDownloads: () -> Unit,
    val onDiscoverClick: () -> Unit,
)

@Composable
internal fun ProfileEpisodeListPage(
    title: String,
    state: State,
    searchQuery: String,
    showMenu: Boolean,
    showDiscoverButton: Boolean,
    isFreeAccountBannerVisible: Boolean,
    manageDownloadsSize: Long?,
    bottomInsetPx: Int,
    listResetKey: Int,
    listener: ProfileEpisodeListListener,
    multiSelectTopBar: (@Composable () -> Unit)?,
    episodeRow: @Composable (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Layout(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi01),
        content = {
            Box {
                if (multiSelectTopBar != null) {
                    multiSelectTopBar()
                } else {
                    AppCompatTopAppBar(
                        title = title,
                        navigationButton = NavigationButton.Drawable(IR.drawable.ic_arrow_back, LR.string.back),
                        onNavigationClick = listener.onNavigationClick,
                        iconColor = colors.secondaryIcon01,
                        titleColor = colors.secondaryText01,
                        backgroundColor = colors.secondaryUi01,
                        bottomShadow = false,
                        modifier = toolbarModifier,
                        actions = { tint ->
                            if (showMenu) {
                                MoreOptionsAction(tint, listener.onMoreOptionsClick)
                            }
                        },
                    )
                }
            }
            Column {
                Box(Modifier.fillMaxWidth().background(colors.secondaryUi01)) {
                    if (state.showSearchBar) {
                        SearchBar(
                            text = searchQuery,
                            placeholder = stringResource(LR.string.search),
                            onTextChange = listener.onSearchQueryChange,
                            onSearch = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                if (isFreeAccountBannerVisible) {
                    FreeAccountBanner(listener)
                }
                if (manageDownloadsSize != null) {
                    ManageDownloadsBanner(manageDownloadsSize, listener)
                }
                if (state !is State.Empty) {
                    EpisodeList(state, bottomInsetPx, listResetKey, episodeRow, Modifier.weight(1f))
                }
            }
            if (state is State.Empty) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    NoContentBanner(
                        title = stringResource(state.titleRes),
                        body = stringResource(state.summaryRes),
                        iconResourceId = state.iconRes,
                        primaryButtonText = if (showDiscoverButton) stringResource(LR.string.go_to_discover) else null,
                        onPrimaryButtonClick = listener.onDiscoverClick,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val toolbar = measurables[0].measure(Constraints(maxWidth = width, maxHeight = height))
        val body = measurables[1].measure(Constraints.fixed(width, (height - toolbar.height).coerceAtLeast(0)))
        val empty = measurables.getOrNull(2)?.measure(Constraints(maxWidth = width, maxHeight = height))
        layout(width, height) {
            toolbar.place(0, 0)
            body.place(0, toolbar.height)
            empty?.place(0, toolbar.height + (height - toolbar.height - empty.height) / 2)
        }
    }
}

@Composable
private fun EpisodeList(
    state: State,
    bottomInsetPx: Int,
    listResetKey: Int,
    episodeRow: @Composable (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val topPadding = when {
        state !is State.Loaded -> 8.dp
        state.showSearchBar -> 0.dp
        else -> 16.dp
    }
    key(listResetKey) {
        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(top = topPadding, bottom = with(density) { bottomInsetPx.toDp() }),
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.theme.colors.primaryUi02),
        ) {
            val results = (state as? State.Loaded)?.results.orEmpty()
            items(results, key = { it.uuid }) { episode ->
                episodeRow(episode)
            }
        }
    }
}

@Composable
private fun MoreOptionsAction(tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = 4.dp)
            .fillMaxHeight()
            .width(48.dp),
    ) {
        ToolbarButton(
            contentDescription = stringResource(LR.string.podcasts_menu_more_options),
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .align(TextViewAlignment.Center),
        ) {
            DrawableImage(
                drawableId = IR.drawable.ic_more_vert_black_24dp,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .align(TextViewAlignment.Center),
            )
        }
    }
}

@Composable
private fun FreeAccountBanner(listener: ProfileEpisodeListListener) {
    Banner(
        title = stringResource(LR.string.encourage_account_history_banner_title),
        description = stringResource(LR.string.encourage_account_history_banner_description),
        actionLabel = stringResource(LR.string.encourage_account_banner_action_label),
        icon = painterResource(IR.drawable.ic_filters_clock),
        onActionClick = listener.onCreateFreeAccountClick,
        onDismiss = listener.onDismissFreeAccountBanner,
        modifier = Modifier
            .background(MaterialTheme.theme.colors.primaryUi02)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun ManageDownloadsBanner(totalDownloadSize: Long, listener: ProfileEpisodeListListener) {
    CallOnce {
        listener.onManageDownloadsShown()
    }
    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .background(color = MaterialTheme.theme.colors.primaryUi02)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        ManageDownloadsCard(
            totalDownloadSize = totalDownloadSize,
            onManageDownloadsClick = listener.onManageDownloadsClick,
            onDismissClick = listener.onDismissManageDownloads,
        )
    }
}
