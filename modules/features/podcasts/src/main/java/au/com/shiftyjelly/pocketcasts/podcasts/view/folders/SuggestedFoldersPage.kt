package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImage
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SuggestedFoldersPage(
    onDismiss: () -> Unit,
    onUseTheseFolders: () -> Unit,
    onCreateCustomFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        IconButton(
            onClick = onDismiss,
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_close),
                contentDescription = stringResource(LR.string.close),
                tint = MaterialTheme.theme.colors.primaryInteractive01,
                modifier = Modifier.padding(16.dp),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .padding(horizontal = 16.dp)
                .weight(1f),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                TextH10(
                    text = stringResource(LR.string.suggested_folders),
                    lineHeight = 36.sp,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                TextP40(
                    text = stringResource(LR.string.suggested_folders_subtitle),
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            items(
                count = 4,
                key = { index -> index },
            ) { index ->
                FolderItem("Test", Color.Yellow, podcastUuids = mockedPodcastsUuids)
            }
        }

        RowButton(
            text = stringResource(LR.string.suggested_folders_use_these_folders_button),
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
            onClick = onUseTheseFolders,
        )

        RowOutlinedButton(
            text = stringResource(id = LR.string.suggested_folders_use_create_custom_folders_button),
            onClick = onCreateCustomFolders,
            includePadding = false,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryIcon01, backgroundColor = Color.Transparent),
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
fun FolderItem(folderName: String, folderColor: Color, podcastUuids: List<String>, modifier: Modifier = Modifier) {
    val stringResource = stringResource(LR.string.folder_content_description, folderName)

    FolderImage(
        name = folderName,
        color = folderColor,
        podcastUuids = podcastUuids,
        textSpacing = true,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = stringResource
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SuggestedFoldersPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        SuggestedFoldersPage(
            onDismiss = {},
            onUseTheseFolders = {},
            onCreateCustomFolders = {},
        )
    }
}

private val mockedPodcastsUuids = listOf(
    "5d308950-1fe3-012e-02b0-00163e1b201c",
    "f086f200-4f32-0139-3396-0acc26574db2",
    "2e61ba20-50a9-0135-902b-63f4b61a9224",
    "f98ce900-79da-0139-347d-0acc26574db2",
    "39844640-7cb5-013b-f2a7-0acc26574db2",
    "2d721350-e0d4-0137-b6c9-0acc26574db2",
)
