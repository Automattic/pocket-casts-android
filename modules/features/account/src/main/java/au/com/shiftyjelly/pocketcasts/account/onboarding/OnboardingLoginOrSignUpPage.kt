package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.R
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingLoginOrSignUpViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.compose.components.RectangleCover
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogo
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.androidbrowserhelper.trusted.Utils.setNavigationBarColor
import com.google.androidbrowserhelper.trusted.Utils.setStatusBarColor
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingLoginOrSignUpPage(
    theme: Theme.ThemeType,
    flow: String,
    onDismiss: () -> Unit,
    onSignUpClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    onContinueWithGoogleComplete: () -> Unit,
    viewModel: OnboardingLoginOrSignUpViewModel = hiltViewModel()
) {

    val systemUiController = rememberSystemUiController()
    val pocketCastsTheme = MaterialTheme.theme

    LaunchedEffect(Unit) {
        viewModel.onShown(flow)
        systemUiController.apply {
            setStatusBarColor(pocketCastsTheme.colors.primaryUi01.copy(alpha = 0.9f), darkIcons = !theme.darkTheme)
            setNavigationBarColor(Color.Transparent, darkIcons = !theme.darkTheme)
        }
    }

    BackHandler {
        viewModel.onDismiss(flow)
        onDismiss()
    }

    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {

        Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars))

        Row(
            Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            Box(Modifier.weight(1f)) {
                NavigationIconButton(
                    iconColor = MaterialTheme.theme.colors.primaryText01,
                    navigationButton = NavigationButton.Close,
                    onNavigationClick = {
                        viewModel.onDismiss(flow)
                        onDismiss()
                    }
                )
            }

            HorizontalLogo(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .height(28.dp)
            )

            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        Artwork(viewModel.showContinueWithGoogleButton, viewModel.randomPodcasts)

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
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        if (viewModel.showContinueWithGoogleButton) {
            Spacer(Modifier.height(8.dp))
            ContinueWithGoogleButton(
                flow = flow,
                viewModel = viewModel,
                onComplete = onContinueWithGoogleComplete
            )
        } else {
            Spacer(Modifier.height(8.dp))
        }

        SignUpButton(onClick = {
            viewModel.onSignUpClicked(flow)
            onSignUpClicked()
        })

        LogInButton(onClick = {
            viewModel.onLoginClicked(flow)
            onLoginClicked()
        })

        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Composable
private fun Artwork(
    googleSignInShown: Boolean,
    podcasts: List<Podcast>,
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val configuration = LocalConfiguration.current

    val viewWidth = localView.width.pxToDp(context).dp
    val viewHeight = localView.height.pxToDp(context).dp

    val artworkWidth = viewWidth * Artwork.getScaleFactor(googleSignInShown)
    val maxY = Artwork.coverModels.maxOf { it.y }
    val artworkAspectRatio = Artwork.getAspectRatio(configuration, googleSignInShown)
    val artworkHeight = minOf(viewWidth * maxY * artworkAspectRatio, viewHeight / 2f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(artworkHeight)
            .fillMaxWidth()
            .offset(x = artworkWidth * Artwork.getOffsetFactor(googleSignInShown))
    ) {
        Artwork.coverModels.mapIndexed { index, model ->
            val coverWidth = (artworkWidth * model.size).coerceAtMost(artworkHeight / 2f)
            val modifier = Modifier
                .offset(
                    x = artworkWidth * model.x,
                    y = artworkHeight * model.y * Artwork.getCoverYOffsetFactor(configuration)
                )
            val podcast = if (index < podcasts.size) podcasts[index] else null
            podcast?.let {
                PodcastCover(
                    uuid = it.uuid,
                    coverWidth = coverWidth,
                    cornerRadius = 4.dp,
                    modifier = modifier
                )
            } ?: RectangleCover(
                imageResId = model.imageResId,
                coverWidth = coverWidth,
                cornerRadius = 4.dp,
                modifier = modifier
            )
        }
    }
}

/**
 * Let the user sign into Pocket Casts with their Google account.
 * The One Tap for Android library is used. Sign in doesn't work when no Google accounts are set up on the device. In this case, fallback to the legacy Google Sign-In for Android.
 */
@Composable
private fun ContinueWithGoogleButton(
    flow: String,
    viewModel: OnboardingLoginOrSignUpViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val errorMessage = stringResource(LR.string.onboarding_continue_with_google_error)

    val showError = {
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    }

    // request legacy Google Sign-In and process the result
    val googleLegacySignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onGoogleLegacySignInResult(
            result = result,
            onSuccess = onComplete,
            onError = showError
        )
    }

    // request Google One Tap Sign-In and process the result
    val googleOneTapSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onGoogleOneTapSignInResult(
            result = result,
            onSuccess = onComplete,
            onError = {
                viewModel.startGoogleLegacySignIn(
                    onSuccess = { request -> googleLegacySignInLauncher.launch(request) },
                    onError = showError
                )
            }
        )
    }

    val onSignInClick = {
        viewModel.startGoogleOneTapSignIn(
            flow = flow,
            onSuccess = { request -> googleOneTapSignInLauncher.launch(request) },
            onError = {
                viewModel.startGoogleLegacySignIn(
                    onSuccess = { request -> googleLegacySignInLauncher.launch(request) },
                    onError = showError
                )
            }
        )
    }

    RowOutlinedButton(
        text = stringResource(LR.string.onboarding_continue_with_google),
        leadingIcon = painterResource(IR.drawable.google_g),
        tintIcon = false,
        border = BorderStroke(2.dp, MaterialTheme.theme.colors.primaryInteractive03),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryText01),
        onClick = onSignInClick
    )
}

@Composable
private fun SignUpButton(onClick: () -> Unit) {
    RowButton(
        text = stringResource(LR.string.onboarding_sign_up),
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.theme.colors.primaryText01, contentColor = MaterialTheme.theme.colors.primaryUi01),
        includePadding = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun LogInButton(onClick: () -> Unit) {
    RowTextButton(
        text = stringResource(LR.string.onboarding_log_in),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryText01),
        includePadding = false,
        onClick = onClick,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
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
private fun RowOutlinedButtonPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        OnboardingLoginOrSignUpPage(
            theme = themeType,
            flow = "initial_onboarding",
            onDismiss = {},
            onSignUpClicked = {},
            onLoginClicked = {},
            onContinueWithGoogleComplete = {},
        )
    }
}
