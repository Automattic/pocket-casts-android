package au.com.shiftyjelly.pocketcasts.reimagine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform

@Composable
internal fun PlatformBar(
    platforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    onClick: (SocialPlatform) -> Unit,
) {
    val platformsToDisplay = remember(platforms) { platforms.normalize() }
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        platformsToDisplay.forEach { platform ->
            PlatformItem(
                platform = platform,
                shareColors = shareColors,
                onClick = onClick,
            )
        }
    }
}

private fun Set<SocialPlatform>.normalize() = buildList {
    val alwaysAvailablePlatforms = listOf(SocialPlatform.PocketCasts, SocialPlatform.More)
    addAll((this@normalize.sorted() - alwaysAvailablePlatforms).take(2))
    addAll(alwaysAvailablePlatforms)
}

@Preview
@Composable
private fun PlatformBarPreview(
    @PreviewParameter(SocialPlatformsProvider::class) platforms: Set<SocialPlatform>,
) {
    val shareColors = ShareColors(Color(0xFFEC0404))
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.background(shareColors.background),
    ) {
        PlatformBar(
            platforms = platforms,
            shareColors = shareColors,
            onClick = {},
        )
    }
}

private class SocialPlatformsProvider : PreviewParameterProvider<Set<SocialPlatform>> {
    override val values get() = sequenceOf(
        setOf(SocialPlatform.Instagram),
        setOf(SocialPlatform.Tumblr, SocialPlatform.Instagram),
        setOf(SocialPlatform.Telegram, SocialPlatform.X),
        setOf(SocialPlatform.WhatsApp, SocialPlatform.Tumblr, SocialPlatform.X),
        SocialPlatform.entries.toSet(),
        emptySet(),
    )
}
