package au.com.shiftyjelly.pocketcasts.onboarding.signin

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvArtworkImage
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.onboarding.tvOnboardingBackground
import au.com.shiftyjelly.pocketcasts.onboarding.welcome.artworkResIds
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val COVER_SIZE_DP = 140
private const val COVER_CORNER_RADIUS_DP = 14
private const val MAX_SIMULTANEOUS = 2
private const val MAX_COVER_POOL = 40
private const val MIN_ROTATION = 2f
private const val MAX_ROTATION = 10f
private const val ROTATION_STEPS = 5
private const val STEP_INTERVAL_MS = 800L
private const val RETIRE_FADE_MS = 500
private const val RETIRE_REMOVE_MS = RETIRE_FADE_MS + 100L

private val RevealSpring = spring<Float>(dampingRatio = 0.5f, stiffness = 250f)
private val bundledCovers = artworkResIds.map(CoverModel::Bundled)

@Composable
fun TvSyncingScreen(
    onSyncComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvSyncingViewModel = hiltViewModel(),
) {
    BackHandler {}

    CallOnce { viewModel.trackShown() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnSyncComplete by rememberUpdatedState(onSyncComplete)
    LaunchedEffect(uiState.syncComplete) {
        if (uiState.syncComplete) {
            currentOnSyncComplete()
        }
    }

    TvSyncingScreenContent(podcastUuids = uiState.podcastUuids, modifier = modifier)
}

@Composable
private fun TvSyncingScreenContent(
    podcastUuids: List<String>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .tvOnboardingBackground(MaterialTheme.tvColors.backgroundBase, MaterialTheme.tvColors.backgroundSunken),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = stringResource(LR.string.tv_onboarding_welcome_back),
                color = MaterialTheme.tvColors.textPrimary,
                style = MaterialTheme.tvTypography.title1.copy(textAlign = TextAlign.Center),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_syncing_subtitle),
                color = MaterialTheme.tvColors.textSecondary,
                style = MaterialTheme.tvTypography.headline.copy(textAlign = TextAlign.Center),
            )
            Spacer(modifier = Modifier.height(30.dp))
            TvSyncingCoverStack(podcastUuids = podcastUuids)
        }
    }
}

@Composable
private fun TvSyncingCoverStack(
    podcastUuids: List<String>,
    modifier: Modifier = Modifier,
) {
    val realCovers = remember(podcastUuids) {
        podcastUuids.take(MAX_COVER_POOL).map { CoverModel.Remote(PodcastImage.getMediumArtworkUrl(it)) }
    }
    val source: List<CoverModel> = if (realCovers.size > MAX_SIMULTANEOUS) realCovers else bundledCovers
    val currentSource by rememberUpdatedState(source)

    val mounted = remember { mutableStateListOf<MountedCover>() }

    LaunchedEffect(Unit) {
        val revealed = mutableSetOf<CoverModel>()
        var nextId = 0
        while (isActive) {
            val pool = currentSource
            val visible = mounted.filterNot { it.exiting }.mapTo(mutableSetOf()) { it.model }
            val cover = nextCover(pool, visible, revealed)
            if (cover == null) {
                delay(STEP_INTERVAL_MS)
                continue
            }
            if (cover in revealed) {
                revealed.clear()
            }
            revealed.add(cover)
            val id = nextId++
            mounted.add(MountedCover(id = id, model = cover, rotation = coverRotation(id)))
            if (mounted.count { !it.exiting } > MAX_SIMULTANEOUS) {
                mounted.firstOrNull { !it.exiting }?.let { stale ->
                    stale.exiting = true
                    launch {
                        delay(RETIRE_REMOVE_MS)
                        mounted.remove(stale)
                    }
                }
            }
            delay(STEP_INTERVAL_MS)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(COVER_SIZE_DP.dp),
    ) {
        mounted.forEach { cover ->
            key(cover.id) {
                RevealedCover(cover)
            }
        }
    }
}

@Composable
private fun RevealedCover(cover: MountedCover) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.6f) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, RevealSpring) }
        launch { scale.animateTo(1f, RevealSpring) }
        launch { rotation.animateTo(cover.rotation, RevealSpring) }
    }
    LaunchedEffect(cover.exiting) {
        if (cover.exiting) {
            alpha.animateTo(0f, tween(durationMillis = RETIRE_FADE_MS, easing = LinearOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .size(COVER_SIZE_DP.dp)
            .graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = rotation.value
            }
            .clip(RoundedCornerShape(COVER_CORNER_RADIUS_DP.dp)),
    ) {
        when (val model = cover.model) {
            is CoverModel.Bundled -> Image(
                painter = painterResource(model.resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            is CoverModel.Remote -> TvArtworkImage(
                model = model.url,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun <T> nextCover(pool: List<T>, visible: Set<T>, revealed: Set<T>): T? {
    return pool.firstOrNull { it !in visible && it !in revealed }
        ?: pool.firstOrNull { it !in visible }
}

private fun coverRotation(id: Int): Float {
    val sign = if (id % 2 == 0) -1f else 1f
    val t = (id * 3 % ROTATION_STEPS).toFloat() / (ROTATION_STEPS - 1)
    return sign * (MIN_ROTATION + t * (MAX_ROTATION - MIN_ROTATION))
}

private sealed interface CoverModel {
    data class Bundled(@DrawableRes val resId: Int) : CoverModel
    data class Remote(val url: String) : CoverModel
}

private class MountedCover(
    val id: Int,
    val model: CoverModel,
    val rotation: Float,
) {
    var exiting by mutableStateOf(false)
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSyncingScreenFallbackPreview() {
    TvTheme {
        TvSyncingScreenContent(podcastUuids = emptyList())
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSyncingScreenRealCoversPreview() {
    TvTheme {
        TvSyncingScreenContent(podcastUuids = List(6) { "uuid-$it" })
    }
}
