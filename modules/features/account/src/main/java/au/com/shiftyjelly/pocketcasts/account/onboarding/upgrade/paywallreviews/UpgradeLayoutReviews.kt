package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.paywallreviews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.PlusUpgradeLayoutReviewsItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.SubscribeButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun UpgradeLayoutReviews(
    state: OnboardingUpgradeFeaturesState.Loaded,
    onNotNowPressed: () -> Unit,
    onClickSubscribe: () -> Unit,
    canUpgrade: Boolean,
    modifier: Modifier = Modifier,
) {
    AppTheme(Theme.ThemeType.DARK) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .background(color = Color.Black),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 40.dp, top = 32.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            RowTextButton(
                                text = stringResource(R.string.not_now),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = Color.Transparent,
                                    contentColor = Color.White,
                                ),
                                fontSize = 18.sp,
                                onClick = onNotNowPressed,
                                fullWidth = false,
                                includePadding = false,
                            )
                        }
                    }

                    item {
                        TextH20(
                            text = stringResource(LR.string.paywall_layout_reviews_title),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 8.dp)
                                .fillMaxWidth(),
                        )

                        TextP50(
                            text = stringResource(LR.string.paywall_layout_reviews_subtitle),
                            fontWeight = FontWeight.W400,
                            color = colorResource(UR.color.coolgrey_50),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 40.dp)
                                .padding(bottom = 16.dp)
                                .fillMaxWidth(),
                        )
                    }

                    item {
                        PlusBenefits(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 24.dp),
                        )
                    }

                    item {
                        Stars(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 24.dp),
                        )
                    }
                }

                if (canUpgrade) {
                    SubscribeButton(state.currentSubscription, onClickSubscribe)
                }
            }
        }
    }
}

@Composable
private fun PlusBenefits(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            elevation = 8.dp,
            backgroundColor = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .border(
                    width = 3.dp,
                    brush = plusGradientBrush,
                    shape = RoundedCornerShape(10.dp),
                ),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
            ) {
                PlusUpgradeLayoutReviewsItem.entries.forEach {
                    UpgradeFeatureItem(item = it, color = Color.White)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset(y = (-10).dp)
                .padding(vertical = 8.dp),
        ) {
            SubscriptionBadge(
                fontSize = 16.sp,
                padding = 8.dp,
                iconRes = IR.drawable.ic_plus,
                shortNameRes = LR.string.pocket_casts_plus_short,
                iconColor = Color.Black,
                backgroundBrush = plusGradientBrush,
                textColor = Color.Black,
            )
        }
    }
}

@Composable
fun Stars(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(IR.drawable.stars),
            contentDescription = stringResource(LR.string.paywall_layout_reviews_stars_content_description),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .size(width = 150.dp, height = 26.dp),
        )

        TextH40(
            text = stringResource(LR.string.paywall_layout_reviews_rating),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
fun PlusBenefitsPreview() {
    PlusBenefits()
}

@Preview
@Composable
fun StarsPreview() {
    Stars()
}
