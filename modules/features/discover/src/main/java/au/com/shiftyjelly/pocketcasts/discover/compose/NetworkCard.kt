package au.com.shiftyjelly.pocketcasts.discover.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.NetworkImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverListSummary
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

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
        NetworkImage(
            imageUrl = network.collectionImage,
            imageRequestSize = NetworkCardWidth,
            modifier = Modifier.fillMaxWidth(),
        )
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
