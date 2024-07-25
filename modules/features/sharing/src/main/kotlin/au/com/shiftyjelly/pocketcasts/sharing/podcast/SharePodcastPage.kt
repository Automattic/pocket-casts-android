package au.com.shiftyjelly.pocketcasts.sharing.podcast

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.sharing.social.PlatformBar
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.CloseButton
import au.com.shiftyjelly.pocketcasts.sharing.ui.Devices
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.sharing.ui.VerticalPodcastCast
import com.airbnb.android.showkase.annotation.ShowkaseComposable

internal interface SharePodcastPageListener {
    fun onShare(podcast: Podcast, platform: SocialPlatform)
    fun onClose()

    companion object {
        val Preview = object : SharePodcastPageListener {
            override fun onShare(podcast: Podcast, platform: SocialPlatform) = Unit
            override fun onClose() = Unit
        }
    }
}

@Composable
internal fun SharePodcastPage(
    podcast: Podcast?,
    episodeCount: Int,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    listener: SharePodcastPageListener,
) = when (LocalConfiguration.current.orientation) {
    Configuration.ORIENTATION_LANDSCAPE -> Box { }
    else -> VerticalSharePodcastPage(
        podcast = podcast,
        episodeCount = episodeCount,
        socialPlatforms = socialPlatforms,
        shareColors = shareColors,
        listener = listener,
    )
}

@Composable
private fun VerticalSharePodcastPage(
    podcast: Podcast?,
    episodeCount: Int,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    listener: SharePodcastPageListener,
) = Column(
    modifier = Modifier
        .fillMaxSize()
        .background(shareColors.background),
) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier
            .weight(0.2f)
            .fillMaxSize(),
    ) {
        CloseButton(
            shareColors = shareColors,
            onClick = listener::onClose,
            modifier = Modifier.padding(top = 12.dp, end = 12.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            TextH30(
                text = "Share podcast",
                textAlign = TextAlign.Center,
                color = shareColors.backgroundPrimaryText,
                modifier = Modifier.sizeIn(maxWidth = 220.dp),
            )
            Spacer(
                modifier = Modifier.height(8.dp),
            )
            TextH40(
                text = "Chose a format and a platform to share to",
                textAlign = TextAlign.Center,
                color = shareColors.backgroundSecondaryText,
                modifier = Modifier.sizeIn(maxWidth = 220.dp),
            )
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(0.6f)
            .fillMaxSize(),
    ) {
        if (podcast != null) {
            VerticalPodcastCast(
                podcast = podcast,
                episodeCount = episodeCount,
                shareColors = shareColors,
            )
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.weight(0.18f),
    ) {
        PlatformBar(socialPlatforms, shareColors) { platform ->
            if (podcast != null) {
                listener.onShare(podcast, platform)
            }
        }
    }
}

@ShowkaseComposable(name = "SharePodcastVerticalRegularPreview", group = "Podcast")
@Preview(name = "SharePodcastVerticalRegularPreview", device = Devices.PortraitRegular)
@Composable
fun SharePodcastVerticalRegularPreview() = SharePodcastPagePreview()

@ShowkaseComposable(name = "SharePodcastVerticalSmallPreview", group = "Podcast")
@Preview(name = "SharePodcastVerticalSmallPreview", device = Devices.PortraitSmall)
@Composable
fun SharePodcastVerticalSmallPreviewPreview() = SharePodcastPagePreview()

@ShowkaseComposable(name = "SharePodcastVerticalTabletPreview", group = "Podcast")
@Preview(name = "SharePodcastVerticalTabletPreview", device = Devices.PortraitTablet)
@Composable
fun SharePodcastVerticalTabletPreview() = SharePodcastPagePreview()

@ShowkaseComposable(name = "SharePodcastHorizontalRegularPreview", group = "Podcast")
@Preview(name = "SharePodcastHorizontalRegularPreview", device = Devices.LandscapeRegular)
@Composable
fun SharePodcastHorizontalRegularPreview() = SharePodcastPagePreview()

@ShowkaseComposable(name = "SharePodcastHorizontalSmallPreview", group = "Podcast")
@Preview(name = "SharePodcastHorizontalSmallPreview", device = Devices.LandscapeSmall)
@Composable
fun SharePodcastHorizontalSmallPreviewPreview() = SharePodcastPagePreview()

@ShowkaseComposable(name = "SharePodcastHorizontalTabletPreview", group = "Podcast")
@Preview(name = "SharePodcastHorizontalTabletPreview", device = Devices.LandscapeTablet)
@Composable
fun SharePodcastHorizontalTabletPreview() = SharePodcastPagePreview()

@Composable
internal fun SharePodcastPagePreview(
    color: Long = 0xFFEC0404,
) = SharePodcastPage(
    podcast = Podcast(
        uuid = "podcast-id",
        title = "Podcast title",
        episodeFrequency = "monthly",
    ),
    episodeCount = 120,
    socialPlatforms = SocialPlatform.entries.toSet(),
    shareColors = ShareColors(Color(color)),
    listener = SharePodcastPageListener.Preview,
)
