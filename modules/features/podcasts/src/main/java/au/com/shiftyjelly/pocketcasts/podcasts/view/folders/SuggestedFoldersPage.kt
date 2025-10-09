package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImage
import au.com.shiftyjelly.pocketcasts.compose.layout.verticalNavigationBars
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SuggestedFoldersPage(
    action: SuggestedAction?,
    folders: List<SuggestedFolder>,
    onActionClick: () -> Unit,
    onCreateCustomFolderClick: () -> Unit,
    onFolderClick: (SuggestedFolder) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier.heightIn(min = 64.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(LR.string.close),
                tint = MaterialTheme.theme.colors.primaryInteractive01,
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp),
        )

        if (action != null) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp)
                    .padding(horizontal = 16.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SmartFoldersHeader(action)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(
                        modifier = Modifier.height(10.dp),
                    )
                }
                items(folders) { folder ->
                    val color = MaterialTheme.theme.colors.getFolderColor(folder.colorIndex)
                    val description = stringResource(LR.string.suggested_folder_content_description, folder.name)

                    FolderImage(
                        name = folder.name,
                        color = color,
                        podcastUuids = folder.podcastIds,
                        textSpacing = true,
                        modifier = Modifier
                            .clickable(onClick = { onFolderClick(folder) })
                            .clearAndSetSemantics {
                                contentDescription = description
                            },
                    )
                }
            }

            RowButton(
                text = when (action) {
                    SuggestedAction.UseFolders -> stringResource(LR.string.suggested_folders_use_these_folders_button)
                    SuggestedAction.ReplaceFolders -> stringResource(LR.string.suggested_folders_replace_folders_button)
                },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                textColor = MaterialTheme.theme.colors.primaryInteractive02,
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
                ),
                includePadding = false,
                onClick = onActionClick,
            )

            RowOutlinedButton(
                text = stringResource(LR.string.suggested_folders_create_custom_folder_button),
                onClick = onCreateCustomFolderClick,
                includePadding = false,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryIcon01, backgroundColor = Color.Transparent),
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
                modifier = Modifier
                    .padding(WindowInsets.verticalNavigationBars.asPaddingValues())
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun SmartFoldersHeader(
    action: SuggestedAction,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextH10(
            text = stringResource(LR.string.suggested_folders_title),
        )
        TextP40(
            text = when (action) {
                SuggestedAction.UseFolders -> stringResource(LR.string.suggested_folders_use_subtitle)
                SuggestedAction.ReplaceFolders -> stringResource(LR.string.suggested_folders_replace_subtitle)
            },
            color = MaterialTheme.theme.colors.primaryText02,
        )
    }
}

data class SuggestedFolder(
    val name: String,
    val colorIndex: Int,
    val podcastIds: List<String>,
)

enum class SuggestedAction {
    UseFolders,
    ReplaceFolders,
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun SuggestedFoldersPagePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SuggestedFoldersPage(
            action = SuggestedAction.ReplaceFolders,
            folders = List(20) { index ->
                SuggestedFolder(
                    name = "Folder $index",
                    podcastIds = listOf("1", "2"),
                    colorIndex = index % 12,
                )
            },
            onActionClick = {},
            onCreateCustomFolderClick = {},
            onFolderClick = {},
            onCloseClick = {},
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun SuggestedFoldersPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SuggestedFoldersPage(
            action = SuggestedAction.UseFolders,
            folders = List(20) { index ->
                SuggestedFolder(
                    name = "Folder $index",
                    podcastIds = listOf("1", "2"),
                    colorIndex = index % 12,
                )
            },
            onActionClick = {},
            onCreateCustomFolderClick = {},
            onFolderClick = {},
            onCloseClick = {},
        )
    }
}
