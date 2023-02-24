package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImageSmall
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val FolderImageSize = 156.dp
private val PodcastImageSize = 68.dp
private val SubscribeIconSize = 32.dp

@Composable
fun SearchFolderItem(
    folder: Folder,
    podcasts: List<Podcast>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.theme.colors.getFolderColor(folder.color)
    Column(
        modifier = modifier
            .width(FolderImageSize + 16.dp)
            .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
            .padding(8.dp)
    ) {
        BoxWithConstraints(
            modifier = modifier.aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            FolderImageSmall(
                color = color,
                podcastUuids = podcasts.map { it.uuid },
                folderImageSize = FolderImageSize,
                podcastImageSize = PodcastImageSize
            )

            val buttonBackgroundColor = Color.Black.copy(alpha = 0.4f)
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(SubscribeIconSize)
                        .clip(CircleShape)
                        .background(buttonBackgroundColor)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tick),
                        contentDescription = stringResource(LR.string.podcast_subscribed),
                        tint = Color.White,
                        modifier = Modifier.size(SubscribeIconSize / 1.25f)
                    )
                }
            }
        }

        Column(
            modifier = modifier
                .padding(top = 10.dp)
        ) {
            TextH40(
                text = folder.name,
                maxLines = 1,
            )
            val podcastCount = if (podcasts.size == 1) {
                stringResource(LR.string.podcasts_singular)
            } else {
                stringResource(
                    LR.string.podcasts_plural,
                    podcasts.size
                )
            }
            TextH50(
                text = podcastCount,
                maxLines = 1,
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
