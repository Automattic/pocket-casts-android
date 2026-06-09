package au.com.shiftyjelly.pocketcasts.onboarding.signin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.onboarding.welcome.artworkResIds
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.AsyncImage
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val COVER_SIZE_DP = 181
private const val COVER_CORNER_RADIUS_DP = 11
private const val COVER_SPACING_DP = 12
private const val ANIMATION_OFFSET_DP = 200
private const val SCROLL_DURATION_MS = 20_000
private const val MIN_COVERS_IN_ROW = 10

@Composable
fun TvSyncingScreen(
    onSyncComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvSyncingViewModel = hiltViewModel(),
) {
    BackHandler {}

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnSyncComplete by rememberUpdatedState(onSyncComplete)
    LaunchedEffect(uiState.syncComplete) {
        if (uiState.syncComplete) {
            currentOnSyncComplete()
        }
    }

    TvSyncingScreenContent(
        podcastUuids = uiState.podcastUuids,
        modifier = modifier,
    )
}

@Composable
private fun TvSyncingScreenContent(
    podcastUuids: List<String>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Dark),
    ) {
        TvSyncingCoverRow(
            podcastUuids = podcastUuids,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to TvColors.Dark,
                            0.55f to TvColors.Dark,
                            0.75f to TvColors.Dark.copy(alpha = 0.8f),
                            1f to TvColors.Dark.copy(alpha = 0.3f),
                        ),
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                painter = painterResource(IR.drawable.ic_pocket_casts_logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_welcome_back),
                color = Color.White,
                style = TvTextStyles.WelcomeTitle,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_syncing_subtitle),
                color = TvColors.TextSecondary,
                style = TvTextStyles.WelcomeSubtitle,
            )
        }
    }
}

@Composable
private fun TvSyncingCoverRow(
    podcastUuids: List<String>,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "syncing_covers")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SCROLL_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cover_row_offset",
    )

    val offsetPx = with(LocalDensity.current) { ANIMATION_OFFSET_DP.dp.toPx() }

    val coverItems = remember(podcastUuids) {
        buildCoverItems(podcastUuids)
    }

    Box(modifier = modifier.clipToBounds()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(COVER_SPACING_DP.dp),
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .graphicsLayer { translationX = animationProgress * offsetPx },
        ) {
            coverItems.forEach { item ->
                when (item) {
                    is CoverItem.Remote -> AsyncImage(
                        model = item.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(COVER_SIZE_DP.dp)
                            .clip(RoundedCornerShape(COVER_CORNER_RADIUS_DP.dp)),
                    )
                    is CoverItem.Local -> Image(
                        painter = painterResource(item.resId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(COVER_SIZE_DP.dp)
                            .clip(RoundedCornerShape(COVER_CORNER_RADIUS_DP.dp)),
                    )
                }
            }
        }
    }
}

private sealed interface CoverItem {
    data class Remote(val url: String) : CoverItem
    data class Local(val resId: Int) : CoverItem
}

private fun buildCoverItems(podcastUuids: List<String>): List<CoverItem> {
    val remoteItems = podcastUuids.map { uuid ->
        CoverItem.Remote(PodcastImage.getArtworkUrl(size = 480, uuid = uuid, isWearOS = false))
    }
    if (remoteItems.size >= MIN_COVERS_IN_ROW) return remoteItems

    val localItems = artworkResIds.map { CoverItem.Local(it) }
    if (remoteItems.isEmpty()) return localItems

    val padding = localItems.take(MIN_COVERS_IN_ROW - remoteItems.size)
    return remoteItems + padding
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSyncingScreenEmptyPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvSyncingScreenContent(podcastUuids = emptyList())
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSyncingScreenWithPodcastsPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvSyncingScreenContent(
                podcastUuids = listOf(
                    "e7a6f7d0-02f2-0133-1c51-059c869cc4eb",
                    "37589040-0385-012e-f9a0-00163e1b201c",
                    "3782b780-0bc5-012e-fb02-00163e1b201c",
                ),
            )
        }
    }
}
