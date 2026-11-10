package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.ChipDefaults
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.material.Chip
import com.google.android.horologist.images.base.paintable.DrawableResPaintable
import com.google.android.horologist.images.base.paintable.ImageVectorPaintable
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun LoginScreen(
    onLoginWithGoogleClick: () -> Unit,
    onLoginWithPhoneClick: () -> Unit,
    onLoginWithCodeClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val columnState = rememberResponsiveColumnState()
    val isCodeLoginEnabled by FeatureFlag.isEnabledFlow(Feature.WEAR_QR_SIGN_IN).collectAsStateWithLifecycle()

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
                        viewModel.onGoogleLoginClicked {
                            onLoginWithGoogleClick()
                        }
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

            if (isCodeLoginEnabled) {
                item {
                    Chip(
                        labelId = LR.string.log_in_with_code,
                        colors = ChipDefaults.secondaryChipColors(),
                        icon = ImageVectorPaintable(Icons.Default.QrCode),
                        onClick = onLoginWithCodeClick,
                    )
                }
            }
        }
    }
}
