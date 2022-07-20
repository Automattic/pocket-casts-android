package au.com.shiftyjelly.pocketcasts.compose.podcast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.podcastImageCornerSize
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val colorSelectedAlpha = Color(0x7FFFFFFF)

@Composable
fun PodcastSelectImage(
    podcast: Podcast,
    selected: Boolean,
    onPodcastSelected: () -> Unit,
    onPodcastUnselected: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { if (selected) onPodcastUnselected() else onPodcastSelected() }
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center
    ) {
        PodcastImage(
            uuid = podcast.uuid,
            title = podcast.title,
            showTitle = true,
            modifier = Modifier.fillMaxSize()
        )

        if (selected) {
            val corners = podcastImageCornerSize(maxWidth)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(corners))
                    .background(color = colorSelectedAlpha)
            )
        }
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .shadow(2.dp, shape = CircleShape)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF78D549))
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_tick),
                        contentDescription = stringResource(LR.string.selected),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                val notSelected = stringResource(LR.string.not_selected)
                Box(
                    modifier = Modifier
                        .border(width = 1.5.dp, color = Color.White, shape = CircleShape)
                        .size(20.dp)
                        .shadow(1.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Color(0x75FFFFFF))
                        .semantics { contentDescription = notSelected }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorPickerLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        Column {
            PodcastSelectImage(
                podcast = Podcast(uuid = "e7a6f7d0-02f2-0133-1c51-059c869cc4eb", title = "Material"),
                selected = false,
                onPodcastSelected = {},
                onPodcastUnselected = {},
                modifier = Modifier.size(100.dp)
            )
            Spacer(Modifier.height(16.dp))
            PodcastSelectImage(
                podcast = Podcast(uuid = "e7a6f7d0-02f2-0133-1c51-059c869cc4eb", title = "Android Developers Backstage"),
                selected = true,
                onPodcastSelected = {},
                onPodcastUnselected = {},
                modifier = Modifier.size(100.dp)
            )
        }
    }
}
