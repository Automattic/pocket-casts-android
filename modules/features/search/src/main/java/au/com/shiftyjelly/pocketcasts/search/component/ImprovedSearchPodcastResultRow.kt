package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ImprovedSearchPodcastResultRow(
    item: SearchAutoCompleteItem.Podcast,
    onClick: () -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ImprovedSearchPodcastResultRow(
        podcastUuid = item.uuid,
        title = item.title,
        author = item.author,
        isSubscribed = item.isSubscribed,
        isExplicit = item.isExplicit,
        onClick = onClick,
        onFollow = onFollow,
        modifier = modifier,
    )
}

@Composable
fun ImprovedSearchPodcastResultRow(
    podcastItem: ImprovedSearchResultItem.PodcastItem,
    onClick: () -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ImprovedSearchPodcastResultRow(
        podcastUuid = podcastItem.uuid,
        title = podcastItem.title,
        author = podcastItem.author,
        isSubscribed = podcastItem.isFollowed,
        isExplicit = podcastItem.isExplicit,
        onClick = onClick,
        onFollow = onFollow,
        modifier = modifier,
    )
}

@Composable
private fun ImprovedSearchPodcastResultRow(
    podcastUuid: String,
    title: String,
    author: String,
    isSubscribed: Boolean,
    isExplicit: Boolean,
    onClick: () -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showExplicitIndicator by FeatureFlag
        .isEnabledFlow(Feature.EXPLICIT_PODCAST_INDICATOR)
        .collectAsStateWithLifecycle()

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PodcastImage(
            uuid = podcastUuid,
            elevation = 6.dp,
            cornerSize = 4.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            PodcastResultTitle(
                title = title,
                isExplicit = isExplicit && showExplicitIndicator,
            )
            TextP50(
                text = author,
                color = MaterialTheme.theme.colors.primaryText02,
                maxLines = 1,
            )
        }
        if (isSubscribed) {
            Icon(
                modifier = Modifier
                    .minimumInteractiveComponentSize(),
                painter = painterResource(IR.drawable.ic_check_black_24dp),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.support02,
            )
        } else {
            IconButton(onClick = onFollow) {
                Icon(
                    painter = painterResource(IR.drawable.ic_add_black_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.secondaryText02,
                )
            }
        }
    }
}

@Composable
private fun PodcastResultTitle(
    title: String,
    isExplicit: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextH40(
            text = title,
            color = MaterialTheme.theme.colors.primaryText01,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (isExplicit) {
            val explicitDescription = stringResource(LR.string.explicit)
            Icon(
                painter = painterResource(IR.drawable.explicit),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier
                    .size(16.dp)
                    .clearAndSetSemantics { contentDescription = explicitDescription },
            )
        }
    }
}

@Preview
@Composable
private fun PreviewPodcastRow(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        ImprovedSearchPodcastResultRow(
            podcastUuid = "",
            title = "Podcast title",
            author = "Author name",
            isSubscribed = false,
            isExplicit = true,
            onClick = {},
            onFollow = {},
        )
    }
}
