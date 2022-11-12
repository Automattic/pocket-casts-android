package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.R

@Composable
fun OnboardingPlusFeatures(
    onShown: () -> Unit,
    onBackPressed: () -> Unit,
) {

    LaunchedEffect(Unit) { onShown() }
    BackHandler { onBackPressed() }

    Background()
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
        Column(Modifier.background(Color.Black)) {
            Image(
                painterResource(R.drawable.upgrade_background_glows),
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
private fun BackgroundPreview() {
    OnboardingPlusFeatures(
        onShown = {},
        onBackPressed = {},
    )
}
