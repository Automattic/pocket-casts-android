package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ScreenshotDetectedDialog(
    onNotNow: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onNotNow) {
        Card(
            modifier = modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextH30(
                    text = stringResource(LR.string.end_of_year_share_dialog_title),
                    modifier = Modifier.padding(16.dp),
                )
                TextP60(
                    text = stringResource(LR.string.end_of_year_share_dialog_message),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onNotNow() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            text = stringResource(LR.string.not_now),
                            color = Color.Black,
                            fontSize = 18.sp,
                        )
                    }
                    TextButton(
                        onClick = { onShare() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            text = stringResource(LR.string.share),
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CrashMessageDialogPreview() {
    ScreenshotDetectedDialog(
        onNotNow = {},
        onShare = {},
    )
}
