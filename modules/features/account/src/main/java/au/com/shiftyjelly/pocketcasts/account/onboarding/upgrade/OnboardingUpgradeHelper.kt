package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.GradientRowButton
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.Clickable
import au.com.shiftyjelly.pocketcasts.compose.components.ClickableTextHelper
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.extensions.brush
import au.com.shiftyjelly.pocketcasts.compose.patronGradientBrush
import au.com.shiftyjelly.pocketcasts.compose.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

object OnboardingUpgradeHelper {

    private val unselectedColor = Color(0xFF666666)

    @Composable
    fun UpgradeRowButton(
        primaryText: String,
        textColor: Color,
        backgroundColor: Color,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        fontWeight: FontWeight = FontWeight.W600,
        secondaryText: String? = null,
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(backgroundColor, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AutoResizeText(
                    text = primaryText,
                    color = textColor,
                    maxFontSize = 18.sp,
                    lineHeight = 21.sp,
                    fontWeight = fontWeight,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                secondaryText?.let {
                    TextP60(
                        text = it,
                        textAlign = TextAlign.Center,
                        color = textColor,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }

    @Composable
    fun UpgradeRowButton(
        primaryText: String,
        textColor: Color,
        onClick: () -> Unit,
        gradientBackgroundColor: Brush,
        modifier: Modifier = Modifier,
        fontWeight: FontWeight = FontWeight.W600,
        secondaryText: String? = null,
    ) {
        GradientRowButton(
            primaryText = primaryText,
            secondaryText = secondaryText,
            fontWeight = fontWeight,
            textColor = textColor,
            gradientBackgroundColor = gradientBackgroundColor,
            onClick = onClick,
            modifier = modifier,
        )
    }

    @Composable
    fun OutlinedRowButton(
        text: String,
        brush: Brush,
        subscriptionTier: SubscriptionTier,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        topText: String? = null,
        selectedCheckMark: Boolean = false,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    ) {
        ConstraintLayout(modifier) {
            val buttonRef = createRef()
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, brush),
                elevation = null,
                interactionSource = interactionSource,
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                modifier = Modifier.constrainAs(buttonRef) {
                    bottom.linkTo(parent.bottom)
                },
            ) {
                Box(Modifier.fillMaxWidth()) {
                    TextH30(
                        text = text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .brush(brush)
                            .padding(vertical = 6.dp, horizontal = 24.dp)
                            .align(Alignment.Center),
                    )
                    if (selectedCheckMark) {
                        Icon(
                            painter = painterResource(IR.drawable.plus_check),
                            contentDescription = null,
                            modifier = Modifier
                                .brush(brush)
                                .align(Alignment.CenterEnd)
                                .width(24.dp),
                        )
                    }
                }
            }

            topText?.let {
                ConstrainedTopText(
                    buttonRef = buttonRef,
                    topText = it,
                    subscriptionTier = subscriptionTier,
                    isSelected = true,
                )
            }
        }
    }

    @Composable
    fun UnselectedOutlinedRowButton(
        text: String,
        onClick: () -> Unit,
        subscriptionTier: SubscriptionTier,
        modifier: Modifier = Modifier,
        topText: String? = null,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    ) {
        ConstraintLayout(modifier) {
            val buttonRef = createRef()
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, unselectedColor),
                elevation = null,
                interactionSource = interactionSource,
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                modifier = Modifier.constrainAs(buttonRef) {
                    bottom.linkTo(parent.bottom)
                },
            ) {
                TextH30(
                    text = text,
                    textAlign = TextAlign.Center,
                    color = unselectedColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 24.dp),
                )
            }

            topText?.let {
                ConstrainedTopText(
                    buttonRef = buttonRef,
                    topText = it,
                    isSelected = false,
                    subscriptionTier = subscriptionTier,
                )
            }
        }
    }

    @Composable
    fun TopText(
        topText: String,
        subscriptionTier: SubscriptionTier,
        modifier: Modifier = Modifier,
        selected: Boolean = true,
    ) {
        val brush = when (subscriptionTier) {
            SubscriptionTier.Plus -> Brush.plusGradientBrush
            SubscriptionTier.Patron -> Brush.patronGradientBrush
        }
        val textColor = when (subscriptionTier) {
            SubscriptionTier.Plus -> Color.Black
            SubscriptionTier.Patron -> Color.White
        }
        Box(
            modifier = if (selected) {
                modifier.background(
                    brush = brush,
                    shape = RoundedCornerShape(12.dp),
                )
            } else {
                modifier.background(
                    color = unselectedColor,
                    shape = RoundedCornerShape(12.dp),
                )
            },
        ) {
            TextP60(
                text = topText,
                color = textColor,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 2.dp,
                ),
            )
        }
    }

    @Composable
    private fun ConstraintLayoutScope.ConstrainedTopText(
        buttonRef: ConstrainedLayoutReference,
        topText: String,
        subscriptionTier: SubscriptionTier,
        isSelected: Boolean,
    ) {
        val topTextRef = createRef()
        val topTextModifier = Modifier
            .constrainAs(topTextRef) {
                top.linkTo(buttonRef.top)
                bottom.linkTo(buttonRef.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        TopText(
            topText = topText,
            subscriptionTier = subscriptionTier,
            selected = isSelected,
            modifier = topTextModifier,
        )
    }

    val backgroundColor = Color(0xFF121212)

    @Composable
    fun UpgradeBackground(
        tier: SubscriptionTier,
        @DrawableRes backgroundGlowsRes: Int,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Box(modifier) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (tier) {
                    SubscriptionTier.Plus -> PlusBlurredCanvasBackground()
                    SubscriptionTier.Patron -> PatronBlurredCanvasBackground()
                }
            } else {
                ImageBackground(backgroundGlowsRes)
            }
            content()
        }
    }

    @Composable
    @RequiresApi(Build.VERSION_CODES.S) // Blur only works on Android >=12
    private fun BoxScope.PlusBlurredCanvasBackground() {
        val width = LocalConfiguration.current.screenWidthDp.dp
        Canvas(
            Modifier
                .matchParentSize()
                .blur(width * 0.8f),
        ) {
            drawRect(backgroundColor)

            withTransform({ rotate(6f) }) {
                drawOval(
                    color = Color(0xFFD4B43A),
                    topLeft = Offset(-size.width * 2.4f, 0f),
                    size = Size(size.width * 3f, size.height * 1.3f),
                    alpha = 0.66f,
                    blendMode = BlendMode.SrcOver,
                )
            }
        }
    }

    @Composable
    @RequiresApi(Build.VERSION_CODES.S) // Blur only works on Android >=12
    private fun BoxScope.PatronBlurredCanvasBackground() {
        val width = LocalConfiguration.current.screenWidthDp.dp
        Canvas(
            Modifier
                .matchParentSize()
                .blur(width * 0.5f),
        ) {
            drawRect(backgroundColor)

            withTransform({ rotate(6f) }) {
                drawOval(
                    color = Color(0xFF402EA3),
                    topLeft = Offset(-size.width * 1.5f, size.height * .35f),
                    size = Size(size.width * 2f, size.width * 1.8f),
                    blendMode = BlendMode.SrcOver,
                )
            }
        }
    }

    @Composable
    private fun BoxScope.ImageBackground(
        @DrawableRes backgroundGlowsRes: Int,
    ) {
        Image(
            painterResource(backgroundGlowsRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .background(backgroundColor)
                .matchParentSize(),
        )
    }

    @Composable
    fun PrivacyPolicy(
        color: Color,
        textAlign: TextAlign,
        modifier: Modifier = Modifier,
        lineHeight: TextUnit = 16.sp,
        onPrivacyPolicyClick: () -> Unit = {},
        onTermsAndConditionsClick: () -> Unit = {},
    ) {
        val privacyPolicyText = stringResource(LR.string.onboarding_plus_privacy_policy)
        val termsAndConditionsText = stringResource(LR.string.onboarding_plus_terms_and_conditions)
        val text = stringResource(
            LR.string.onboarding_plus_continuing_agrees_to,
            privacyPolicyText,
            termsAndConditionsText,
        )
        val uriHandler = LocalUriHandler.current
        ClickableTextHelper(
            text = text,
            color = color,
            lineHeight = lineHeight,
            textAlign = textAlign,
            clickables = listOf(
                Clickable(
                    text = privacyPolicyText,
                    onClick = {
                        onPrivacyPolicyClick()
                        uriHandler.openUri(Settings.INFO_PRIVACY_URL)
                    },
                ),
                Clickable(
                    text = termsAndConditionsText,
                    onClick = {
                        onTermsAndConditionsClick()
                        uriHandler.openUri(Settings.INFO_TOS_URL)
                    },
                ),
            ),
            modifier = modifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UpgradeRowButtonWithGradientBackgroundPreview() {
    UpgradeRowButton(
        primaryText = "Upgrade Now",
        textColor = Color.Black,
        onClick = {},
        gradientBackgroundColor = Brush.plusGradientBrush,
    )
}

@Preview(showBackground = true)
@Composable
private fun UpgradeRowButtonPreview() {
    UpgradeRowButton(
        primaryText = "Upgrade Now",
        textColor = Color.Black,
        onClick = {},
        backgroundColor = colorResource(UR.color.plus_gold),
    )
}
