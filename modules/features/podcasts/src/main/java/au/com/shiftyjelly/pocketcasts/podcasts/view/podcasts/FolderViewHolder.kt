package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImage
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.math.min

class FolderViewHolder(
    val composeView: ComposeView,
    val theme: Theme,
    val gridWidthDp: Int,
    val podcastsLayout: PodcastGridLayoutType,
    val onFolderClick: (Folder) -> Unit
) : RecyclerView.ViewHolder(composeView) {

    fun bind(folder: Folder, podcasts: List<Podcast>, badgeType: BadgeType, podcastUuidToBadge: Map<String, Int>) {
        val badgeCount = calculateFolderBadge(podcasts, badgeType, podcastUuidToBadge)

        composeView.setContent {
            AppTheme(theme.activeTheme) {
                val color = MaterialTheme.theme.colors.getFolderColor(folder.color)
                val podcastUuids = podcasts.map { it.uuid }
                when (podcastsLayout) {
                    PodcastGridLayoutType.LIST_VIEW -> {
                        FolderListAdapter(
                            color = color,
                            name = folder.name,
                            podcastUuids = podcastUuids,
                            badgeCount = badgeCount,
                            badgeType = badgeType,
                            onClick = { onFolderClick(folder) }
                        )
                    }
                    else -> {
                        FolderGridAdapter(
                            color = color,
                            name = folder.name,
                            podcastUuids = podcastUuids,
                            badgeCount = badgeCount,
                            badgeType = badgeType,
                            onClick = { onFolderClick(folder) },
                            modifier = Modifier.size(gridWidthDp.dp)
                        )
                    }
                }
            }
        }
    }

    private fun calculateFolderBadge(podcasts: List<Podcast>, badgeType: BadgeType, podcastUuidToBadge: Map<String, Int>): Int {
        if (badgeType == BadgeType.OFF) {
            return 0
        }
        val episodeCount = podcasts.sumOf { podcast -> podcastUuidToBadge[podcast.uuid] ?: 0 }
        return when (badgeType) {
            BadgeType.OFF -> 0
            BadgeType.ALL_UNFINISHED -> min(99, episodeCount)
            BadgeType.LATEST_EPISODE -> min(1, episodeCount)
        }
    }
}

@Composable
private fun FolderGridAdapter(color: Color, name: String, podcastUuids: List<String>, badgeCount: Int, badgeType: BadgeType, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FolderImage(
        name = name,
        color = color,
        podcastUuids = podcastUuids,
        badgeCount = badgeCount,
        badgeType = badgeType,
        modifier = modifier.clickable { onClick() }
    )
}

@Composable
private fun FolderListAdapter(color: Color, name: String, podcastUuids: List<String>, badgeCount: Int, badgeType: BadgeType, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column {
        FolderListRow(
            color = color,
            name = name,
            podcastUuids = podcastUuids,
            badgeCount = badgeCount,
            badgeType = badgeType,
            modifier = modifier,
            onClick = onClick
        )
        HorizontalDivider()
    }
}
