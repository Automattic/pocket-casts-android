package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.compose.bars.AppCompatTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.profile.R
import au.com.shiftyjelly.pocketcasts.servers.sync.FileAccount
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.component.GradientIcon
import au.com.shiftyjelly.pocketcasts.views.compose.ToolbarButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private val ToolbarHeight = 56.dp

internal class CloudFilesListener(
    val onNavigationClick: () -> Unit,
    val onOptionsClick: () -> Unit,
    val onAddFileClick: () -> Unit,
    val onRefresh: () -> Unit,
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun CloudFilesPage(
    episodes: List<UserEpisode>?,
    account: FileAccount?,
    isRefreshEnabled: Boolean,
    bottomInsetPx: Int,
    listener: CloudFilesListener,
    multiSelectTopBar: (@Composable () -> Unit)?,
    episodeRow: @Composable (UserEpisode) -> Unit,
    castButton: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi02),
    ) {
        if (multiSelectTopBar != null) {
            multiSelectTopBar()
        } else {
            val buttonModifier = toolbarButtonModifier()
            AppCompatTopAppBar(
                title = stringResource(LR.string.profile_navigation_files),
                navigationButton = NavigationButton.Drawable(IR.drawable.ic_arrow_back, LR.string.back),
                onNavigationClick = listener.onNavigationClick,
                iconColor = colors.secondaryIcon01,
                titleColor = colors.secondaryText01,
                backgroundColor = colors.secondaryUi01,
                bottomShadow = false,
                modifier = toolbarModifier,
                navigationButtonModifier = buttonModifier,
                actions = { tint ->
                    Box(buttonModifier.offset(x = 4.dp).fillMaxHeight()) {
                        castButton(tint)
                    }
                    OptionsAction(tint, listener.onOptionsClick, buttonModifier)
                },
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (account != null) {
                    UsageHeader(account)
                } else {
                    LockedHeader()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(UR.dimen.divider_height))
                        .background(colors.primaryUi05),
                )
                val pullRefreshState = rememberPullRefreshState(refreshing = false, onRefresh = listener.onRefresh)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pullRefresh(pullRefreshState, enabled = isRefreshEnabled),
                ) {
                    FileList(
                        episodes = episodes.orEmpty(),
                        bottomPadding = with(density) { (bottomInsetPx + 88.dp.roundToPx()).toDp() },
                        episodeRow = episodeRow,
                    )
                    PullRefreshIndicator(
                        refreshing = false,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
            if (episodes?.isEmpty() == true) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(TextViewAlignment.Center)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    NoContentBanner(
                        title = stringResource(LR.string.profile_files_empty_title),
                        body = stringResource(LR.string.profile_files_empty_summary),
                        iconResourceId = IR.drawable.ic_file,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            AddFileButton(
                onClick = listener.onAddFileClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = with(density) { (bottomInsetPx + 16.dp.roundToPx()).toDp() }),
            )
        }
    }
}

@Composable
private fun toolbarButtonModifier(): Modifier {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val actionBarSizePx = remember(context, configuration) {
        val value = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.actionBarSize, value, true)) {
            TypedValue.complexToDimensionPixelSize(value.data, context.resources.displayMetrics)
        } else {
            0
        }
    }
    val shiftPx = ((with(density) { ToolbarHeight.roundToPx() } - actionBarSizePx) / 2).coerceAtLeast(0)
    return Modifier.offset { IntOffset(0, -shiftPx) }
}

@Composable
private fun AddFileButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.theme.colors
    val contentDescription = stringResource(LR.string.profile_cloud_add_file)
    val currentOnClick by rememberUpdatedState(onClick)
    val backgroundColor = colors.primaryInteractive01.toArgb()
    val iconColor = colors.primaryInteractive02.toArgb()
    AndroidView(
        factory = { context ->
            FloatingActionButton(context).apply {
                setImageResource(IR.drawable.ic_add_black_24dp)
                setOnClickListener { currentOnClick() }
            }
        },
        update = { button ->
            button.contentDescription = contentDescription
            button.backgroundTintList = ColorStateList.valueOf(backgroundColor)
            button.imageTintList = ColorStateList.valueOf(iconColor)
        },
        modifier = modifier,
    )
}

@Composable
private fun FileList(
    episodes: List<UserEpisode>,
    bottomPadding: Dp,
    episodeRow: @Composable (UserEpisode) -> Unit,
) {
    val listState = rememberLazyListState()
    val firstUuid = episodes.firstOrNull()?.uuid
    val previousFirst = remember { FirstItem(firstUuid) }
    if (firstUuid != previousFirst.uuid) {
        val wasLaidOutAtTop = previousFirst.uuid != null &&
            listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset == 0
        if (wasLaidOutAtTop) {
            listState.requestScrollToItem(0)
        }
        previousFirst.uuid = firstUuid
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = bottomPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(episodes, key = { it.uuid }) { episode ->
            episodeRow(episode)
        }
    }
}

private class FirstItem(var uuid: String?)

@Composable
private fun UsageHeader(account: FileAccount) {
    val context = LocalContext.current
    val colors = MaterialTheme.theme.colors
    val used = Util.formattedBytes(account.usedSize, context = context, minimumBytes = 0)
    val total = Util.formattedBytes(account.totalSize, context = context, minimumBytes = 0)
    val percentage = ((account.usedSize.toDouble() / account.totalSize.toDouble()) * 100f).roundToInt()
    HeaderRow {
        HeaderIcon(IR.drawable.ic_cloud, width = 16.dp, height = 16.dp)
        HeaderText(stringResource(LR.string.profile_cloud_count_files, account.totalFiles), colors.primaryText02)
        Spacer(Modifier.width(8.dp))
        HeaderText("$used / $total", colors.primaryText02, Modifier.weight(1f), TextAlign.End)
        Spacer(Modifier.width(8.dp))
        HeaderText(
            text = if (percentage in 0..100) stringResource(LR.string.profile_cloud_percentage_full, percentage) else "",
            color = when (percentage) {
                in 80..95 -> colors.support08
                in 95..101 -> colors.support05
                else -> colors.primaryText01
            },
        )
    }
}

@Composable
private fun LockedHeader() {
    HeaderRow {
        HeaderIcon(R.drawable.ic_lock, width = 18.dp, height = 24.dp)
        HeaderText(stringResource(LR.string.profile_cloud_no_files_uploaded), MaterialTheme.theme.colors.primaryText02)
    }
}

@Composable
private fun HeaderRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 16.dp),
        content = content,
    )
}

@Composable
private fun HeaderIcon(@DrawableRes drawableId: Int, width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(end = 8.dp),
    ) {
        AndroidView(
            factory = { context ->
                GradientIcon(context).apply { setup(AppCompatResources.getDrawable(context, drawableId)) }
            },
            modifier = Modifier
                .width(width)
                .height(height)
                .align(TextViewAlignment.Center),
        )
    }
}

@Composable
private fun HeaderText(text: String, color: Color, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    Box(
        contentAlignment = TextViewAlignment.CenterStart,
        modifier = modifier.fillMaxHeight(),
    ) {
        TextViewText(
            text = text,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            textAlign = textAlign,
            modifier = if (textAlign == TextAlign.End) Modifier.fillMaxWidth() else Modifier,
        )
    }
}

@Composable
private fun OptionsAction(tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = 4.dp)
            .fillMaxHeight()
            .width(48.dp),
    ) {
        ToolbarButton(
            contentDescription = stringResource(LR.string.profile_files_options),
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
