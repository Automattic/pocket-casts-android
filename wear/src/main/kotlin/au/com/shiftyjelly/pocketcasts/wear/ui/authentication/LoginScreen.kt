package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
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

    val viewModel = hiltViewModel<LoginViewModel>()

    CallOnce {
        viewModel.onShown()
    }

    ScalingLazyColumn(
        columnState = columnState,
    ) {
        item {
            Text(
                text = stringResource(LR.string.log_in),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.title2,
            )
        }

        item {
            Text(
                text = stringResource(LR.string.log_in_subtitle),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.body1,
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
                onClick = {
                    viewModel.onGoogleLoginClicked()
                    onLoginWithGoogleClick()
                },
            )
        }

        item {
            StandardChip(
                labelId = LR.string.log_in_on_phone,
                chipType = StandardChipType.Secondary,
                icon = IR.drawable.baseline_phone_android_24,
                onClick = {
                    viewModel.onPhoneLoginClicked()
                    onLoginWithPhoneClick()
                },
            )
        }

        item {
            StandardChip(
                labelId = LR.string.log_in_with_email,
                chipType = StandardChipType.Secondary,
                icon = IR.drawable.ic_email_white_24dp,
                onClick = {
                    viewModel.onEmailLoginClicked()
                    onLoginWithEmailClick()
                },
            )
        }
    }
}
