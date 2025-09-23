package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material.ChipDefaults
import au.com.shiftyjelly.pocketcasts.compose.BuildConfig
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberColumnState
import com.google.android.horologist.compose.material.Chip
import com.google.android.horologist.images.base.paintable.DrawableResPaintable
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun LoginScreen(
    onLoginWithGoogleClick: () -> Unit,
    onLoginWithPhoneClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginWithEmailClick: () -> Unit,
) {
    val columnState = rememberColumnState()

    ScreenScaffold(
        scrollState = columnState,
    ) {
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
                    icon = DrawableResPaintable(IR.drawable.google_g_white),
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
                    icon = DrawableResPaintable(IR.drawable.baseline_phone_android_24),
                    onClick = {
                        viewModel.onPhoneLoginClicked()
                        onLoginWithPhoneClick()
                    },
                )
            }

            if (BuildConfig.DEBUG) {
                item {
                    Chip(
                        labelId = LR.string.log_in_with_email,
                        colors = ChipDefaults.secondaryChipColors(),
                        icon = DrawableResPaintable(IR.drawable.ic_email_white_24dp),
                        onClick = {
                            viewModel.onEmailLoginClicked()
                            onLoginWithEmailClick()
                        },
                    )
                }
            }
        }
    }
}
