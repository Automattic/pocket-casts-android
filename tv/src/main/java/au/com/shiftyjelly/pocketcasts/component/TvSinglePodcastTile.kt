package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import coil3.compose.AsyncImage
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvSinglePodcastTile(
    artworkUrl: String,
    title: String,
    author: String,
    description: String,
    isSponsored: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val textPrimary = if (isFocused) MaterialTheme.tvColors.textPrimaryActive else MaterialTheme.tvColors.textPrimary
    val textSecondary = if (isFocused) MaterialTheme.tvColors.textSecondaryActive else MaterialTheme.tvColors.textSecondary

    TvTile(
        onClick = onClick,
        scale = CardDefaults.scale(focusedScale = 1.02f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(9.dp)),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.tvColors.backgroundSunken,
            focusedContainerColor = MaterialTheme.tvColors.backgroundActive,
        ),
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
    ) {
        Row(
            modifier = Modifier
                .width(876.dp)
                .height(184.dp)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(114.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp),
            ) {
                if (isSponsored || author.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (isSponsored) {
                            Text(
                                text = stringResource(LR.string.sponsored),
                                style = MaterialTheme.tvTypography.body,
                                color = textPrimary,
                            )
                            if (author.isNotBlank()) {
                                Text(
                                    text = "·",
                                    style = MaterialTheme.tvTypography.body,
                                    color = textSecondary,
                                )
                            }
                        }
                        if (author.isNotBlank()) {
                            Text(
                                text = author,
                                style = MaterialTheme.tvTypography.body,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(9.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.tvTypography.title2,
                    color = textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.tvTypography.body,
                        color = textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSinglePodcastTilePreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken).padding(18.dp)) {
            TvSinglePodcastTile(
                artworkUrl = "",
                title = "The Writer's Voice",
                author = "iHeartPodcasts and Kaleidoscope",
                description = "New fiction from the pages of The New Yorker, read by its authors.",
                isSponsored = true,
                onClick = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSinglePodcastTileRecommendedPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken).padding(18.dp)) {
            TvSinglePodcastTile(
                artworkUrl = "",
                title = "The Writer's Voice",
                author = "",
                description = "New fiction from the pages of The New Yorker, read by its authors.",
                isSponsored = false,
                onClick = {},
            )
        }
    }
}
