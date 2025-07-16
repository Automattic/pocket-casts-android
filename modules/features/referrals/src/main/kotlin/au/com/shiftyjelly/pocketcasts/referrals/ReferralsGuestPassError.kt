package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ReferralsGuestPassError(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        CloseButton(
            modifier = Modifier
                .align(Alignment.End),
            onClick = onDismiss,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextH40(
            text = errorMessage,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(40.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White.copy(alpha = 0.2f),
            ),
        ) {
            TextP40(
                text = stringResource(LR.string.try_again),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.W400,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview
@Composable
private fun ReferralsSendGuestPassErrorPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        ReferralsGuestPassError(
            errorMessage = stringResource(LR.string.error_no_network),
            onRetry = {},
            onDismiss = {},
        )
    }
}
