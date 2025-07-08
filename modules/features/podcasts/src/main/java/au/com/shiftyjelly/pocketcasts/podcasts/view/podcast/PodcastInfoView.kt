package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.podcasts.R as IR

@Composable
fun PodcastInfoView(
    state: PodcastInfoState,
    onWebsiteLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.theme.colors.primaryUi02,
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.theme.colors.primaryUi05,
                shape = RoundedCornerShape(8.dp),
            )
            .fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            PodcastInfoItem(
                state.author,
                IR.drawable.ic_author,
            )

            if (!state.link.isNullOrEmpty()) {
                PodcastInfoItem(
                    text = state.link,
                    icon = IR.drawable.ic_link,
                    isLink = true,
                    onWebsiteLinkClick = onWebsiteLinkClick,
                )
            }

            if (!state.schedule.isNullOrEmpty()) {
                PodcastInfoItem(
                    text = state.schedule,
                    icon = IR.drawable.ic_schedule,
                )
            }

            if (!state.next.isNullOrEmpty()) {
                PodcastInfoItem(
                    text = state.next,
                    icon = IR.drawable.ic_nextepisode,
                )
            }
        }
    }
}

@Composable
private fun PodcastInfoItem(
    text: String,
    icon: Int,
    modifier: Modifier = Modifier,
    isLink: Boolean = false,
    onWebsiteLinkClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.theme.colors.primaryIcon02,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(24.dp),
        )
        if (isLink) {
            TextP40(
                text = text,
                maxLines = 3,
                color = MaterialTheme.theme.colors.support05,
                fontWeight = FontWeight.W400,
                modifier = Modifier.clickable {
                    onWebsiteLinkClick.invoke()
                },
            )
        } else {
            TextP40(
                text = text,
                maxLines = 3,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.theme.colors.primaryText01,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewPodcastInfoView(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        PodcastInfoView(
            state = PodcastInfoState(
                "John",
                "www.google.com",
                "Every two weeks",
                "Episode 2",
            ),
            onWebsiteLinkClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

data class PodcastInfoState(
    val author: String,
    val link: String?,
    val schedule: String?,
    val next: String?,
)
