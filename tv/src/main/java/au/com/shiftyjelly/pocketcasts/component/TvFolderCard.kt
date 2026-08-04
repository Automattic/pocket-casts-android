package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalColors
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date

@Composable
fun TvFolderCard(
    folder: Folder,
    coverUrls: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val folderColor = LocalColors.current.colors.getFolderColor(folder.color)

    TvTile(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = folderColor,
            focusedContainerColor = folderColor,
        ),
        modifier = modifier,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            val cardWidth = maxWidth
            val coverSize = cardWidth * COVER_SIZE_RATIO
            val coverSpacing = cardWidth * COVER_SPACING_RATIO
            val coverCornerRadius = cardWidth * COVER_SPACING_RATIO

            Column(
                verticalArrangement = Arrangement.spacedBy(coverSpacing),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = cardWidth * COVERS_TOP_PADDING_RATIO),
            ) {
                repeat(2) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(coverSpacing)) {
                        repeat(2) { column ->
                            TvArtworkImage(
                                model = coverUrls.getOrNull(row * 2 + column),
                                modifier = Modifier
                                    .size(coverSize)
                                    .clip(RoundedCornerShape(coverCornerRadius)),
                            )
                        }
                    }
                }
            }

            Text(
                text = folder.name,
                style = TvTextStyles.FolderCardTitle,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = cardWidth * TITLE_VERTICAL_PADDING_RATIO)
                    .fillMaxWidth(),
            )
        }
    }
}

private const val COVER_SIZE_RATIO = 0.32f
private const val COVER_SPACING_RATIO = 0.024f
private const val COVERS_TOP_PADDING_RATIO = 0.096f
private const val TITLE_VERTICAL_PADDING_RATIO = 0.064f

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvFolderCardPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark).padding(32.dp)) {
                TvFolderCard(
                    folder = Folder(
                        uuid = "folder",
                        name = "Tech & Science",
                        color = 3,
                        addedDate = Date(0),
                        sortPosition = 0,
                        podcastsSortType = PodcastsSortType.NAME_A_TO_Z,
                        deleted = false,
                        syncModified = 0,
                    ),
                    coverUrls = listOf("", ""),
                    onClick = {},
                    modifier = Modifier.size(160.dp),
                )
            }
        }
    }
}
