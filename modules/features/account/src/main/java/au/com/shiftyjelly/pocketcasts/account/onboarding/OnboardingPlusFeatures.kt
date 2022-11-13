package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.R
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingPlusFeatures(
    onShown: () -> Unit,
    onBackPressed: () -> Unit,
) {

    LaunchedEffect(Unit) { onShown() }
    BackHandler { onBackPressed() }

    Background()

    Column {

        Spacer(Modifier.height(8.dp))
        NavigationIconButton(
            onNavigationClick = onBackPressed,
            iconColor = Color.White,
            modifier = Modifier
                .height(48.dp)
                .width(48.dp)
        )

        Spacer(Modifier.height(12.dp))

        Column(Modifier.padding(horizontal = 24.dp)) {

            IconRow()

            Spacer(Modifier.height(36.dp))

            TextH10(
                text = stringResource(LR.string.onboarding_upgrade_everything_you_love_about_pocket_casts_plus),
                color = Color.White,
            )

            Spacer(Modifier.height(12.dp))

            TextP40(
                text = stringResource(LR.string.onboarding_upgrade_exclusive_features_and_options),
                color = Color.White.copy(alpha = 0.8f),
            )

            Spacer(Modifier.height(58.dp))

            FeatureRow()

            Spacer(Modifier.height(36.dp))

            PlusRowButton(
                text = stringResource(LR.string.onboarding_upgrade_unlock_all_features),
                onClick = { /* TODO */ },
            )

            Spacer(Modifier.height(16.dp))

            PlusOutlinedRowButton(
                text = stringResource(LR.string.not_now),
                onClick = { /* TODO */ },
            )
        }
    }
}

@Composable
private fun IconRow() {
    Row {
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
private fun FeatureRow() {
    Row(
        modifier = Modifier
            .height(180.dp)
            .background(Color.Gray)
    ) {
        TextP40("placeholder")
    }
}

@Composable
private fun Background() {

    // Blur only works on Android >=12
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .blur(150.dp)
        ) {

            // Background
            drawRect(Color.Black)

            drawCircle(
                color = Color(0xFFFFD845),
                radius = size.width * .5f,
                center = Offset(size.width * .05f, size.height * .05f),
            )

            drawCircle(
                color = Color(0xFFFFB626),
                radius = size.width * .35f,
                center = Offset(size.width * .95f, size.height * .18f),
                alpha = 0.8f,
            )

            // Overlay
            drawRect(Color(0xFF121212), alpha = 0.28f)
        }
    } else {
        Column(
            Modifier
                .background(Color.Black)
                .fillMaxSize()
        ) {
            Image(
                painterResource(R.drawable.upgrade_background_glows),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val plusGradientBrush = Brush.horizontalGradient(
    0f to Color(0xFFFED745),
    1f to Color(0xFFFEB525),
)

@Composable
private fun PlusRowButton(
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
private fun PlusOutlinedRowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, plusGradientBrush),
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
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

@Preview
@Composable
private fun OnboardingPlusFeaturesPreview() {
    OnboardingPlusFeatures(
        onShown = {},
        onBackPressed = {},
    )
}
