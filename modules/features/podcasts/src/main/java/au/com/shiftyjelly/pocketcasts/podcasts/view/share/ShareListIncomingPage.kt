package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.widget.ProgressBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.compose.bars.AppCompatTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ShareListIncomingPage(
    share: ShareState?,
    subscribedUuids: Set<String>,
    bottomInset: PaddingValues,
    listener: ShareListIncomingListener,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi01),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.theme.colors.secondaryUi01),
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            EnterAlwaysToolbarLayout(
                toolbar = {
                    AppCompatTopAppBar(
                        title = stringResource(LR.string.podcasts_share_list),
                        navigationButton = NavigationButton.Drawable(IR.drawable.ic_cancel, LR.string.close),
                        onNavigationClick = listener::onCloseClick,
                        iconColor = MaterialTheme.theme.colors.secondaryIcon01,
                        titleColor = MaterialTheme.theme.colors.secondaryText01,
                        backgroundColor = MaterialTheme.theme.colors.secondaryUi01,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = toolbarModifier,
                    )
                },
                content = {
                    val loaded = share as? ShareState.Loaded
                    ShareListIncomingList(
                        title = loaded?.title,
                        description = loaded?.description,
                        podcasts = loaded?.podcasts,
                        subscribedUuids = subscribedUuids,
                        bottomInset = bottomInset,
                        listener = listener,
                    )
                },
            )
            if (share == null || share is ShareState.Loading) {
                AndroidView(
                    factory = { context -> ProgressBar(context, null, android.R.attr.progressBarStyleLarge) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun EnterAlwaysToolbarLayout(
    toolbar: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var toolbarHeight by remember { mutableFloatStateOf(0f) }
    var toolbarOffset by remember { mutableFloatStateOf(0f) }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val previous = toolbarOffset
                toolbarOffset = (previous + available.y).coerceIn(-toolbarHeight, 0f)
                return Offset(0f, toolbarOffset - previous)
            }
        }
    }
    Layout(
        contents = listOf(content, toolbar),
        modifier = Modifier.fillMaxSize().nestedScroll(connection),
    ) { (contentMeasurables, toolbarMeasurables), constraints ->
        val toolbarPlaceable = toolbarMeasurables.first().measure(constraints.copy(minHeight = 0))
        toolbarHeight = toolbarPlaceable.height.toFloat()
        val contentPlaceable = contentMeasurables.first().measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
        val offset = toolbarOffset.roundToInt()
        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceable.place(0, toolbarPlaceable.height + offset)
            toolbarPlaceable.place(0, offset)
        }
    }
}

@Composable
private fun ShareListIncomingList(
    title: String?,
    description: String?,
    podcasts: List<Podcast>?,
    subscribedUuids: Set<String>,
    bottomInset: PaddingValues,
    listener: ShareListIncomingListener,
) {
    val optimisticallySubscribed = remember(subscribedUuids) { mutableStateListOf<String>() }
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomInset.calculateBottomPadding()),
        modifier = Modifier.fillMaxSize(),
    ) {
        val hasPodcasts = !podcasts.isNullOrEmpty()
        item(key = "header") {
            if (hasPodcasts) {
                ShareListIncomingHeader(title = title, description = description)
            }
        }
        items(podcasts.orEmpty()) { podcast ->
            ShareListIncomingPodcastRow(
                podcast = podcast,
                isSubscribed = podcast.uuid in subscribedUuids || podcast.uuid in optimisticallySubscribed,
                onClick = { listener.onPodcastClick(podcast) },
                onSubscribeClick = {
                    optimisticallySubscribed += podcast.uuid
                    listener.onSubscribeClick(podcast)
                },
                onUnsubscribeClick = { listener.onUnsubscribeClick(podcast) },
            )
        }
        item(key = "footer") {
            ShareListIncomingFooter(
                podcasts = podcasts,
                isSubscribedToAll = podcasts?.all { it.uuid in subscribedUuids } ?: true,
                onSubscribeToAllClick = listener::onSubscribeToAllClick,
            )
        }
    }
}

interface ShareListIncomingListener {
    fun onCloseClick()
    fun onPodcastClick(podcast: Podcast)
    fun onSubscribeClick(podcast: Podcast)
    fun onUnsubscribeClick(podcast: Podcast)
    fun onSubscribeToAllClick(podcasts: List<Podcast>)
}
