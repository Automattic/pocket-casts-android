package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.components.ChapterImage
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import java.util.Date
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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
        onChapterUrlClick = {
            state.chapter?.url?.let {
                playerViewModel.onChapterUrlClick(it.toString())
            }
        },
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Content(
    state: ArtworkSectionState,
    onChapterUrlClick: () -> Unit,
) {
    val isPortrait = LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE
    Box(
        modifier = Modifier
            .padding(16.dp),
        contentAlignment = if (isPortrait) Alignment.Center else Alignment.CenterStart,
    ) {
        val artworkModifier = Modifier
            .then(if (isPortrait) Modifier.fillMaxSize() else Modifier.sizeIn(maxWidth = 192.dp, maxHeight = 192.dp))
            .clip(RoundedCornerShape(8.dp))

        when {
            state.episode != null && state.isEpisodeArtworkVisible -> {
                EpisodeImage(
                    episode = state.episode,
                    useEpisodeArtwork = state.useEpisodeArtwork,
                    useAspectRatio = !isPortrait,
                    modifier = artworkModifier
                        .clearAndSetSemantics {},
                )
            }

            state.isChapterArtworkVisible -> {
                ChapterImage(
                    chapterImagePath = requireNotNull(state.chapter?.imagePath),
                    placeholderType = if (LocalInspectionMode.current) PlaceholderType.Large else PlaceholderType.None,
                    useAspectRatio = !isPortrait,
                    modifier = artworkModifier,
                )
            }

            else -> Box(modifier = artworkModifier)
        }
        state.chapter?.url?.let {
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration(Color.White, RippleDefaults.rippleAlpha(Color.White, true)),
            ) {
                IconButton(
                    onClick = onChapterUrlClick,
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

data class ArtworkSectionState(
    val episode: BaseEpisode? = null,
    val useEpisodeArtwork: Boolean = false,
    val isEpisodeArtworkVisible: Boolean = false,
    val chapter: Chapter? = null,
    val isChapterArtworkVisible: Boolean = false,
)

@Preview(widthDp = 300, heightDp = 300)
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

@Preview(widthDp = 300, heightDp = 300)
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
            ),
            isChapterArtworkVisible = true,
        ),
        onChapterUrlClick = {},
    )
}
