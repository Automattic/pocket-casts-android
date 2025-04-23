package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.OrientationPreview
import au.com.shiftyjelly.pocketcasts.compose.components.ChapterImage
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import java.util.Date
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ArtworkSection(
    playerViewModel: PlayerViewModel,
) {
    val state by remember {
        playerViewModel.listDataLive.asFlow()
            .distinctUntilChanged { old, new ->
                old.podcastHeader.episode?.uuid == new.podcastHeader.episode?.uuid &&
                    old.podcastHeader.useEpisodeArtwork == new.podcastHeader.useEpisodeArtwork &&
                    old.podcastHeader.chapter?.title == new.podcastHeader.chapter?.title
            }
            .map {
                ArtworkSectionState(
                    episode = it.podcastHeader.episode,
                    useEpisodeArtwork = it.podcastHeader.useEpisodeArtwork,
                    isEpisodeArtworkVisible = it.podcastHeader.isPodcastArtworkVisible(),
                    chapter = it.podcastHeader.chapter,
                    isChapterArtworkVisible = it.podcastHeader.isChapterArtworkVisible(),
                )
            }
    }
        .collectAsStateWithLifecycle(initialValue = ArtworkSectionState())

    Content(
        state = state,
        onChapterUrlClick = { playerViewModel.onChapterUrlClick(it) },
    )
}

private val LANDSCAPE_COMPACT_HEIGHT_BREAKPOINT = 480.dp
private const val PHONE_LANDSCAPE_HEIGHT_CLASS_TOLERANCE = 1.05f

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun Content(
    state: ArtworkSectionState,
    config: ArtworkConfig = ArtworkConfig(),
    onChapterUrlClick: (HttpUrl) -> Unit,
) {
    val activity = LocalContext.current.getActivity()
    val orientation = LocalConfiguration.current.orientation
    val heightSizeClass = if (LocalInspectionMode.current) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) WindowHeightSizeClass.Compact else WindowHeightSizeClass.Medium
    } else {
        val windowSize = activity?.let { calculateWindowSizeClass(it) } ?: return

        // See for details: https://github.com/Automattic/pocket-casts-android/issues/3901
        // As it turned out, there is a very narrow section of phones that are classified as WindowHeightSizeClass.Medium in landscape orientation.
        // These conditions are meant to relax the rule of 480dp by adding a tolerance of +5% in order to treat them as phones in landscape mode.
        if (
            orientation == Configuration.ORIENTATION_LANDSCAPE &&
            windowSize.heightSizeClass != WindowHeightSizeClass.Compact &&
            LocalConfiguration.current.screenHeightDp.dp <= LANDSCAPE_COMPACT_HEIGHT_BREAKPOINT * PHONE_LANDSCAPE_HEIGHT_CLASS_TOLERANCE
        ) {
            WindowHeightSizeClass.Compact
        } else {
            windowSize.heightSizeClass
        }
    }
    val isPhoneLandscape = heightSizeClass == WindowHeightSizeClass.Compact

    Box(
        modifier = Modifier
            .padding(16.dp),
        contentAlignment = if (isPhoneLandscape) Alignment.CenterStart else Alignment.Center,
    ) {
        val artworkModifier = Modifier
            .then(
                if (isPhoneLandscape) {
                    Modifier.sizeIn(
                        maxWidth = config.landscapeImageMaxSize,
                        maxHeight = config.landscapeImageMaxSize,
                    )
                } else {
                    Modifier.fillMaxSize()
                },
            )
            .clearAndSetSemantics {}

        when {
            state.episode != null && state.isEpisodeArtworkVisible -> {
                EpisodeImage(
                    episode = state.episode,
                    useEpisodeArtwork = state.useEpisodeArtwork,
                    useAspectRatio = isPhoneLandscape,
                    corners = config.cornerRadius,
                    contentScale = ContentScale.Fit,
                    modifier = artworkModifier,
                )
            }

            state.isChapterArtworkVisible -> {
                ChapterImage(
                    chapterImagePath = requireNotNull(state.chapter?.imagePath),
                    placeholderType = if (LocalInspectionMode.current) PlaceholderType.Large else PlaceholderType.None,
                    useAspectRatio = isPhoneLandscape,
                    corners = config.cornerRadius,
                    contentScale = ContentScale.Fit,
                    modifier = artworkModifier,
                )
            }

            else -> Box(modifier = artworkModifier)
        }
        state.chapter?.url?.let {
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration(
                    Color.White,
                    RippleDefaults.rippleAlpha(Color.White, true),
                ),
            ) {
                IconButton(
                    onClick = { onChapterUrlClick(it) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(CircleShape),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_link_back),
                        contentDescription = stringResource(id = LR.string.player_chapter_url),
                        modifier = Modifier
                            .size(36.dp),
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_link),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp),
                    )
                }
            }
        }
    }
}

data class ArtworkConfig(
    val cornerRadius: Dp = 8.dp,
    val landscapeImageMaxSize: Dp = 192.dp,
)

data class ArtworkSectionState(
    val episode: BaseEpisode? = null,
    val useEpisodeArtwork: Boolean = false,
    val isEpisodeArtworkVisible: Boolean = false,
    val chapter: Chapter? = null,
    val isChapterArtworkVisible: Boolean = false,
)

@OrientationPreview
@Composable
private fun ArtworkSectionEpisodePreview() {
    Content(
        state = ArtworkSectionState(
            episode = PodcastEpisode("", publishedDate = Date()),
            useEpisodeArtwork = true,
            isEpisodeArtworkVisible = true,
        ),
        onChapterUrlClick = {},
    )
}

@OrientationPreview
@Composable
private fun ArtworkSectionChapterPreview() {
    Content(
        state = ArtworkSectionState(
            chapter = Chapter(
                title = "Chapter Title",
                endTime = 10.toDuration(DurationUnit.MINUTES),
                startTime = 0.toDuration(DurationUnit.MINUTES),
                imagePath = "",
                url = "https://pocketcasts.com".toHttpUrlOrNull(),
                index = 0,
                uiIndex = 1,
            ),
            isChapterArtworkVisible = true,
        ),
        onChapterUrlClick = {},
    )
}
