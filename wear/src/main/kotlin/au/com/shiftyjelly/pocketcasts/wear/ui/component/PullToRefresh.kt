package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState

/**
 * Pull-to-refresh component for Wear OS that wraps content and shows a circular progress indicator
 * when the user pulls down at the top of the list.
 *
 * Uses the mobile Material 2 pullrefresh API as recommended by Google for Wear OS apps.
 *
 * @param state The refresh state from the ViewModel
 * @param onRefresh Callback to trigger refresh
 * @param modifier Modifier for the component
 * @param content The content to display (typically a ScalingLazyColumn)
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PullToRefresh(
    state: RefreshState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isRefreshing = state is RefreshState.Refreshing
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh,
        refreshThreshold = 40.dp,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState),
    ) {
        content()
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
