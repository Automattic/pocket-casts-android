package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.ChipDefaults
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.material.Chip
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun LoginScreen(
    columnState: ScalingLazyColumnState,
    onLoginWithGoogleClick: () -> Unit,
    onLoginWithPhoneClick: () -> Unit,
) {

    val viewModel = hiltViewModel<LoginViewModel>()

    CallOnce {
        viewModel.onShown()
    }

    ScalingLazyColumn(
        columnState = columnState,
    ) {
        item {
            Chip(
                labelId = LR.string.log_in_with_google,
                colors = ChipDefaults.secondaryChipColors(),
                icon = IR.drawable.google_g_white,
                onClick = {
                    viewModel.onGoogleLoginClicked()
                    onLoginWithGoogleClick()
                },
            )
        }

        item {
            Chip(
                labelId = LR.string.log_in_on_phone,
                colors = ChipDefaults.secondaryChipColors(),
                icon = IR.drawable.baseline_phone_android_24,
                onClick = {
                    viewModel.onPhoneLoginClicked()
                    onLoginWithPhoneClick()
                },
            )
        }
    }
}
