package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import au.com.shiftyjelly.pocketcasts.compose.ad.AdBanner
import au.com.shiftyjelly.pocketcasts.compose.ad.rememberAdColors
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.views.compose.AccelerateDecelerate
import au.com.shiftyjelly.pocketcasts.views.compose.gridDragGestures
import au.com.shiftyjelly.pocketcasts.views.compose.rememberGridDragState
import au.com.shiftyjelly.pocketcasts.views.compose.rememberLockedReorder
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

private const val AD_KEY = "banner_ad"

internal class PodcastsPageListener(
    val onNavigationClick: () -> Unit,
    val onFoldersLockedClick: () -> Unit,
    val onCreateFolderClick: () -> Unit,
    val onSearchClick: () -> Unit,
    val onOptionsClick: () -> Unit,
    val onRefresh: () -> Unit,
    val onPodcastClick: (Podcast) -> Unit,
    val onFolderClick: (Folder) -> Unit,
    val onAdClick: (BlazeAd) -> Unit,
    val onAdOptionsClick: (BlazeAd) -> Unit,
    val onAdImpression: (BlazeAd) -> Unit,
    val onReorder: suspend (List<FolderItem>) -> Unit,
)

internal class PodcastsToolbarState(
    val title: String,
    val colors: ToolbarColors,
    val showBack: Boolean,
    val showFoldersLocked: Boolean,
    val showCreateFolder: Boolean,
    val showSearch: Boolean,
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun PodcastsPage(
    toolbar: PodcastsToolbarState,
    content: PodcastsContent,
    showContent: Boolean,
    showEmpty: Boolean,
    isRefreshing: Boolean,
    gridState: LazyGridState,
    bottomInsetPx: Int,
    imageRequestFactory: PocketCastsImageRequestFactory,
    listener: PodcastsPageListener,
    castButton: @Composable (Color) -> Unit,
    emptyContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PodcastsToolbar(toolbar, listener, castButton, toolbarModifier)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (showEmpty) {
                emptyContent()
            }
            if (showContent) {
                val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = listener.onRefresh)
                Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
                    PodcastsGrid(content, gridState, bottomInsetPx, imageRequestFactory, listener)
                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastsToolbar(
    toolbar: PodcastsToolbarState,
    listener: PodcastsPageListener,
    castButton: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    ThemedTopAppBar(
        title = toolbar.title,
        navigationButton = if (toolbar.showBack) NavigationButton.Back else null,
        onNavigationClick = listener.onNavigationClick,
        iconColor = toolbar.colors.iconComposeColor,
        textColor = toolbar.colors.titleComposeColor,
        backgroundColor = toolbar.colors.backgroundComposeColor,
        windowInsets = WindowInsets.statusBars,
        modifier = modifier,
        actions = { tint ->
            if (toolbar.showFoldersLocked) {
                ToolbarAction(IR.drawable.ic_folder_plus, LR.string.create_folder, Color.Unspecified, listener.onFoldersLockedClick)
            }
            if (toolbar.showCreateFolder) {
                ToolbarAction(IR.drawable.ic_folder_plus, LR.string.create_folder, tint, listener.onCreateFolderClick)
            }
            castButton(tint)
            if (toolbar.showSearch) {
                ToolbarAction(VR.drawable.ic_search_black_24dp, LR.string.podcasts_menu_search_podcasts, tint, listener.onSearchClick)
            }
            ToolbarAction(IR.drawable.ic_more_vert_black_24dp, LR.string.podcasts_menu_more_options, tint, listener.onOptionsClick)
        },
    )
}

@Composable
private fun ToolbarAction(iconId: Int, contentDescriptionId: Int, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = stringResource(contentDescriptionId),
            tint = tint,
        )
    }
}

@Composable
private fun PodcastsGrid(
    content: PodcastsContent,
    gridState: LazyGridState,
    bottomInsetPx: Int,
    imageRequestFactory: PocketCastsImageRequestFactory,
    listener: PodcastsPageListener,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isList = content.layout == PodcastGridLayoutType.LIST_VIEW
    val spanCount = remember(content.layout, configuration) {
        when (content.layout) {
            PodcastGridLayoutType.LARGE_ARTWORK -> UiUtil.getGridColumnCount(false, context)
            PodcastGridLayoutType.SMALL_ARTWORK -> UiUtil.getGridColumnCount(true, context)
            PodcastGridLayoutType.LIST_VIEW -> 1
        }
    }
    val resources = context.resources
    val outerPadding = resources.getDimensionPixelSize(if (isList) VR.dimen.list_outer_adding else VR.dimen.grid_outer_padding)
    val itemInset = if (isList) 0 else resources.getDimensionPixelSize(VR.dimen.grid_item_padding)
    val reorder = rememberLockedReorder(content.items, FolderItem::uuid, listener.onReorder)
    val dragState = rememberGridDragState(gridState)
    dragState.itemInset = itemInset
    dragState.startPadding = outerPadding
    reorder.bind(dragState)
    val padding = with(density) { outerPadding.toDp() }
    val inset = with(density) { itemInset.toDp() }
    LazyVerticalGrid(
        columns = GridCells.Fixed(spanCount),
        state = gridState,
        contentPadding = PaddingValues(start = padding, top = padding, end = padding, bottom = with(density) { (outerPadding + bottomInsetPx).toDp() }),
        modifier = Modifier
            .fillMaxSize()
            .gridDragGestures(dragState),
    ) {
        content.activeAd?.let { ad ->
            item(key = AD_KEY, span = { GridItemSpan(maxLineSpan) }) {
                BannerAdItem(ad, listener, Modifier.padding(inset))
            }
        }
        items(reorder.items, key = { it.uuid }) { item ->
            Box(
                modifier = Modifier
                    .zIndex(if (dragState.isLifted(item.uuid)) 1f else 0f)
                    .animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = if (dragState.isActive(item.uuid)) null else MoveSpec)
                    .graphicsLayer {
                        val translation = dragState.translation(item.uuid)
                        translationX = translation.x
                        translationY = translation.y
                    }
                    .padding(inset),
            ) {
                FolderItemCell(item, content, dragState.isLifted(item.uuid), imageRequestFactory, listener.onPodcastClick, listener.onFolderClick)
            }
        }
    }
}

private val MoveSpec = tween<IntOffset>(250, easing = AccelerateDecelerate)

@Composable
private fun BannerAdItem(ad: BlazeAd, listener: PodcastsPageListener, modifier: Modifier = Modifier) {
    LaunchedEffect(ad.id) {
        listener.onAdImpression(ad)
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        AdBanner(
            ad = ad,
            colors = rememberAdColors().bannerAd,
            onAdClick = { listener.onAdClick(ad) },
            onOptionsClick = { listener.onAdOptionsClick(ad) },
        )
    }
}

@Composable
internal fun PodcastsEmptyContent(
    activeAd: BlazeAd?,
    listener: PodcastsPageListener,
    banner: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        activeAd?.let { ad ->
            AdBanner(
                ad = ad,
                colors = rememberAdColors().bannerAd,
                onAdClick = { listener.onAdClick(ad) },
                onOptionsClick = { listener.onAdOptionsClick(ad) },
            )
            LaunchedEffect(ad.id) {
                listener.onAdImpression(ad)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        banner()
        Spacer(modifier = Modifier.weight(2f))
    }
}
