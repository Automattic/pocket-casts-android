package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.type.Subscription

@Composable
fun SubscribeButton(
    subscription: Subscription,
    onClickSubscribe: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val offerText = when {
        subscription is Subscription.Trial && !isLandscape -> stringResource(R.string.paywall_free_1_month_trial)
        subscription is Subscription.Intro && !isLandscape -> stringResource(R.string.paywall_save_50_off)
        else -> null
    }

    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bottomPadding = if (offerText != null) 0.dp else 34.dp

        SubscribeButtonContent(onClickSubscribe, Modifier.padding(bottom = bottomPadding))

        offerText?.let {
            TextP50(
                text = it,
                fontWeight = FontWeight.W400,
            )
            Spacer(Modifier.height(50.dp))
        }
    }
}

@Composable
private fun SubscribeButtonContent(
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier,
    ) {
        RowButton(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.get_pocket_casts_plus),
            onClick = onClickSubscribe,
            fontWeight = FontWeight.W600,
            fontSize = 18.sp,
            textColor = Color.Black,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colorResource(au.com.shiftyjelly.pocketcasts.ui.R.color.plus_gold),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SubscribeButtonPreview() {
    SubscribeButtonContent(onClickSubscribe = { })
}
