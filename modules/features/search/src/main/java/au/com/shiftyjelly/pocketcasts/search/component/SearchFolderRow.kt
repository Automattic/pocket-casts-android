package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImageSmall
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SearchFolderRow(
    folder: Folder,
    podcasts: List<Podcast>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showFollowed: Boolean = true,
) {
    val color = MaterialTheme.theme.colors.getFolderColor(folder.color)
    val podcastUuids = podcasts.map { it.uuid }
    SearchFolderRow(
        title = folder.name,
        folderColor = color,
        podcastUuids = podcastUuids,
        modifier = modifier,
        onClick = onClick,
        showFollowed = showFollowed,
    )
}

@Composable
fun SearchFolderRow(
    title: String,
    folderColor: Color,
    podcastUuids: List<String>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showFollowed: Boolean = true,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.theme.colors.primaryUi01)
                .then(if (onClick == null) Modifier else Modifier.clickable { onClick() }),
        ) {
            Box(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 12.dp, bottom = 8.dp)) {
                FolderImageSmall(color = folderColor, podcastUuids = podcastUuids)
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                TextH40(
                    text = title,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                val podcastCount = if (podcastUuids.size == 1) {
                    stringResource(LR.string.podcasts_singular)
                } else {
                    stringResource(LR.string.podcasts_plural, podcastUuids.size)
                }
                TextH50(
                    text = podcastCount,
                    maxLines = 1,
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (showFollowed) {
                Icon(
                    painter = painterResource(id = IR.drawable.ic_tick),
                    contentDescription = stringResource(LR.string.podcast_subscribed),
                    tint = MaterialTheme.theme.colors.support02,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        }
        HorizontalDivider(startIndent = 16.dp)
    }
}
