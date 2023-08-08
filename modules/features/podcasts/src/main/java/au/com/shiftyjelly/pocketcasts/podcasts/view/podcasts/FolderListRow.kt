package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImageSmall
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun FolderListRow(
    color: Color,
    name: String,
    podcastUuids: List<String>,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    badgeType: BadgeType = BadgeType.OFF,
    onClick: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(80.dp)
            .fillMaxWidth()
            .background(MaterialTheme.theme.colors.primaryUi01)
            .padding(horizontal = 16.dp)
            .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
    ) {
        FolderImageSmall(color = color, podcastUuids = podcastUuids)
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = name,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.theme.colors.primaryText01,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            val podcastCount = if (podcastUuids.size == 1) {
                stringResource(LR.string.podcasts_singular)
            } else {
                stringResource(LR.string.podcasts_plural, podcastUuids.size)
            }
            Text(
                text = podcastCount,
                fontSize = 13.sp,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (badgeType != BadgeType.OFF) {
            Text(
                text = if (badgeType != BadgeType.LATEST_EPISODE) badgeCount.toString() else "●",
                fontSize = 14.sp,
                color = if (badgeType == BadgeType.LATEST_EPISODE) MaterialTheme.theme.colors.support05 else MaterialTheme.theme.colors.primaryText02
            )
        }
    }
}
