package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.components.rememberViewInteropNestedScrollConnection
import au.com.shiftyjelly.pocketcasts.compose.pocketRed
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.profile.winback.FailureReason.Default
import au.com.shiftyjelly.pocketcasts.profile.winback.FailureReason.NoOrderId
import au.com.shiftyjelly.pocketcasts.profile.winback.FailureReason.NoProducts
import au.com.shiftyjelly.pocketcasts.profile.winback.FailureReason.NoPurchases
import au.com.shiftyjelly.pocketcasts.profile.winback.FailureReason.TooManyProducts
import au.com.shiftyjelly.pocketcasts.profile.winback.FailureReason.TooManyPurchases
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AvailablePlansPage(
    plansState: SubscriptionPlansState,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onGoToSubscriptions: () -> Unit,
    onReload: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = plansState,
        modifier = modifier,
    ) { state ->
        when (state) {
            is SubscriptionPlansState.Loading -> LoadingState()

            is SubscriptionPlansState.Failure -> when (state.reason) {
                TooManyPurchases, TooManyProducts -> TooManyPurchasesState(
                    onGoToSubscriptions = onGoToSubscriptions,
                )
                NoPurchases, NoProducts, NoOrderId, Default -> ErrorState(
                    onReload = onReload,
                )
            }

            is SubscriptionPlansState.Loaded -> LoadedState(
                userPlanId = state.activePurchase.productId,
                plans = state.plans,
                onSelectPlan = onSelectPlan,
            )
        }
    }
}

@Composable
private fun LoadedState(
    userPlanId: String?,
    plans: List<SubscriptionPlan>,
    onSelectPlan: (SubscriptionPlan) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .nestedScroll(rememberViewInteropNestedScrollConnection())
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            modifier = Modifier.height(64.dp),
        )
        PocketCastsLogo()
        Spacer(
            modifier = Modifier.height(20.dp),
        )
        Text(
            text = stringResource(LR.string.winback_available_plans_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 38.5.sp,
            color = MaterialTheme.theme.colors.primaryText01,
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        plans.forEach { plan ->
            SubscriptionRow(
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
}

@Composable
private fun TooManyPurchasesState(
    onGoToSubscriptions: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(
            modifier = Modifier.height(64.dp),
        )
        PocketCastsLogo()
        Spacer(
            modifier = Modifier.height(20.dp),
        )
        Text(
            text = stringResource(LR.string.winback_too_many_subscritpions_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 38.5.sp,
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
            .padding(horizontal = 48.dp),
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
private fun SubscriptionRow(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = when (MaterialTheme.theme.type) {
                    ThemeType.INDIGO -> Color.White
                    else -> MaterialTheme.theme.colors.primaryUi01
                },
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.theme.colors.primaryField03Active,
                        shape = RoundedCornerShape(8.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = !isSelected,
                onClick = onClick,
                role = Role.Button,
                indication = ripple(color = MaterialTheme.theme.colors.primaryIcon01),
                interactionSource = null,
            )
            .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            TextH30(text = plan.title)
            TextP40(text = plan.price())
        }
        CheckMark(
            isSelected = isSelected,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CheckMark(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isSelected) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(
                color = MaterialTheme.theme.colors.primaryField03Active,
                shape = CircleShape,
            ),
        ) {
            Image(
                painter = painterResource(IR.drawable.ic_checkmark_small),
                colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryUi01),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
            )
        }
    } else {
        Box(
            modifier = modifier.border(
                width = 1.dp,
                color = MaterialTheme.theme.colors.primaryIcon02,
                shape = CircleShape,
            ),
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

@Composable
@ReadOnlyComposable
private fun SubscriptionPlan.price() = when (billingPeriod) {
    BillingPeriod.Monthly -> stringResource(LR.string.plus_per_month, formattedPrice)
    BillingPeriod.Yearly -> stringResource(LR.string.plus_per_year, formattedPrice)
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun AvailablePlansPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(
        themeType = theme,
        backgroundColor = { MaterialTheme.theme.colors.primaryUi04 },
    ) {
        AvailablePlansPage(
            plansState = SubscriptionPlansState.Loaded(
                activePurchase = ActivePurchase(
                    orderId = "orderId",
                    productId = "plus.monthly",
                ),
                plans = listOf(
                    SubscriptionPlan(
                        productId = "plus.monthly",
                        offerToken = "",
                        title = "Plus Monthly",
                        formattedPrice = "$3.99",
                        billingPeriod = BillingPeriod.Monthly,
                    ),
                    SubscriptionPlan(
                        productId = "patron.monthly",
                        offerToken = "",
                        title = "Patron Monthly",
                        formattedPrice = "$9.99",
                        billingPeriod = BillingPeriod.Monthly,
                    ),
                    SubscriptionPlan(
                        productId = "plus.yearly",
                        offerToken = "",
                        title = "Plus Yearly",
                        formattedPrice = "$39.99",
                        billingPeriod = BillingPeriod.Yearly,
                    ),
                    SubscriptionPlan(
                        productId = "patron.yearly",
                        offerToken = "",
                        title = "Plus Yearly",
                        formattedPrice = "$99.99",
                        billingPeriod = BillingPeriod.Yearly,
                    ),
                ),
            ),
            onSelectPlan = {},
            onGoToSubscriptions = {},
            onReload = {},
            onGoBack = {},
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun AvailablePlansPageFailureTooManyPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(
        themeType = theme,
        backgroundColor = { MaterialTheme.theme.colors.primaryUi04 },
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

@Preview(device = Devices.PortraitRegular)
@Composable
private fun AvailablePlansPageFailureDefaultPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(
        themeType = theme,
        backgroundColor = { MaterialTheme.theme.colors.primaryUi04 },
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
