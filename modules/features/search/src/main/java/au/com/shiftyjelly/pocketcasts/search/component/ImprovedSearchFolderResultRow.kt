package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun ImprovedSearchFolderResultRow(
    folderItem: ImprovedSearchResultItem.FolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchFolderRow(
        folder = folderItem.folder,
        podcasts = folderItem.podcasts,
        onClick = onClick,
        modifier = modifier,
        showFollowed = false,
    )
}

@Composable
fun ImprovedSearchFolderResultRow(
    folder: SearchAutoCompleteItem.Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchFolderRow(
        title = folder.title,
        folderColor = MaterialTheme.theme.colors.getFolderColor(folder.color),
        podcastUuids = folder.podcasts.map { it.uuid },
        onClick = onClick,
        modifier = modifier,
        showFollowed = false,
    )
}

@Preview
@Composable
private fun PreviewFolderRow(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        ImprovedSearchFolderResultRow(
            folder = SearchAutoCompleteItem.Folder(
                uuid = "",
                title = "Folder",
                podcasts = listOf(
                    SearchAutoCompleteItem.Podcast(
                        uuid = "",
                        title = "podcast",
                        author = "author",
                        isSubscribed = true,
                    ),
                ),
                color = 0xff00ff,
            ),
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
