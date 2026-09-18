package au.com.shiftyjelly.pocketcasts.discover.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverListSummary
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.rememberAsyncImagePainter

internal val NetworkCardWidth = 168.dp

@Composable
internal fun NetworkCard(
    network: DiscoverListSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        NetworkImage(imageUrl = network.collectionImage)
        Spacer(Modifier.height(TitleSpacing))
        TextH40(
            text = network.title.orEmpty(),
            color = MaterialTheme.theme.colors.primaryText01,
            maxLines = 1,
        )
        Spacer(Modifier.height(DescriptionSpacing))
        TextP50(
            text = network.description.orEmpty(),
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            maxLines = 2,
            // the design reserves two lines so cards next to each other line up whatever their description
            modifier = Modifier.heightIn(min = DescriptionHeight),
        )
    }
}

@Composable
private fun NetworkImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageRequest = remember(imageUrl, context) {
        PocketCastsImageRequestFactory(
            context = context,
            placeholderType = PocketCastsImageRequestFactory.PlaceholderType.Small,
            size = NetworkCardWidth.value.toInt(),
        ).themed().createForFileOrUrl(imageUrl.orEmpty())
    }
    Image(
        painter = rememberAsyncImagePainter(imageRequest, contentScale = ContentScale.Crop),
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .dropShadow(CircleShape, NetworkImageShadow)
            .clip(CircleShape),
    )
}

private val NetworkImageShadow = Shadow(
    radius = 8.dp,
    color = Color.Black,
    spread = 0.dp,
    offset = DpOffset(x = 0.dp, y = 2.dp),
    alpha = 0.15f,
)
private val TitleSpacing: Dp = 10.dp
private val DescriptionSpacing: Dp = 2.dp
private val DescriptionHeight: Dp = 40.dp

internal val NetworkPreview = DiscoverListSummary(
    uuid = "c73d120f-c174-4324-b0a3-18f9b239a59d",
    title = "WNYC",
    description = "New York's flagship public radio station, producing award-winning podcasts across news, politics, arts and science.",
    type = null,
    summaryStyle = null,
    expandedStyle = null,
    source = null,
    collectionImage = null,
    itemCount = 11,
    urlPath = null,
)

@Preview
@Composable
private fun NetworkCardPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        NetworkCard(
            network = NetworkPreview,
            onClick = {},
            modifier = Modifier.width(NetworkCardWidth),
        )
    }
}
