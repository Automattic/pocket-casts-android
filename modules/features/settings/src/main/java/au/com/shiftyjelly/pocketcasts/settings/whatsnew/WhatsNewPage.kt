package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewViewModel.UiState
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewViewModel.WhatsNewFeature

@Composable
fun WhatsNewPage(
    onClose: () -> Unit,
    onConfirm: (WhatsNewViewModel.NavigationState) -> Unit,
    viewModel: WhatsNewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    when (state) {
        is UiState.Loading -> Unit
        is UiState.Loaded -> {
            val uiState = state as UiState.Loaded
            WhatsNewPageLoaded(
                state = uiState,
                header = {
                    when (uiState.feature) {
                        is WhatsNewFeature.Shuffle -> ShuffleHeader()
                    }
                },
                onConfirm = { viewModel.onConfirm() },
                onClose = onClose,
            )
        }
    }

    LaunchedEffect(onConfirm) {
        viewModel.navigationState.collect { navigationState ->
            onConfirm(navigationState)
        }
    }
}

@Composable
private fun WhatsNewPageLoaded(
    state: UiState.Loaded,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
    header: @Composable () -> Unit,
) {
    var closing by remember { mutableStateOf(false) }
    val targetAlpha = if (closing) 0f else 0.66f
    val scrimAlpha: Float by animateFloatAsState(
        targetValue = targetAlpha,
        finishedListener = { onClose() },
    )

    val performClose = {
        closing = true
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = performClose,
        )
            // when the device width is wider than the popup add an alpha scrim to the background
            .background(if (LocalConfiguration.current.screenWidthDp > 500) Color.Black.copy(alpha = scrimAlpha) else MaterialTheme.theme.colors.primaryUi01)
            .statusBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 500.dp)
                .background(MaterialTheme.theme.colors.primaryUi01),
        ) {
            ThemedTopAppBar(
                navigationButton = NavigationButton.Close,
                onNavigationClick = performClose,
                style = ThemedTopAppBar.Style.Immersive,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) Modifier.weight(1f) else Modifier),
            ) {
                header()

                Spacer(modifier = Modifier.height(16.dp))

                TextH10(
                    text = stringResource(id = state.feature.title),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText01,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextP40(
                    text = stringResource(state.feature.message),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText01,
                    modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 8.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            RowButton(
                text = stringResource(state.feature.confirmButtonTitle),
                onClick = {
                    performClose()
                    onConfirm()
                },
                includePadding = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            state.feature.closeButtonTitle?.let {
                RowTextButton(
                    text = stringResource(it),
                    fontSize = 15.sp,
                    onClick = performClose,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
