package au.com.shiftyjelly.pocketcasts.compose.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButton
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    isMultiSelecting: () -> Boolean,
    isSelected: (Bookmark) -> Boolean,
    onPlayClick: (Bookmark) -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) {
    Column(
        modifier = modifier
    ) {
        Divider(
            color = MaterialTheme.theme.colors.primaryUi05,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isMultiSelecting() && isSelected(bookmark)) {
                        MaterialTheme.theme.colors.primaryUi02Selected
                    } else {
                        MaterialTheme.theme.colors.primaryUi02
                    }
                )
        ) {
            val createdAtText by remember {
                mutableStateOf(
                    bookmark.createdAt.toLocalizedFormatPattern(
                        bookmark.createdAtDatePattern()
                    )
                )
            }
            if (isMultiSelecting()) {
                Checkbox(
                    checked = isSelected(bookmark),
                    onCheckedChange = null,
                    modifier = Modifier
                        .padding(start = 16.dp)
                )
            }

            if (showIcon) {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    PodcastImage(
                        uuid = bookmark.podcastUuid,
                        modifier = modifier.size(56.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                TextC70(
                    text = bookmark.episodeTitle,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 8.dp),
                    isUpperCase = false,
                )

                TextH40(
                    text = bookmark.title,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp),
                )

                TextC70(
                    text = createdAtText,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    isUpperCase = false,
                )
            }

            Box(modifier = Modifier.padding(end = 16.dp)) {
                TimePlayButton(
                    timeSecs = bookmark.timeSecs,
                    contentDescriptionId = LR.string.bookmark_play,
                    onClick = { onPlayClick(bookmark) },
                    backgroundColor = Color.Transparent,
                )
            }
        }
    }
}

@Preview
@Composable
private fun BookmarkPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class)
    themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        BookmarkItem(
            bookmark = Bookmark(
                uuid = "",
                podcastUuid = "",
                episodeTitle = "Episode Title",
                timeSecs = 10,
                title = "Bookmark Title",
                createdAt = Date()
            ),
            isMultiSelecting = { false },
            isSelected = { false },
            onPlayClick = {},
            modifier = Modifier,
            showIcon = false
        )
    }
}
