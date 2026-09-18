package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.rememberAsyncImagePainter

private val imageSize = 56.dp

@Composable
fun ImprovedSearchNetworkResultRow(
    networkItem: ImprovedSearchResultItem.NetworkItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ImprovedSearchNetworkResultRow(
        title = networkItem.title,
        description = networkItem.description,
        imageUrl = networkItem.imageUrl,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun ImprovedSearchNetworkResultRow(
    title: String,
    description: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NetworkImage(imageUrl = imageUrl)
        Column(modifier = Modifier.weight(1f)) {
            TextH40(
                text = title,
                color = MaterialTheme.theme.colors.primaryText01,
                maxLines = 1,
            )
            if (!description.isNullOrBlank()) {
                TextP50(
                    text = description,
                    color = MaterialTheme.theme.colors.primaryText02,
                    maxLines = 2,
                )
            }
        }
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
            size = imageSize.value.toInt(),
        ).themed().createForFileOrUrl(imageUrl.orEmpty())
    }
    Image(
        painter = rememberAsyncImagePainter(imageRequest, contentScale = ContentScale.Crop),
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = modifier
            .size(imageSize)
            .clip(CircleShape),
    )
}

@Preview
@Composable
private fun PreviewNetworkRow(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        ImprovedSearchNetworkResultRow(
            title = "WNYC",
            description = "New York's flagship public radio station",
            imageUrl = null,
            onClick = {},
        )
    }
}
