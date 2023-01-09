package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.R
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingPlusFeatures
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingWelcomeState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingWelcomeViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.extensions.brush
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingWelcomePage(
    activeTheme: Theme.ThemeType,
    flow: OnboardingFlow,
    isSignedInAsPlus: Boolean,
    onDone: () -> Unit,
    onContinueToDiscover: () -> Unit,
    onImportTapped: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val viewModel = hiltViewModel<OnboardingWelcomeViewModel>()
    val state by viewModel.stateFlow.collectAsState()

    val systemUiController = rememberSystemUiController()
    val pocketCastsTheme = MaterialTheme.theme

    LaunchedEffect(Unit) {
        viewModel.onShown(flow)
        systemUiController.apply {
            setStatusBarColor(pocketCastsTheme.colors.primaryUi01.copy(alpha = 0.9f), darkIcons = !activeTheme.darkTheme)
            setNavigationBarColor(Color.Transparent, darkIcons = !activeTheme.darkTheme)
        }
    }

    BackHandler {
        viewModel.onDismiss(flow, persistNewsletter = false)
        onBackPressed()
    }

    Content(
        isSignedInAsPlus = isSignedInAsPlus,
        onContinueToDiscover = {
            viewModel.onContinueToDiscover(flow)
            onContinueToDiscover()
        },
        onImportTapped = {
            viewModel.onImportTapped(flow)
            onImportTapped()
            /* Mark confetti as shown if import tapped before confetti animation ends. */
            viewModel.onConfettiShown()
        },
        state = state,
        onDone = {
            viewModel.onDismiss(flow, persistNewsletter = true)
            onDone()
        },
        onNewsletterCheckedChanged = viewModel::updateNewsletter
    )

    if (state.showConfetti) {
        Confetti { viewModel.onConfettiShown() }
    }
}

@Composable
private fun Content(
    isSignedInAsPlus: Boolean,
    onContinueToDiscover: () -> Unit,
    onImportTapped: () -> Unit,
    state: OnboardingWelcomeState,
    onDone: () -> Unit,
    onNewsletterCheckedChanged: (Boolean) -> Unit,
) {
    Column(
        Modifier
//            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars))
        Spacer(Modifier.weight(1f))

        if (isSignedInAsPlus) {
            PlusPersonCheckmark()
        } else {
            PersonCheckmark()
        }

        Spacer(Modifier.height(8.dp))
        TextH10(
            text = stringResource(
                if (isSignedInAsPlus) {
                    LR.string.onboarding_welcome_get_you_listening_plus
                } else {
                    LR.string.onboarding_welcome_get_you_listening
                }
            ),
            modifier = Modifier.padding(end = 8.dp)
        )

        Spacer(Modifier.height(24.dp))
        CardSection(
            titleRes = LR.string.onboarding_import_podcasts_title,
            descriptionRes = LR.string.onboarding_import_podcasts_text,
            actionRes = LR.string.onboarding_import_podcasts_button,
            iconRes = IR.drawable.pc_bw_import,
            onClick = onImportTapped
        )

        Spacer(Modifier.height(24.dp))
        CardSection(
            titleRes = LR.string.onboarding_welcome_recommendations_title,
            descriptionRes = LR.string.onboarding_welcome_recommendations_text,
            actionRes = LR.string.onboarding_welcome_recommendations_button,
            iconRes = IR.drawable.circle_star,
            onClick = onContinueToDiscover
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(Modifier.height(16.dp))

        NewsletterSwitch(
            checked = state.newsletter,
            onCheckedChange = onNewsletterCheckedChanged
        )

        Spacer(Modifier.height(16.dp))
        RowButton(
            text = stringResource(LR.string.done),
            includePadding = false,
            onClick = onDone,

        )

        Spacer(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(16.dp)
        )
    }
}

@Composable
private fun Confetti(
    onConfettiShown: () -> Unit,
) {
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val progress by animateLottieCompositionAsState(lottieComposition)

    LottieAnimation(
        composition = lottieComposition,
        contentScale = ContentScale.Crop,
    )
    if (progress == 1.0f) onConfettiShown()
}

@Composable
private fun CardSection(
    @StringRes titleRes: Int,
    @StringRes descriptionRes: Int,
    @StringRes actionRes: Int,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.theme.colors.primaryUi05,
        ),
        modifier = Modifier.clickable {
            onClick()
        }
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column(
                    // This column needs a weight modifier so that it is measured after
                    // the icon. Otherwise, it seems that the text is measured first and
                    // doesn't leave any room for the icon
                    modifier = Modifier.weight(weight = 1f),
                ) {
                    TextH40(stringResource(titleRes))
                    Spacer(Modifier.height(4.dp))
                    TextP60(
                        text = stringResource(descriptionRes),
                        color = MaterialTheme.theme.colors.primaryText02,
                    )
                }

                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.primaryInteractive01,
                    modifier = Modifier.width(56.dp)
                )
            }
            Divider(
                thickness = 1.dp,
                color = MaterialTheme.theme.colors.primaryUi05,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            TextH40(
                text = stringResource(actionRes),
                color = MaterialTheme.theme.colors.primaryInteractive01,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun NewsletterSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
    ) {
        Column(
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f)
        ) {
            TextH40(stringResource(LR.string.onboarding_get_the_newsletter))
            TextP60(
                text = stringResource(LR.string.profile_create_newsletter_summary),
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray,
            )
        )
    }
}

@Composable
private fun PlusPersonCheckmark() {
    PersonCheckmark(OnboardingPlusFeatures.plusGradientBrush)
}

@Composable
private fun PersonCheckmark(
    personBrush: Brush? = null,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
    ) {
        val personModifier = Modifier.offset(x = 9.dp, y = 9.dp).let { modifier ->
            personBrush?.let {
                modifier.brush(it)
            } ?: modifier
        }
        Icon(
            painter = painterResource(id = IR.drawable.person_outline),
            contentDescription = null,
            tint = MaterialTheme.theme.colors.primaryInteractive01,
            modifier = personModifier
        )

        Icon(
            painter = painterResource(id = IR.drawable.person_outline_check),
            contentDescription = null,
            modifier = Modifier
                .offset(x = 32.dp, y = 32.dp)
                .brush(
                    Brush.horizontalGradient(
                        0f to Color(0xFF78D549),
                        1F to Color(0xFF9BE45E)
                    )
                )
        )
    }
}

@Preview
@Composable
private fun OnboardingWelcomePagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        Content(
            isSignedInAsPlus = false,
            onContinueToDiscover = {},
            onImportTapped = {},
            state = OnboardingWelcomeState(newsletter = false),
            onDone = {},
            onNewsletterCheckedChanged = {},
        )
    }
}

@Preview
@Composable
private fun OnboardingWelcomePagePlusPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        Content(
            isSignedInAsPlus = true,
            onContinueToDiscover = {},
            onImportTapped = {},
            state = OnboardingWelcomeState(newsletter = false),
            onDone = {},
            onNewsletterCheckedChanged = {},
        )
    }
}
