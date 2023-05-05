package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import au.com.shiftyjelly.pocketcasts.account.R
import au.com.shiftyjelly.pocketcasts.compose.components.Clickable
import au.com.shiftyjelly.pocketcasts.compose.components.ClickableTextHelper
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.extensions.brush
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object OnboardingUpgradeHelper {
    val plusGradientBrush = Brush.horizontalGradient(
        0f to Color(0xFFFED745),
        1f to Color(0xFFFEB525),
    )
    private val unselectedColor = Color(0xFF666666)

    @Composable
    fun UpgradeRowButton(
        primaryText: String,
        backgroundColor: Long,
        textColor: Long,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        secondaryText: String? = null,
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(backgroundColor),
            ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextH30(
                    text = primaryText,
                    color = Color(textColor),
                )
                secondaryText?.let { subTitle ->
                    TextP60(
                        text = subTitle,
                        color = Color(textColor),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun PlusRowButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(all = 0.dp), // Remove content padding
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(plusGradientBrush)
            ) {
                Text(
                    text = text,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(6.dp)
                        // add extra 8.dp extra padding to offset removal of button padding (see ButtonDefaults.ButtonVerticalPadding)
                        .padding(8.dp)
                        .align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            }
        }
    }

    @Composable
    fun PlusOutlinedRowButton(
        text: String,
        topText: String? = null,
        onClick: () -> Unit,
        selectedCheckMark: Boolean = false,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        modifier: Modifier = Modifier,
    ) {

        ConstraintLayout(modifier) {

            val buttonRef = createRef()
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, plusGradientBrush),
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
                            .padding(vertical = 6.dp, horizontal = 24.dp)
                            .align(Alignment.Center)
                            .brush(plusGradientBrush)
                    )
                    if (selectedCheckMark) {
                        Icon(
                            painter = painterResource(IR.drawable.plus_check),
                            contentDescription = null,
                            modifier = Modifier
                                .brush(plusGradientBrush)
                                .align(Alignment.CenterEnd)
                                .width(24.dp)
                        )
                    }
                }
            }

            topText?.let {
                ConstrainedTopText(
                    buttonRef = buttonRef,
                    topText = it,
                    isSelected = true
                )
            }
        }
    }

    @Composable
    fun UnselectedPlusOutlinedRowButton(
        text: String,
        topText: String? = null,
        onClick: () -> Unit,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        modifier: Modifier = Modifier,
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
                        .padding(vertical = 6.dp, horizontal = 24.dp)
                )
            }

            topText?.let {
                ConstrainedTopText(
                    buttonRef = buttonRef,
                    topText = it,
                    isSelected = false
                )
            }
        }
    }

    @Composable
    fun TopText(
        topText: String,
        modifier: Modifier = Modifier,
        selected: Boolean = true,
    ) {
        Box(
            modifier = if (selected) {
                modifier.background(
                    brush = plusGradientBrush,
                    shape = RoundedCornerShape(4.dp)
                )
            } else {
                modifier.background(
                    color = unselectedColor,
                    shape = RoundedCornerShape(4.dp)
                )
            }
        ) {
            TextP60(
                text = topText,
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 2.dp
                ),
            )
        }
    }

    @Composable
    private fun ConstraintLayoutScope.ConstrainedTopText(
        buttonRef: ConstrainedLayoutReference,
        topText: String,
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
            selected = isSelected,
            modifier = topTextModifier
        )
    }

    val backgroundColor = Color(0xFF121212)

    @Composable
    fun OldPlusBackground(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Box(modifier) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                OldBlurredCanvasBackground()
            } else {
                ImageBackground(R.drawable.upgrade_background_glows)
            }
            content()
        }
    }

    @Composable
    fun UpgradeBackground(
        modifier: Modifier = Modifier,
        @StringRes shortNamRes: Int,
        @DrawableRes backgroundGlowsRes: Int,
        content: @Composable () -> Unit,
    ) {
        Box(modifier) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (shortNamRes) {
                    LR.string.pocket_casts_plus_short -> PlusBlurredCanvasBackground()
                    LR.string.pocket_casts_patron_short -> PatronBlurredCanvasBackground()
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
        Canvas(
            Modifier
                .matchParentSize()
                .blur(420.dp)
        ) {
            // Background
            drawRect(backgroundColor)

            drawOval(
                color = Color(0xFFFFD846),
                topLeft = Offset(0f, size.height * .3f),
                size = Size(size.width * 3.5f, size.height),
                alpha = 0.8f,
                blendMode = BlendMode.SrcOver,
            )

            drawCircle(
                color = Color(0xFFD4B43A),
                radius = size.width * 1.5f,
                center = Offset(-size.width, size.height * 1.3f),
            )

            // Overlay
            drawRect(Color(0xFF121212), alpha = 0.28f)
        }
    }

    @Composable
    @RequiresApi(Build.VERSION_CODES.S) // Blur only works on Android >=12
    private fun BoxScope.PatronBlurredCanvasBackground() {
        Canvas(
            Modifier
                .matchParentSize()
                .blur(173.dp)
        ) {
            // Background
            drawRect(backgroundColor)

            drawOval(
                color = Color(0xFF503ACC),
                topLeft = Offset(0f, size.height * .25f),
                size = Size(size.width * 2f, size.height * .6f),
                alpha = 0.9f,
                blendMode = BlendMode.SrcOver,
            )

            drawCircle(
                color = Color(0xFF402EA3),
                radius = size.width * 0.9f,
                center = Offset(-size.width / 2f, size.height * .7f),
            )
        }
    }

    @Composable
    @RequiresApi(Build.VERSION_CODES.S) // Blur only works on Android >=12
    private fun BoxScope.OldBlurredCanvasBackground() {
        val screenHeight = LocalConfiguration.current.screenHeightDp
        Canvas(
            Modifier
                .matchParentSize()
                .blur(150.dp)
        ) {

            // Background
            drawRect(backgroundColor)

            drawCircle(
                color = Color(0xFFFFD845),
                radius = size.width * .5f,
                center = Offset(size.width * .05f, screenHeight * .05f),
            )

            drawCircle(
                color = Color(0xFFFFB626),
                radius = size.width * .35f,
                center = Offset(size.width * .95f, screenHeight * .18f),
                alpha = 0.8f,
            )

            // Overlay
            drawRect(Color(0xFF121212), alpha = 0.28f)
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
                .matchParentSize()
        )
    }

    @Composable
    fun IconRow(modifier: Modifier = Modifier) {
        Row(modifier) {
            Icon(
                painter = painterResource(R.drawable.pocket_casts_white),
                contentDescription = null,
                tint = Color.White,
            )

            Spacer(Modifier.width(8.dp))

            Icon(
                painter = painterResource(R.drawable.plus_bw),
                contentDescription = null,
                tint = Color.White,
            )
        }
    }

    @Composable
    fun PrivacyPolicy(
        color: Color,
        textAlign: TextAlign,
        modifier: Modifier = Modifier,
        lineHeight: TextUnit = 16.sp,
    ) {
        val privacyPolicyText = stringResource(LR.string.onboarding_plus_privacy_policy)
        val termsAndConditionsText = stringResource(LR.string.onboarding_plus_terms_and_conditions)
        val text = stringResource(
            LR.string.onboarding_plus_continuing_agrees_to,
            privacyPolicyText,
            termsAndConditionsText
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
                        uriHandler.openUri(Settings.INFO_PRIVACY_URL)
                    }
                ),
                Clickable(
                    text = termsAndConditionsText,
                    onClick = {
                        uriHandler.openUri(Settings.INFO_TOS_URL)
                    }
                ),
            ),
            modifier = modifier,
        )
    }
}
