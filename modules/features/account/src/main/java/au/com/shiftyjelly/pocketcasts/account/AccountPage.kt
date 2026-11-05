package au.com.shiftyjelly.pocketcasts.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIcon
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.cartheme.R as CR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

internal data class AccountPageState(
    val isSignedIn: Boolean,
    val isCar: Boolean,
    val isLandscape: Boolean,
)

@Composable
internal fun AccountPage(
    state: AccountPageState,
    onCloseClick: () -> Unit,
    onCreateClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    continueWithGoogle: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.theme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi01),
    ) {
        if (!state.isCar) {
            AccountHeader(onCloseClick)
        }
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
            ) {
                AccountIntro(state, continueWithGoogle, onCreateClick, onSignInClick)
            }
        }
        if (!state.isCar) {
            RowButton(
                text = stringResource(if (state.isSignedIn) LR.string.done else LR.string.create_account),
                onClick = onCreateClick,
                includePadding = false,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp),
            )
            if (!state.isSignedIn) {
                RowTextButton(
                    text = stringResource(LR.string.sign_in),
                    onClick = onSignInClick,
                    includePadding = false,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun AccountHeader(onCloseClick: () -> Unit) {
    val context = LocalContext.current
    val logo = remember(context) {
        context.obtainStyledAttributes(intArrayOf(UR.attr.logo_title_horizontal)).use { it.getResourceId(0, 0) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        CloseButton(
            onClick = onCloseClick,
            tintColor = MaterialTheme.theme.colors.primaryIcon01,
            contentDescription = stringResource(LR.string.cancel),
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        )
        if (logo != 0) {
            Image(
                painter = painterResource(logo),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 170.dp, height = 34.dp),
            )
        }
    }
}

@Composable
private fun AccountIntro(
    state: AccountPageState,
    continueWithGoogle: @Composable () -> Unit,
    onCreateClick: () -> Unit,
    onSignInClick: () -> Unit,
) {
    val colors = MaterialTheme.theme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val iconModifier = when {
            state.isLandscape -> Modifier.size(0.dp)
            state.isCar -> Modifier.size(360.dp)
            else -> Modifier.size(width = 171.dp, height = 160.dp)
        }
        GradientIcon(
            painter = painterResource(if (state.isSignedIn) IR.drawable.ic_alert_small else R.drawable.ic_create_new_account),
            gradientBrush = Brush.linearGradient(listOf(colors.gradient02A, colors.gradient02E)),
            modifier = iconModifier,
        )
        val titleTop = if (state.isCar) dimensionResource(R.dimen.car_create_heading_top_margin) else 16.dp
        TextH20(
            text = stringResource(if (state.isSignedIn) LR.string.profile_alreadysignedin else LR.string.profile_sign_in_or_create_account),
            fontSize = if (state.isCar) 56.sp else 22.sp,
            lineHeight = if (state.isCar) 64.sp else 30.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(start = 24.dp, top = titleTop, end = 24.dp)
                .then(if (state.isCar) Modifier else Modifier.fillMaxWidth()),
        )
        val description = stringResource(if (state.isSignedIn) LR.string.profile_alreadysignedindescription else LR.string.profile_save_your_podcasts)
        if (state.isCar) {
            TextP40(
                text = description,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                color = colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 24.dp, top = 32.dp, end = 24.dp)
                    .width(670.dp),
            )
        } else {
            TextH40(
                text = description,
                color = colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 24.dp, top = 8.dp, end = 24.dp)
                    .fillMaxWidth(),
            )
        }
        if (state.isCar) {
            CarButtons(state, continueWithGoogle, onCreateClick, onSignInClick)
        }
    }
}

@Composable
private fun CarButtons(
    state: AccountPageState,
    continueWithGoogle: @Composable () -> Unit,
    onCreateClick: () -> Unit,
    onSignInClick: () -> Unit,
) {
    val colors = MaterialTheme.theme.colors
    val textSize = dimensionResource(CR.dimen.car_body2_size).value.sp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = dimensionResource(R.dimen.car_create_account_btn_top_margin), end = 16.dp)
                .width(592.dp),
        ) {
            continueWithGoogle()
        }
        RowButton(
            text = stringResource(if (state.isSignedIn) LR.string.done else LR.string.create_account),
            onClick = onCreateClick,
            includePadding = false,
            fontSize = textSize,
            modifier = Modifier
                .padding(start = 16.dp, top = 32.dp, end = 16.dp)
                .width(592.dp),
        )
        if (!state.isSignedIn) {
            RowTextButton(
                text = stringResource(LR.string.sign_in),
                onClick = onSignInClick,
                includePadding = false,
                fontSize = textSize,
                modifier = Modifier
                    .padding(start = 16.dp, top = 32.dp, end = 16.dp)
                    .width(592.dp),
            )
        }
    }
}
