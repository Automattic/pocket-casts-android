package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.parallaxview.ParallaxView
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastLogoWhite
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StoryIntroView(
    modifier: Modifier = Modifier,
) {
    Box {
        BackgroundImage()
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = modifier.weight(1f))

            TitleView()

            Spacer(modifier = modifier.weight(1f))

            PodcastLogoWhite()

            Spacer(modifier = modifier.height(30.dp))
        }
    }
}

@Composable
private fun BackgroundImage() {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        NumberImage(
            xMultiplier = -0.2f,
            yMultiplier = -0.35f,
            depthMultiplier = 8,
            imageRes = R.drawable.img_2,
        )
        NumberImage(
            xMultiplier = 0.65f,
            yMultiplier = -0.25f,
            depthMultiplier = 12,
            imageRes = R.drawable.img_0,
        )
        NumberImage(
            xMultiplier = -0.05f,
            yMultiplier = 1.25f,
            depthMultiplier = 12,
            imageRes = R.drawable.img_2_1,
        )
        NumberImage(
            xMultiplier = 0.3f,
            yMultiplier = 0.75f,
            depthMultiplier = 10,
            imageRes = R.drawable.img_3,
        )
    }
}

@Composable
private fun TitleView() {
    ParallaxView(
        depthMultiplier = 30,
        content = { modifier, biasAlignment ->
            Image(
                painter = painterResource(R.drawable.pocket_casts_playback),
                contentDescription = stringResource(LR.string.end_of_year_pocket_casts_playback),
                contentScale = ContentScale.FillBounds,
                modifier = modifier,
                alignment = biasAlignment,
            )
        }
    )
}

@Composable
private fun NumberImage(
    xMultiplier: Float,
    yMultiplier: Float,
    depthMultiplier: Int,
    @DrawableRes imageRes: Int,
) {
    ParallaxView(depthMultiplier = depthMultiplier) { modifier, biasAlignment ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .drawWithContent {
                    withTransform({
                        translate(
                            left = size.width * xMultiplier,
                            top = size.height * yMultiplier,
                        )
                    }) {
                        this@drawWithContent.drawContent()
                    }
                }
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                alignment = biasAlignment,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StoryIntroPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Surface(color = Color.Black) {
            StoryIntroView()
        }
    }
}
