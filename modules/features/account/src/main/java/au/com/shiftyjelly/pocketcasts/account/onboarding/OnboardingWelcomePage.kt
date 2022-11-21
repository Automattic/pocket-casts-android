package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingWelcomeViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.extensions.brush
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingWelcomePage(
    onContinue: () -> Unit,
    isSignedInAsPlus: Boolean,
) {

    val viewModel = hiltViewModel<OnboardingWelcomeViewModel>()
    val state by viewModel.stateFlow.collectAsState()

    @Suppress("NAME_SHADOWING")
    val onContinue = {
        viewModel.persistNewsletterSelection()
        onContinue()
    }

    BackHandler {
        onContinue()
    }

    Column {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 100.dp)
        ) {

            if (isSignedInAsPlus) {
                PlusPersonCheckmark()
            } else {
                PersonCheckmark()
            }

            Spacer(Modifier.height(8.dp))
            TextH20(
                text = stringResource(
                    if (isSignedInAsPlus) {
                        LR.string.onboarding_welcome_get_you_listening_plus
                    } else {
                        LR.string.onboarding_welcome_get_you_listening
                    }
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(2f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp, end = 32.dp)
        ) {
            Column(Modifier.padding(end = 16.dp)) {
                TextH30(stringResource(LR.string.onboarding_get_the_newsletter))
                TextP60(stringResource(LR.string.profile_create_newsletter_summary))
            }

            Switch(
                checked = state.newsletter,
                onCheckedChange = viewModel::updateNewsletter,
                colors = SwitchDefaults.colors(
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Gray,
                )
            )
        }

        Spacer(Modifier.height(16.dp))
        RowButton(
            text = stringResource(LR.string.done),
            onClick = onContinue,
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
        OnboardingWelcomePage(
            onContinue = {},
            isSignedInAsPlus = false
        )
    }
}

@Preview
@Composable
private fun OnboardingWelcomePagePlusPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        OnboardingWelcomePage(
            onContinue = {},
            isSignedInAsPlus = true
        )
    }
}
