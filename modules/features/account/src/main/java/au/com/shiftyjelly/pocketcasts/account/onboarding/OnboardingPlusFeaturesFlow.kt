package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OnboardingPlusFeaturesFlow(
    onShown: () -> Unit,
    onUpgradePressed: () -> Unit,
    onNotNowPressed: () -> Unit,
    onBackPressed: () -> Unit,
) {

    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    BackHandler {
        if (sheetState.isVisible) {
            coroutineScope.launch {
                sheetState.hide()
            }
        } else {
            onBackPressed()
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    ModalBottomSheetLayout(
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        content = @Composable {
            OnboardingPlusPage(
                onShown = onShown,
                onUpgradePressed = {
                    coroutineScope.launch {
                        sheetState.show()
                    }
                    onUpgradePressed()
                },
                onNotNowPressed = onNotNowPressed,
                onBackPressed = onBackPressed
            )
        },
        sheetContent = {
            OnboardingPlusBottomSheet()
        },
    )
}

object OnboardingPlusFeatures {
    val plusGradientBrush = Brush.horizontalGradient(
        0f to Color(0xFFFED745),
        1f to Color(0xFFFEB525),
    )

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
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, plusGradientBrush),
            elevation = null,
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
            modifier = modifier.fillMaxWidth(),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    text = text,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.Center)
                        .textBrush(plusGradientBrush)
                )
            }
        }
    }

    // From https://stackoverflow.com/a/71376469/1910286
    private fun Modifier.textBrush(brush: Brush) = this
        .graphicsLayer(alpha = 0.99f)
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(brush, blendMode = BlendMode.SrcAtop)
            }
        }
}
