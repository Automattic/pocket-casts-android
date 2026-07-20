package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun SyncedTranscriptsHeader(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = IR.drawable.whats_new_synced_transcripts),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    )
}
