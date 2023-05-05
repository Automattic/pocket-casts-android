package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import com.google.android.horologist.base.ui.components.StandardChip
import com.google.android.horologist.base.ui.components.StandardChipType
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun LoginScreen(
    columnState: ScalingLazyColumnState,
    onLoginWithGoogleClick: () -> Unit,
    onLoginWithPhoneClick: () -> Unit,
    onLoginWithEmailClick: () -> Unit,
) {
    ScalingLazyColumn(
        columnState = columnState,
    ) {
        item {
            Text(
                text = stringResource(LR.string.log_in),
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
                color = Color.White,
            )
        }

        item {
            Text(
                text = stringResource(LR.string.log_in_subtitle),
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
                color = Color.White,
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            StandardChip(
                labelId = LR.string.log_in_with_google,
                chipType = StandardChipType.Secondary,
                icon = IR.drawable.google_g_white,
                onClick = onLoginWithGoogleClick,
            )
        }

        item {
            StandardChip(
                labelId = LR.string.log_in_on_phone,
                chipType = StandardChipType.Secondary,
                icon = IR.drawable.baseline_phone_android_24,
                onClick = onLoginWithPhoneClick,
            )
        }

        item {
            StandardChip(
                labelId = LR.string.log_in_with_email,
                chipType = StandardChipType.Secondary,
                icon = IR.drawable.ic_email_white_24dp,
                onClick = onLoginWithEmailClick,
            )
        }
    }
}
