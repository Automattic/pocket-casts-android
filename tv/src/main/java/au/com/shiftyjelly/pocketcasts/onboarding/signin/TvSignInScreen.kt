package au.com.shiftyjelly.pocketcasts.onboarding.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvSignInScreen(
    onSignInComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvSignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val emailState by viewModel.emailState.collectAsStateWithLifecycle()
    val currentOnSignInComplete by rememberUpdatedState(onSignInComplete)

    CallOnce { viewModel.trackShown() }

    LaunchedEffect(uiState) {
        if (uiState is TvSignInUiState.Complete) {
            currentOnSignInComplete()
        }
    }

    TvSignInContent(
        mode = mode,
        qrState = uiState,
        emailState = emailState,
        onSelectMode = viewModel::selectMode,
        onEmailChange = viewModel::updateEmail,
        onPasswordChange = viewModel::updatePassword,
        onSubmitEmail = viewModel::submitEmailSignIn,
        onRetryQr = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun TvSignInContent(
    mode: TvSignInMode,
    qrState: TvSignInUiState,
    emailState: TvEmailSignInState,
    onSelectMode: (TvSignInMode) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmitEmail: () -> Unit,
    onRetryQr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.tvColors.backgroundSunken),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Image(
                painter = painterResource(IR.drawable.ic_pocket_casts_logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = stringResource(LR.string.tv_sign_in_title),
                color = MaterialTheme.tvColors.textPrimary,
                style = MaterialTheme.tvTypography.title2.copy(textAlign = TextAlign.Center),
            )
            TvSignInModeTabs(selected = mode, onSelect = onSelectMode)
            when (mode) {
                TvSignInMode.QrCode -> TvQrSignIn(state = qrState, onRetry = onRetryQr)

                TvSignInMode.Email -> TvEmailSignInForm(
                    state = emailState,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onSubmit = onSubmitEmail,
                )
            }
            if (mode == TvSignInMode.Email) {
                Spacer(modifier = Modifier.height(TvSignInKeyboardInset))
            }
        }
    }
}

@Composable
private fun TvSignInModeTabs(
    selected: TvSignInMode,
    onSelect: (TvSignInMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = TvSignInMode.entries.indexOf(selected)
    val selectedTabFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .background(MaterialTheme.tvColors.backgroundBase, RoundedCornerShape(percent = 50))
            .padding(3.dp),
    ) {
        TabRow(
            selectedTabIndex = selectedIndex,
            modifier = Modifier.focusProperties {
                onEnter = { runCatching { selectedTabFocusRequester.requestFocus() } }
            },
            containerColor = Color.Transparent,
            indicator = @Composable { tabPositions, doesTabRowHaveFocus ->
                tabPositions.getOrNull(selectedIndex)?.let { currentTabPosition ->
                    TabRowDefaults.PillIndicator(
                        currentTabPosition = currentTabPosition,
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                        activeColor = MaterialTheme.tvColors.backgroundActive,
                        inactiveColor = MaterialTheme.tvColors.backgroundActive20,
                    )
                }
            },
        ) {
            TvSignInMode.entries.forEachIndexed { index, tabMode ->
                Tab(
                    selected = index == selectedIndex,
                    onFocus = { onSelect(tabMode) },
                    onClick = { onSelect(tabMode) },
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 24.dp)
                        .then(
                            if (index == selectedIndex) Modifier.focusRequester(selectedTabFocusRequester) else Modifier,
                        ),
                    colors = TabDefaults.pillIndicatorTabColors(
                        contentColor = MaterialTheme.tvColors.textPrimary,
                        selectedContentColor = MaterialTheme.tvColors.textPrimary,
                        focusedContentColor = MaterialTheme.tvColors.textPrimary,
                        focusedSelectedContentColor = MaterialTheme.tvColors.textPrimaryActive,
                        inactiveContentColor = MaterialTheme.tvColors.textPrimary,
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(tabMode.labelRes()),
                            color = LocalContentColor.current,
                            style = MaterialTheme.tvTypography.caption1,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { selectedTabFocusRequester.requestFocus() }
    }
}

@Composable
private fun TvQrSignIn(
    state: TvSignInUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.heightIn(min = 280.dp),
    ) {
        when (state) {
            is TvSignInUiState.Ready -> TvSignInQrContent(
                userCode = state.userCode,
                verificationUriComplete = state.verificationUriComplete,
                steps = signInSteps(state.verificationUri),
            )

            is TvSignInUiState.Error -> TvSignInErrorContent(onRetry = onRetry)

            else -> LoadingView(
                color = MaterialTheme.tvColors.textPrimary,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Composable
internal fun TvSignInErrorContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(IR.drawable.ic_waitingforwifi),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.tvColors.textSecondary),
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(LR.string.tv_sign_in_qr_error),
            color = MaterialTheme.tvColors.textPrimary,
            style = MaterialTheme.tvTypography.body.copy(textAlign = TextAlign.Center),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = TvButtonDefaults.filledButtonColors(),
            modifier = Modifier.focusRequester(focusRequester),
        ) {
            Text(text = stringResource(LR.string.try_again))
        }
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
}

@Composable
private fun signInSteps(verificationUri: String): List<String> {
    val url = remember(verificationUri) { verificationDisplayUrl(verificationUri) }
    return listOf(
        stringResource(LR.string.tv_sign_in_step_scan, url),
        stringResource(LR.string.tv_sign_in_step_login),
        stringResource(LR.string.tv_sign_in_step_confirm_code),
    )
}

private fun TvSignInMode.labelRes() = when (this) {
    TvSignInMode.QrCode -> LR.string.tv_sign_in_tab_qr_code
    TvSignInMode.Email -> LR.string.tv_sign_in_tab_email
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSignInScreenQrPreview() {
    TvTheme {
        TvSignInContent(
            mode = TvSignInMode.QrCode,
            qrState = TvSignInUiState.Ready(
                userCode = listOf("J", "M", "R", "3", "W", "2"),
                verificationUri = "https://pocketcasts.com/pair",
                verificationUriComplete = "https://pocketcasts.com/pair?code=JMR3W2",
            ),
            emailState = TvEmailSignInState(),
            onSelectMode = {},
            onEmailChange = {},
            onPasswordChange = {},
            onSubmitEmail = {},
            onRetryQr = {},
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSignInScreenEmailPreview() {
    TvTheme {
        TvSignInContent(
            mode = TvSignInMode.Email,
            qrState = TvSignInUiState.Loading,
            emailState = TvEmailSignInState(email = "listener@pocketcasts.com"),
            onSelectMode = {},
            onEmailChange = {},
            onPasswordChange = {},
            onSubmitEmail = {},
            onRetryQr = {},
        )
    }
}
