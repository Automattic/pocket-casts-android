package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun LoginWithPhoneScreen(
    columnState: ScalingLazyColumnState,
    onDone: () -> Unit,
) {
    ScalingLazyColumn(
        columnState = columnState,
        modifier = Modifier.clickable(onClick = onDone),
    ) {
        item {
            Text(
                text = stringResource(LR.string.log_in_on_phone),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title2,
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
        }

        item {
            Text(
                text = "1. ${stringResource(LR.string.log_in_watch_from_phone_instructions_1)}",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2,
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
        }

        item {
            Text(
                text = "2. ${stringResource(LR.string.log_in_watch_from_phone_instructions_2)}",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
