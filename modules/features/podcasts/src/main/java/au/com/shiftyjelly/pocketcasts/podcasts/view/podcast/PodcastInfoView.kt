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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.podcasts.R as IR

@Composable
fun PodcastInfoView(
    state: PodcastInfoState,
    onWebsiteLinkClick: () -> Unit,
    onNetworkClick: () -> Unit,
    modifier: Modifier = Modifier,
    linkColor: Color = MaterialTheme.theme.colors.primaryIcon01,
) {
    val isNetworkDiscoveryEnabled by FeatureFlag.isEnabledFlow(Feature.NETWORK_DISCOVERY).collectAsStateWithLifecycle()
    val isAuthorLinked = isNetworkDiscoveryEnabled && state.networkListId != null
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
            if (state.author.isNotEmpty()) {
                PodcastInfoItem(
                    text = state.author,
                    icon = IR.drawable.ic_author,
                    isLink = isAuthorLinked,
                    linkColor = linkColor,
                    onClick = onNetworkClick,
                )
            }

            if (!state.link.isNullOrEmpty()) {
                PodcastInfoItem(
                    text = state.link,
                    icon = IR.drawable.ic_link,
                    isLink = true,
                    linkColor = linkColor,
                    onClick = onWebsiteLinkClick,
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
    linkColor: Color = MaterialTheme.theme.colors.primaryIcon01,
    onClick: () -> Unit = {},
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
                color = linkColor,
                fontWeight = FontWeight.W400,
                modifier = Modifier.clickable(onClick = onClick),
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
                author = "John",
                networkListId = "list-id",
                link = "www.google.com",
                schedule = "Every two weeks",
                next = "Episode 2",
            ),
            onWebsiteLinkClick = {},
            onNetworkClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

data class PodcastInfoState(
    val author: String,
    val networkListId: String?,
    val link: String?,
    val schedule: String?,
    val next: String?,
)
