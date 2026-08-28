package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvModal
import au.com.shiftyjelly.pocketcasts.component.TvModalButton
import au.com.shiftyjelly.pocketcasts.component.TvModalSurface
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.qr.rememberQrPainter
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import java.time.Instant
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvSettingsModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var subScreen by remember { mutableStateOf<TvSettingsSubScreen?>(null) }

    LaunchedEffect(subScreen) {
        when (subScreen) {
            TvSettingsSubScreen.Subscription -> viewModel.trackSubscriptionShown()
            TvSettingsSubScreen.PrivacyPolicy -> viewModel.trackPrivacyPolicyShown()
            TvSettingsSubScreen.TermsOfUse -> viewModel.trackTermsOfUseShown()
            null -> Unit
        }
    }

    TvModal(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        LaunchedEffect(Unit) {
            viewModel.trackSettingsShown()
        }
        TvSettingsMenuContent(
            isSignedIn = uiState.isSignedIn,
            useEpisodeArtwork = uiState.useEpisodeArtwork,
            onSubscription = { subScreen = TvSettingsSubScreen.Subscription },
            onPrivacyPolicy = { subScreen = TvSettingsSubScreen.PrivacyPolicy },
            onTermsOfUse = { subScreen = TvSettingsSubScreen.TermsOfUse },
            onToggleEpisodeArtwork = { viewModel.setUseEpisodeArtwork(!uiState.useEpisodeArtwork) },
        )
    }

    when (subScreen) {
        TvSettingsSubScreen.Subscription -> TvSubscriptionInfoModal(
            subscription = uiState.subscription,
            onDismissRequest = { subScreen = null },
        )

        TvSettingsSubScreen.PrivacyPolicy -> TvQrLinkModal(
            title = stringResource(LR.string.profile_privacy_policy),
            message = stringResource(LR.string.tv_settings_privacy_policy_qr_message),
            url = Settings.INFO_PRIVACY_URL,
            onDismissRequest = { subScreen = null },
        )

        TvSettingsSubScreen.TermsOfUse -> TvQrLinkModal(
            title = stringResource(LR.string.profile_terms_of_use),
            message = stringResource(LR.string.tv_settings_terms_of_use_qr_message),
            url = Settings.INFO_TOS_URL,
            onDismissRequest = { subScreen = null },
        )

        null -> Unit
    }
}

private enum class TvSettingsSubScreen {
    Subscription,
    PrivacyPolicy,
    TermsOfUse,
}

@Composable
private fun ColumnScope.TvSettingsMenuContent(
    isSignedIn: Boolean,
    useEpisodeArtwork: Boolean,
    onSubscription: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTermsOfUse: () -> Unit,
    onToggleEpisodeArtwork: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSignedIn) {
        focusRequester.requestFocus()
    }

    if (isSignedIn) {
        TvModalButton(
            text = stringResource(LR.string.tv_settings_subscription_title),
            onClick = onSubscription,
            modifier = Modifier.focusRequester(focusRequester),
        )
    }
    TvModalButton(
        text = stringResource(LR.string.profile_privacy_policy),
        onClick = onPrivacyPolicy,
        modifier = if (isSignedIn) Modifier else Modifier.focusRequester(focusRequester),
    )
    TvModalButton(
        text = stringResource(LR.string.profile_terms_of_use),
        onClick = onTermsOfUse,
    )
    TvSettingsDivider()
    TvSettingsToggleRow(
        label = stringResource(LR.string.settings_use_episode_artwork),
        checked = useEpisodeArtwork,
        onClick = onToggleEpisodeArtwork,
    )
}

@Composable
private fun TvSettingsToggleRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = TvButtonDefaults.filledButtonColors(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.tvTypography.caption1,
                modifier = Modifier.weight(1f),
            )
            if (checked) {
                Icon(
                    painter = painterResource(IR.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun TvSettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.tvColors.overlayBorder),
    )
}

@Composable
private fun TvSubscriptionInfoModal(
    subscription: Subscription?,
    onDismissRequest: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    TvModal(onDismissRequest = onDismissRequest) {
        Text(
            text = stringResource(LR.string.tv_settings_subscription_title),
            color = MaterialTheme.tvColors.textPrimary,
            style = MaterialTheme.tvTypography.headline.copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
        if (subscription == null) {
            Text(
                text = stringResource(LR.string.profile_free_account),
                color = MaterialTheme.tvColors.textSecondary,
                style = MaterialTheme.tvTypography.caption1.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TvSettingsInfoRow(
                label = stringResource(LR.string.tv_settings_subscription_plan),
                value = subscriptionPlanText(subscription),
            )
            TvSettingsInfoRow(
                label = subscriptionRenewalLabel(subscription),
                value = subscriptionRenewalText(subscription),
            )
            if (subscription.isManagedOnAnotherPlatform) {
                Text(
                    text = stringResource(LR.string.tv_settings_subscription_other_platform),
                    color = MaterialTheme.tvColors.textSecondary,
                    style = MaterialTheme.tvTypography.caption1.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        TvModalButton(
            text = stringResource(LR.string.done),
            onClick = onDismissRequest,
            modifier = Modifier.focusRequester(focusRequester),
        )
    }
}

private val Subscription.isManagedOnAnotherPlatform: Boolean
    get() = platform == SubscriptionPlatform.iOS || platform == SubscriptionPlatform.Web

@Composable
private fun TvSettingsInfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = MaterialTheme.tvColors.textSecondary,
            style = MaterialTheme.tvTypography.body,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = MaterialTheme.tvColors.textPrimary,
            style = MaterialTheme.tvTypography.body.copy(textAlign = TextAlign.End),
        )
    }
}

@Composable
private fun subscriptionPlanText(subscription: Subscription): String {
    val tier = when (subscription.tier) {
        SubscriptionTier.Plus -> stringResource(LR.string.pocket_casts_plus_short)
        SubscriptionTier.Patron -> stringResource(LR.string.pocket_casts_patron_short)
    }
    val cycle = when (subscription.billingCycle) {
        BillingCycle.Monthly -> stringResource(LR.string.profile_monthly)
        BillingCycle.Yearly -> stringResource(LR.string.profile_yearly)
        null -> null
    }
    return listOfNotNull(tier, cycle).joinToString(separator = " ")
}

@Composable
private fun subscriptionRenewalLabel(subscription: Subscription): String = stringResource(
    when {
        subscription.isChampion || subscription.isAutoRenewing -> LR.string.tv_settings_subscription_next_renewal
        else -> LR.string.tv_settings_subscription_expires
    },
)

@Composable
private fun subscriptionRenewalText(subscription: Subscription): String {
    if (subscription.isChampion) {
        return stringResource(LR.string.tv_settings_subscription_lifetime)
    }
    return remember(subscription.expiryDate) {
        Date.from(subscription.expiryDate).toLocalizedFormatLongStyle()
    }
}

@Composable
private fun TvQrLinkModal(
    title: String,
    message: String,
    url: String,
    onDismissRequest: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    TvModal(onDismissRequest = onDismissRequest) {
        Text(
            text = title,
            color = MaterialTheme.tvColors.textPrimary,
            style = MaterialTheme.tvTypography.title2.copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = message,
            color = MaterialTheme.tvColors.textSecondary,
            style = MaterialTheme.tvTypography.body.copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
        TvQrCode(
            content = url,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            text = url,
            color = MaterialTheme.tvColors.textSecondary,
            style = MaterialTheme.tvTypography.caption1.copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
        TvModalButton(
            text = stringResource(LR.string.done),
            onClick = onDismissRequest,
            modifier = Modifier.focusRequester(focusRequester),
        )
    }
}

@Composable
private fun TvQrCode(content: String, modifier: Modifier = Modifier) {
    val qrPainter = rememberQrPainter(content = content, size = QrSize)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(10.dp),
    ) {
        Image(
            painter = qrPainter,
            contentDescription = null,
            modifier = Modifier.size(QrSize),
        )
    }
}

private val QrSize = 160.dp

@Preview
@Composable
private fun TvSettingsMenuSignedInPreview() {
    TvTheme {
        TvModalSurface {
            TvSettingsMenuContent(
                isSignedIn = true,
                useEpisodeArtwork = true,
                onSubscription = {},
                onPrivacyPolicy = {},
                onTermsOfUse = {},
                onToggleEpisodeArtwork = {},
            )
        }
    }
}

@Preview
@Composable
private fun TvSettingsMenuSignedOutPreview() {
    TvTheme {
        TvModalSurface {
            TvSettingsMenuContent(
                isSignedIn = false,
                useEpisodeArtwork = false,
                onSubscription = {},
                onPrivacyPolicy = {},
                onTermsOfUse = {},
                onToggleEpisodeArtwork = {},
            )
        }
    }
}

@Preview
@Composable
private fun TvSubscriptionInfoPreview() {
    TvTheme {
        TvModalSurface {
            TvSettingsInfoRow(
                label = stringResource(LR.string.tv_settings_subscription_plan),
                value = "Plus Yearly",
            )
            TvSettingsInfoRow(
                label = stringResource(LR.string.tv_settings_subscription_next_renewal),
                value = Date.from(Instant.now()).toLocalizedFormatLongStyle(),
            )
        }
    }
}
