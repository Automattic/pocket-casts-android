package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImage
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SuggestedFoldersPage(
    folders: List<Folder>,
    useWhiteColorForHowItWorks: Boolean,
    onShown: () -> Unit,
    onDismiss: () -> Unit,
    onUseTheseFolders: () -> Unit,
    onCreateCustomFolders: () -> Unit,
    onHowItWorks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CallOnce {
        onShown.invoke()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
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
                SuggestedFoldersDescription(
                    textColor = MaterialTheme.theme.colors.primaryText02,
                    secondTextColor = if (useWhiteColorForHowItWorks) Color.White else MaterialTheme.theme.colors.primaryInteractive01,
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    onHowItWorks.invoke()
                }
            }
            items(
                count = folders.size,
                key = { index -> index },
            ) { index ->
                val folder = folders[index]
                val backgroundColor = MaterialTheme.theme.colors.getFolderColor(folder.color)
                FolderItem(folder.name, backgroundColor, podcastUuids = folder.podcasts)
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

@Composable
private fun SuggestedFoldersDescription(
    textColor: Color,
    secondTextColor: Color,
    modifier: Modifier = Modifier,
    onSecondTextClick: () -> Unit,
) {
    val firstString = stringResource(LR.string.suggested_folders_subtitle)
    val secondString = stringResource(LR.string.suggested_folders_how_it_works)

    val annotatedString = buildAnnotatedString {
        append(firstString)
        append(" ")
        addLink(
            url = LinkAnnotation.Url(
                url = "",
                styles = TextLinkStyles(style = SpanStyle(color = secondTextColor)),
            ) {
                onSecondTextClick.invoke()
            },
            start = firstString.length + 1,
            end = firstString.length + 1 + secondString.length,
        )

        withStyle(style = SpanStyle(color = secondTextColor, fontWeight = FontWeight.W700)) {
            append(secondString)
        }
    }

    Text(
        text = annotatedString,
        color = textColor,
        modifier = modifier,
        fontSize = 16.sp,
        lineHeight = 22.sp,
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
            onShown = {},
            onHowItWorks = {},
            useWhiteColorForHowItWorks = false,
            folders = listOf(
                Folder("Folder 1", listOf("2e61ba20-50a9-0135-902b-63f4b61a9224", "2e61ba20-50a9-0135-902b-63f4b61a9224"), 1),
                Folder("Folder 2", listOf("2e61ba20-50a9-0135-902b-63f4b61a9224", "2e61ba20-50a9-0135-902b-63f4b61a9224"), 2),
            ),
        )
    }
}
