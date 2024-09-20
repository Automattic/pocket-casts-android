package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.paywallreviews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.rateUs
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
    data: List<ReviewData> = reviews,
) {
    val context = LocalContext.current

    AppTheme(Theme.ThemeType.DARK) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .background(color = Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
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

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .verticalFadingEdge(color = Color.Black, width = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    TextH20(
                        text = stringResource(LR.string.paywall_layout_reviews_title),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 8.dp, top = 8.dp)
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

                items(
                    count = data.size,
                    key = { index -> index },
                ) { index ->
                    ReviewItem(
                        data = data[index],
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp),
                    )
                }

                item {
                    Button(
                        onClick = {
                            rateUs(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .padding(top = 20.dp, bottom = 32.dp),
                    ) {
                        TextP60(
                            text = stringResource(LR.string.paywall_layout_reviews_see_all_reviews_in_store),
                            fontWeight = FontWeight.W400,
                            color = colorResource(UR.color.plus_gold_dark),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (canUpgrade) {
                SubscribeButton(state.currentSubscription, onClickSubscribe)
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
            painter = painterResource(IR.drawable.stars_gradient),
            contentDescription = stringResource(LR.string.paywall_layout_reviews_stars_content_description),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .size(width = 157.dp, height = 29.dp),
        )

        TextH40(
            text = stringResource(LR.string.paywall_layout_reviews_rating),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ReviewItem(
    data: ReviewData,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .background(colorResource(UR.color.darkgrey_60))
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextH50(
                    text = stringResource(data.titleResourceId),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                TextP50(
                    text = stringResource(data.dateResourceId),
                    fontWeight = FontWeight.W400,
                    color = colorResource(UR.color.coolgrey_60),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Image(
                painter = painterResource(IR.drawable.stars),
                contentDescription = stringResource(LR.string.paywall_layout_reviews_stars_content_description),
            )

            Spacer(modifier = Modifier.height(6.dp))

            TextP50(
                text = stringResource(data.messageResourceId),
                color = Color.White,
            )
        }
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

@Preview
@Composable
fun ReviewItemPreview() {
    ReviewItem(
        data = ReviewData(
            titleResourceId = LR.string.paywall_layout_reviews_review_five_title,
            messageResourceId = LR.string.paywall_layout_reviews_review_five_message,
            dateResourceId = LR.string.paywall_layout_reviews_review_five_date,
        ),
    )
}

enum class FadeSide {
    BOTTOM, TOP
}

fun Modifier.verticalFadingEdge(
    color: Color,
    width: Dp,
) = fadingEdge(FadeSide.BOTTOM, FadeSide.TOP, color = color, width = width)

fun Modifier.fadingEdge(
    vararg sides: FadeSide,
    color: Color,
    width: Dp,
) = drawWithContent {
    this@drawWithContent.drawContent()

    sides.forEach { side ->
        val (start, end) = when (side) {
            FadeSide.BOTTOM -> Offset(0f, this.size.height) to Offset.Zero
            FadeSide.TOP -> Offset.Zero to Offset(0f, this.size.height)
        }

        val fraction = width.toPx() / this.size.height

        drawRect(
            brush = Brush.linearGradient(
                0f to color,
                fraction to Color.Transparent,
                start = start,
                end = end,
            ),
            size = this.size,
        )
    }
}
