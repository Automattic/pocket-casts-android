package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SuggestedFoldersPaywall(
    onShown: () -> Unit,
    onUseTheseFolders: () -> Unit,
    onMaybeLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CallOnce {
        onShown.invoke()
    }

    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .wrapContentSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

        val animatedPadding by animateDpAsState(
            targetValue = if (isPortrait) 32.dp else 8.dp,
        )

        Icon(
            painter = painterResource(IR.drawable.ic_swipe),
            contentDescription = null,
            tint = MaterialTheme.theme.colors.primaryUi05,
            modifier = Modifier.clearAndSetSemantics {},
        )

        AnimatedVisibility(isPortrait) {
            Folders(
                podcastUuids = mockedPodcastsUuids,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = animatedPadding),
            )
        }

        TextH30(
            text = stringResource(LR.string.suggested_folders_paywall_tittle),
            fontWeight = FontWeight.W600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        TextH50(
            text = stringResource(LR.string.suggested_folders_paywall_subtitle),
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        RowButton(
            text = stringResource(LR.string.suggested_folders_use_these_folders_button),
            modifier = Modifier.padding(bottom = 16.dp),
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
            text = stringResource(id = LR.string.maybe_later),
            modifier = Modifier.padding(bottom = 16.dp),
            onClick = onMaybeLater,
            includePadding = false,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryIcon01, backgroundColor = Color.Transparent),
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
        )
    }
}

@Composable
private fun Folders(podcastUuids: List<String>, modifier: Modifier = Modifier) {
    val episodeImageWidthDp = UiUtil.getGridImageWidthPx(smallArtwork = false, context = LocalContext.current).pxToDp(LocalContext.current).toInt()

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        items(
            count = 3,
            key = { index -> index },
        ) { index ->
            FolderItem("Test", Color.Yellow, podcastUuids, Modifier.size(episodeImageWidthDp.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestedFoldersPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        SuggestedFoldersPaywall(
            onUseTheseFolders = {},
            onMaybeLater = {},
            onShown = {},
        )
    }
}

private val mockedPodcastsUuids = listOf(
    "5d308950-1fe3-012e-02b0-00163e1b201c",
    "f086f200-4f32-0139-3396-0acc26574db2",
    "2e61ba20-50a9-0135-902b-63f4b61a9224",
)
