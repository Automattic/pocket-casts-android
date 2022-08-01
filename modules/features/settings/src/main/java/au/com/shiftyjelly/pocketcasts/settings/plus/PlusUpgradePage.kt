package au.com.shiftyjelly.pocketcasts.settings.plus

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.VerticalLogoPlus
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.text.LinkText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralDays
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralMonths
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralYears
import au.com.shiftyjelly.pocketcasts.settings.R
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.UpgradeAccountViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.time.Period
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PlusUpgradePage(
    onCloseClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    featureBlocked: Boolean,
    storageLimitGb: Long,
    viewModel: UpgradeAccountViewModel,
) {
    val productState by viewModel.productState.observeAsState()
    PlusUpgradePageView(
        onCloseClick = onCloseClick,
        onUpgradeClick = onUpgradeClick,
        onLearnMoreClick = onLearnMoreClick,
        featureBlocked = featureBlocked,
        storageLimitGb = storageLimitGb,
        productState = productState?.get()
    )
}

@Composable
private fun PlusUpgradePageView(
    onCloseClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    featureBlocked: Boolean,
    storageLimitGb: Long,
    productState: UpgradeAccountViewModel.ProductState?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(MaterialTheme.theme.colors.primaryUi02)) {
        PlusInformation(
            storageLimitGb = storageLimitGb,
            productState = productState,
            onLearnMoreClick = onLearnMoreClick,
            featureBlocked = featureBlocked,
            modifier = modifier.weight(1f),
        )
        ButtonPanel(
            onUpgradeClick = onUpgradeClick,
            onCloseClick = onCloseClick,
            productState = productState
        )
    }
}

@Composable
fun ButtonPanel(
    onUpgradeClick: () -> Unit,
    onCloseClick: () -> Unit,
    productState: UpgradeAccountViewModel.ProductState?,
    modifier: Modifier = Modifier,
) {
    Surface(
        elevation = 8.dp,
        color = MaterialTheme.theme.colors.primaryUi02
    ) {
        Column {
            RowButton(
                text = stringResource(
                    if (productState?.trialBillingPeriod != null) {
                        LR.string.profile_start_free_trial
                    } else {
                        LR.string.profile_upgrade_to_plus
                    }
                ),
                onClick = onUpgradeClick,
                includePadding = false,
                modifier = modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            RowOutlinedButton(
                text = stringResource(LR.string.profile_create_tos_disagree),
                onClick = onCloseClick,
                includePadding = false,
                modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PlusInformation(
    storageLimitGb: Long,
    productState: UpgradeAccountViewModel.ProductState?,
    onLearnMoreClick: () -> Unit,
    featureBlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(vertical = 16.dp)
            .fillMaxWidth()
    ) {
        val context = LocalContext.current
        Spacer(modifier = modifier.height(10.dp))
        VerticalLogoPlus()
        Spacer(modifier = modifier.height(32.dp))
        TextH20(
            text =
            if (featureBlocked) {
                if (productState?.trialBillingPeriod != null) {
                    stringResource(
                        LR.string.profile_feature_try_trial,
                        getFormattedTrialPeriod(productState.trialBillingPeriod, context)
                    )
                } else {
                    stringResource(LR.string.profile_feature_requires)
                }
            } else {
                stringResource(LR.string.profile_help_support)
            },
            textAlign = TextAlign.Center
        )

        if (productState?.trialBillingPeriod != null) {
            TextH40(
                text = stringResource(LR.string.profile_feature_try_trial_secondary_info),
                modifier = modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = modifier.height(16.dp))
        if (storageLimitGb > 0) {
            PlusFeatureList(
                storageLimitGb = storageLimitGb
            )
            Spacer(modifier = modifier.height(8.dp))
        }
        LinkText(
            text = stringResource(LR.string.plus_learn_more_about_plus),
            onClick = onLearnMoreClick
        )
        Spacer(modifier = modifier.height(4.dp))
        if (productState?.price != null) {
            TextH40(
                text = if (productState.trialBillingPeriod != null) {
                    stringResource(
                        LR.string.plus_per_month_with_trial,
                        getFormattedTrialPeriod(productState.trialBillingPeriod, context),
                        productState.price
                    )
                } else {
                    stringResource(LR.string.plus_per_month, productState.price)
                },
                color = MaterialTheme.theme.colors.primaryText02
            )
            Spacer(modifier = modifier.height(6.dp))
        }
    }
}

@Composable
private fun PlusFeatureList(
    storageLimitGb: Long,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(end = 32.dp)) {
        PlusFeature(text = stringResource(id = LR.string.profile_web_player))
        PlusFeature(text = stringResource(id = LR.string.profile_extra_themes))
        PlusFeature(text = stringResource(id = LR.string.profile_extra_app_icons))
        PlusFeature(text = stringResource(id = LR.string.plus_cloud_storage_limit, storageLimitGb))
        PlusFeature(text = stringResource(id = LR.string.folders))
    }
}

@Composable
private fun PlusFeature(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 5.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            modifier = modifier.padding(end = 16.dp)
        )
        TextP40(
            text = text,
            color = MaterialTheme.theme.colors.primaryText02
        )
    }
}

private fun getFormattedTrialPeriod(
    trialBillingPeriod: Period,
    context: Context,
) = when {
    trialBillingPeriod.days > 0 -> context.resources.getStringPluralDays(trialBillingPeriod.days)
    trialBillingPeriod.months > 0 -> context.resources.getStringPluralMonths(trialBillingPeriod.months)
    trialBillingPeriod.years > 0 -> context.resources.getStringPluralYears(trialBillingPeriod.years)
    else -> context.resources.getStringPluralMonths(trialBillingPeriod.months)
}

@Preview(showBackground = true)
@Composable
private fun PlusUpgradePagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        PlusUpgradePageView(
            onCloseClick = {},
            onUpgradeClick = {},
            onLearnMoreClick = {},
            featureBlocked = true,
            storageLimitGb = 10L,
            productState = UpgradeAccountViewModel.ProductState(
                price = "$0.99",
                trialBillingPeriod = Period.ofMonths(1),
            )
        )
    }
}
