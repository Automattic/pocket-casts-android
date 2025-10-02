package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.AvailablePlanRow
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.bars.BottomSheetAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.ProgressDialog
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.components.rememberViewInteropNestedScrollConnection
import au.com.shiftyjelly.pocketcasts.compose.pocketRed
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.AcknowledgedSubscription
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.profile.winback.FailureReason.Default
import au.com.shiftyjelly.pocketcasts.profile.winback.FailureReason.NoPurchases
import au.com.shiftyjelly.pocketcasts.profile.winback.FailureReason.TooManyPurchases
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AvailablePlansPage(
    plansState: SubscriptionPlansState,
    onSelectPlan: (SubscriptionPlan.Base) -> Unit,
    onGoToSubscriptions: () -> Unit,
    onReload: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.nestedScroll(rememberViewInteropNestedScrollConnection()),
    ) {
        BottomSheetAppBar(
            onNavigationClick = onGoBack,
        )
        AnimatedContent(
            targetState = plansState,
            contentKey = { state -> state.javaClass },
            modifier = Modifier,
        ) { state ->
            when (state) {
                is SubscriptionPlansState.Loading -> LoadingState()

                is SubscriptionPlansState.Failure -> when (state.reason) {
                    TooManyPurchases -> TooManyPurchasesState(
                        onGoToSubscriptions = onGoToSubscriptions,
                    )

                    NoPurchases, Default -> ErrorState(
                        onReload = onReload,
                    )
                }

                is SubscriptionPlansState.Loaded -> LoadedState(
                    userPlanId = state.currentSubscription.productId,
                    plans = state.basePlans,
                    isChangingPlan = state.isChangingPlan,
                    onSelectPlan = onSelectPlan,
                )
            }
        }
    }
}

@Composable
private fun LoadedState(
    userPlanId: String?,
    plans: List<SubscriptionPlan.Base>,
    isChangingPlan: Boolean,
    onSelectPlan: (SubscriptionPlan.Base) -> Unit,
) {
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            PocketCastsLogo()
            Spacer(
                modifier = Modifier.height(20.dp),
            )
            Text(
                text = stringResource(LR.string.winback_available_plans_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 33.5.sp,
                color = MaterialTheme.theme.colors.primaryText01,
                textAlign = TextAlign.Center,
            )
            Spacer(
                modifier = Modifier.height(24.dp),
            )
            plans.forEach { plan ->
                AvailablePlanRow(
                    plan = plan,
                    isSelected = plan.productId == userPlanId,
                    onClick = { onSelectPlan(plan) },
                )
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
            }
            TextP50(
                text = stringResource(LR.string.winback_available_plans_billing_note),
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 36.dp),
            )
        }
        if (isChangingPlan) {
            ProgressDialog(
                text = stringResource(LR.string.winback_changing_plan),
                onDismiss = {},
            )
        }
    }
}

@Composable
private fun TooManyPurchasesState(
    onGoToSubscriptions: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        PocketCastsLogo()
        Spacer(
            modifier = Modifier.height(20.dp),
        )
        Text(
            text = stringResource(LR.string.winback_too_many_subscritpions_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 33.5.sp,
            color = MaterialTheme.theme.colors.primaryText01,
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        TextP50(
            text = stringResource(LR.string.winback_too_many_subscritpions_note),
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        ManageSubscriptions(
            onClick = onGoToSubscriptions,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(
            modifier = Modifier.height(52.dp),
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.theme.colors.secondaryIcon01,
        )
    }
}

@Composable
private fun ErrorState(
    onReload: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            modifier = Modifier.weight(3f),
        )
        Image(
            painter = painterResource(IR.drawable.ic_warning),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon03),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextH40(
            text = stringResource(LR.string.winback_error_fetch_description),
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        Button(
            onClick = onReload,
            shape = RoundedCornerShape(percent = 100),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.theme.colors.primaryInteractive03,
            ),
        ) {
            TextP40(
                text = stringResource(LR.string.try_again),
            )
        }
        Spacer(
            modifier = Modifier.weight(5f),
        )
    }
}

@Composable
private fun PocketCastsLogo(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(80.dp)
            .background(Color.pocketRed, RoundedCornerShape(16.dp)),
    ) {
        Image(
            painter = painterResource(IR.drawable.ic_logo_foreground),
            contentDescription = stringResource(LR.string.pocket_casts_logo),
            modifier = Modifier.size(52.dp),
        )
    }
}

@Composable
private fun ManageSubscriptions(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        TextH30(
            text = stringResource(LR.string.winback_too_many_subscritpions_button_label),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun AvailablePlansPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(
        themeType = theme,
    ) {
        AvailablePlansPage(
            plansState = SubscriptionPlansState.Loaded(
                currentSubscription = AcknowledgedSubscription(
                    orderId = "orderId",
                    tier = SubscriptionTier.Plus,
                    billingCycle = BillingCycle.Yearly,
                    isAutoRenewing = true,
                ),
                subscriptionPlans = SubscriptionPlans.Preview,
            ),
            onSelectPlan = {},
            onGoToSubscriptions = {},
            onReload = {},
            onGoBack = {},
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun AvailablePlansPageFailureTooManyPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(
        themeType = theme,
    ) {
        AvailablePlansPage(
            plansState = SubscriptionPlansState.Failure(TooManyPurchases),
            onSelectPlan = {},
            onGoToSubscriptions = {},
            onReload = {},
            onGoBack = {},
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun AvailablePlansPageFailureDefaultPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(
        themeType = theme,
    ) {
        AvailablePlansPage(
            plansState = SubscriptionPlansState.Failure(Default),
            onSelectPlan = {},
            onGoToSubscriptions = {},
            onReload = {},
            onGoBack = {},
        )
    }
}
