package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun WinbackOfferErrorPage(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            modifier = Modifier.weight(3f),
        )
        Image(
            painter = painterResource(IR.drawable.ic_warning),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon03),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextH40(
            text = stringResource(LR.string.error_generic_message),
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(percent = 100),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.theme.colors.primaryInteractive03,
            ),
        ) {
            TextP40(
                text = stringResource(LR.string.dismiss),
            )
        }
        Spacer(
            modifier = Modifier.weight(5f),
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun WinbackOfferErrorPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(
        themeType = theme,
    ) {
        WinbackOfferErrorPage(
            onDismiss = {},
        )
    }
}
