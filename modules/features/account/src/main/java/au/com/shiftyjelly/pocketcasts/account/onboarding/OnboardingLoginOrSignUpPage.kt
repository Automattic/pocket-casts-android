package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.account.R
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.ContinueWithGoogleButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.GoogleSignInButtonViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.GoogleSignInState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingLoginOrSignUpViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingLoginOrSignUpViewModel.UiState
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.compose.components.RectangleCover
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogo
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.ui.extensions.inLandscape
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingLoginOrSignUpPage(
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    onDismiss: () -> Unit,
    onSignUpClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    onContinueWithGoogleComplete: (GoogleSignInState) -> Unit,
    viewModel: OnboardingLoginOrSignUpViewModel = hiltViewModel(),
) {
    val systemUiController = rememberSystemUiController()
    val pocketCastsTheme = MaterialTheme.theme

    CallOnce {
        viewModel.onShown(flow)
    }

    LaunchedEffect(Unit) {
        systemUiController.apply {
            setStatusBarColor(pocketCastsTheme.colors.primaryUi01.copy(alpha = 0.9f), darkIcons = !theme.darkTheme)
            setNavigationBarColor(Color.Transparent, darkIcons = !theme.darkTheme)
        }
    }

    val onNavigationClick = {
        viewModel.onDismiss(flow)
        onDismiss()
    }

    BackHandler {
        onNavigationClick()
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Content(
        state = state,
        flow = flow,
        showContinueWithGoogleButton = viewModel.showContinueWithGoogleButton,
        onSignUpClicked = {
            viewModel.onSignUpClicked(flow)
            onSignUpClicked()
        },
        onLoginClicked = {
            viewModel.onLoginClicked(flow)
            onLoginClicked()
        },
        onContinueWithGoogleComplete = onContinueWithGoogleComplete,
        onNavigationClick = onNavigationClick,
    )
}

@Composable
private fun Content(
    state: UiState,
    flow: OnboardingFlow,
    showContinueWithGoogleButton: Boolean,
    onNavigationClick: () -> Unit,
    onSignUpClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    onContinueWithGoogleComplete: (GoogleSignInState) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = maxWidth
        val height = maxHeight
        Column(
            Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars))

            Row(
                Modifier
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .fillMaxWidth(),
            ) {
                Box(Modifier.weight(1f)) {
                    NavigationIconButton(
                        iconColor = MaterialTheme.theme.colors.primaryText01,
                        navigationButton = NavigationButton.Close,
                        onNavigationClick = onNavigationClick,
                    )
                }

                HorizontalLogo(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .height(28.dp),
                )

                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))

            if (state is UiState.Loaded) {
                val context = LocalContext.current
                Artwork(
                    googleSignInShown = GoogleSignInButtonViewModel.showContinueWithGoogleButton(
                        context,
                    ),
                    podcasts = state.randomPodcasts,
                    viewWidth = width,
                    viewHeight = height,
                )
            }

            Spacer(Modifier.weight(1f))

            TextH10(
                text = stringResource(LR.string.onboarding_discover_your_next_favorite_podcast),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            TextH40(
                text = stringResource(LR.string.onboarding_create_an_account_to),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            if (showContinueWithGoogleButton) {
                Spacer(Modifier.height(8.dp))
                ContinueWithGoogleButton(
                    flow = flow,
                    onComplete = onContinueWithGoogleComplete,
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            SignUpButton(onClick = onSignUpClicked)
            LogInButton(onClick = onLoginClicked)
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun Artwork(
    googleSignInShown: Boolean,
    podcasts: List<Podcast>,
    viewWidth: Dp,
    viewHeight: Dp,
) {
    val configuration = LocalConfiguration.current
    if (configuration.inLandscape()) {
        return
    }

    val artworkWidth = viewWidth * Artwork.getScaleFactor(googleSignInShown)
    val maxY = Artwork.coverModels.maxOf { it.y }
    val artworkAspectRatio = Artwork.getAspectRatio(configuration, googleSignInShown)
    val artworkHeight = minOf(viewWidth * maxY * artworkAspectRatio, viewHeight / 2f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(artworkHeight)
            .fillMaxWidth()
            .offset(x = artworkWidth * Artwork.getOffsetFactor(googleSignInShown)),
    ) {
        Artwork.coverModels.mapIndexed { index, model ->
            val coverWidth = (artworkWidth * model.size).coerceAtMost(artworkHeight / 2f)
            val modifier = Modifier
                .offset(
                    x = artworkWidth * model.x,
                    y = artworkHeight * model.y * Artwork.getCoverYOffsetFactor(configuration),
                )
            val podcast = if (index < podcasts.size) podcasts[index] else null
            podcast?.let {
                PodcastCover(
                    uuid = it.uuid,
                    coverWidth = coverWidth,
                    cornerRadius = 4.dp,
                    modifier = modifier,
                )
            } ?: RectangleCover(
                imageResId = model.imageResId,
                coverWidth = coverWidth,
                cornerRadius = 4.dp,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SignUpButton(onClick: () -> Unit) {
    RowButton(
        text = stringResource(LR.string.onboarding_sign_up),
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.theme.colors.primaryText01, contentColor = MaterialTheme.theme.colors.primaryUi01),
        includePadding = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun LogInButton(onClick: () -> Unit) {
    RowTextButton(
        text = stringResource(LR.string.onboarding_log_in),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryText01),
        includePadding = false,
        onClick = onClick,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
    )
}

private object Artwork {
    data class CoverModel(
        @DrawableRes val imageResId: Int,
        val size: Float,
        val x: Float,
        val y: Float,
    )
    val coverModels = listOf(
        CoverModel(imageResId = R.drawable.conan, size = 0.2f, x = -0.39f, y = 0.05f),
        CoverModel(imageResId = R.drawable.radiolab, size = 0.126f, x = 0.14f, y = 0.28f),
        CoverModel(imageResId = R.drawable.a24, size = 0.126f, x = -0.13f, y = 0.34f),
        CoverModel(imageResId = R.drawable.conversations, size = 0.126f, x = -0.18f, y = -0.34f),
        CoverModel(imageResId = R.drawable.sevenam, size = 0.2f, x = -0.05f, y = -0.14f),
        CoverModel(imageResId = R.drawable.thedaily, size = 0.183f, x = 0.25f, y = -0.3f),
    )

    fun getAspectRatio(configuration: Configuration, googleSignInShown: Boolean) =
        if (configuration.orientation == ORIENTATION_LANDSCAPE || googleSignInShown) {
            2.2f
        } else {
            2.6f
        }
    fun getScaleFactor(googleSignInShown: Boolean) =
        if (googleSignInShown) 1.65f else 1.85f
    fun getOffsetFactor(googleSignInShown: Boolean) =
        if (googleSignInShown) 0.06f else 0.0f
    fun getCoverYOffsetFactor(configuration: Configuration) =
        if (configuration.orientation == ORIENTATION_LANDSCAPE) 0.75f else 0.95f
}

@Preview(showBackground = true)
@Composable
private fun OnboardingLoginOrSignUpPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        OnboardingLoginOrSignUpPage(
            theme = themeType,
            flow = OnboardingFlow.InitialOnboarding,
            onDismiss = {},
            onSignUpClicked = {},
            onLoginClicked = {},
            onContinueWithGoogleComplete = {},
        )
    }
}
