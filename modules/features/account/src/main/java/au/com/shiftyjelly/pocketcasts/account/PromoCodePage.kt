package au.com.shiftyjelly.pocketcasts.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIcon
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class PromoCodePageState(
    val isProgressVisible: Boolean,
    val isLoaded: Boolean,
    @DrawableRes val imageRes: Int,
    @DrawableRes val overlayRes: Int?,
    val title: String,
    val detail: String,
    val doneText: String,
    val createText: String?,
    val isSignInVisible: Boolean,
)

@Composable
internal fun PromoCodePage(
    state: PromoCodePageState,
    onCloseClick: () -> Unit,
    onDoneClick: () -> Unit,
    onCreateClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi01),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (state.isLoaded) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 30.dp),
                    ) {
                        Box(modifier = Modifier.size(160.dp)) {
                            GradientIcon(
                                painter = painterResource(state.imageRes),
                                gradientBrush = Brush.linearGradient(listOf(colors.gradient01A, colors.gradient01E)),
                                contentDescription = stringResource(LR.string.done),
                                modifier = Modifier.fillMaxSize(),
                            )
                            state.overlayRes?.let {
                                Image(painter = painterResource(it), contentDescription = null, modifier = Modifier.fillMaxSize())
                            }
                        }
                        TextH20(text = state.title, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 32.dp))
                        TextP30(
                            text = state.detail,
                            color = colors.primaryText02,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                if (state.isProgressVisible) {
                    CircularProgressIndicator(color = colors.primaryInteractive01)
                }
            }
            state.createText?.let {
                RowTextButton(text = it, onClick = onCreateClick, includePadding = false, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
            }
            if (state.isLoaded) {
                RowButton(text = state.doneText, onClick = onDoneClick, includePadding = false, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
            }
            if (state.isSignInVisible) {
                RowTextButton(
                    text = stringResource(LR.string.sign_in),
                    onClick = onSignInClick,
                    includePadding = false,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
            }
        }
        if (state.isLoaded) {
            CloseButton(onClick = onCloseClick, tintColor = colors.primaryInteractive01, modifier = Modifier.padding(12.dp))
        }
    }
}
