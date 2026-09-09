package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TextSpan
import au.com.shiftyjelly.pocketcasts.transcripts.ui.BookmarkTranscriptView
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun BookmarkTranscriptEditPage(
    uiState: BookmarkTranscriptEditViewModel.UiState,
    playerColors: PlayerColors,
    onPassageChange: (TextSpan) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Header(
            playerColors = playerColors,
            showDone = uiState is BookmarkTranscriptEditViewModel.UiState.Loaded,
            onSave = onSave,
            onClose = onClose,
        )
        when (uiState) {
            is BookmarkTranscriptEditViewModel.UiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator(color = playerColors.highlight01)
                }
            }

            is BookmarkTranscriptEditViewModel.UiState.NotAvailable -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    TextP40(
                        text = stringResource(LR.string.transcript_error_not_available),
                        color = playerColors.contrast02,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is BookmarkTranscriptEditViewModel.UiState.Loaded -> {
                TextP40(
                    text = stringResource(LR.string.bookmark_edit_transcript_subtitle),
                    color = playerColors.contrast02,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                BookmarkTranscriptView(
                    transcript = uiState.transcript,
                    passage = uiState.passage,
                    editable = true,
                    onPassageChange = onPassageChange,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun Header(
    playerColors: PlayerColors,
    showDone: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(IR.drawable.ic_close),
                    contentDescription = stringResource(LR.string.close),
                    tint = playerColors.contrast01,
                )
            }
            Text(
                text = stringResource(LR.string.bookmark_edit_transcript_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = playerColors.contrast01,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showDone) {
                TextButton(onClick = onSave) {
                    Text(
                        text = stringResource(LR.string.done),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = playerColors.highlight01,
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}
