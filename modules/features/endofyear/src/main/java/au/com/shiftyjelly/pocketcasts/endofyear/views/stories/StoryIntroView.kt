package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
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

        ConstraintLayout(
            modifier = modifier.fillMaxSize()
        ) {

            val (yearFirst2, year0, yearSecond2, year3, text, bottomLogo) = createRefs()

            // First 2 in 2023
            NumberImage(
                depthMultiplier = 8,
                imageRes = R.drawable.img_2,
                modifier = Modifier
                    .offset(
                        x = 230.dp,
                        y = 150.dp,
                    )
                    .constrainAs(yearFirst2) {
                        bottom.linkTo(text.top)
                        end.linkTo(text.start)
                    }
            )

            // 3 in 2023
            NumberImage(
                depthMultiplier = 10,
                imageRes = R.drawable.img_3,
                modifier = Modifier
                    .offset(
                        x = (-240).dp,
                        y = (-150).dp,
                    )
                    .constrainAs(year3) {
                        top.linkTo(text.bottom)
                        start.linkTo(text.end)
                    }
            )

            TitleView(
                depthMultiplier = 14,
                modifier = Modifier.constrainAs(text) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )

            // 0 in 2023
            NumberImage(
                depthMultiplier = 20,
                imageRes = R.drawable.img_0,
                modifier = Modifier
                    .offset(
                        x = (-140).dp,
                        y = 70.dp,
                    )
                    .constrainAs(year0) {
                        bottom.linkTo(text.top)
                        start.linkTo(text.end)
                    }
            )

            // Second 2 in 2023
            NumberImage(
                depthMultiplier = 17,
                imageRes = R.drawable.img_2_1,
                modifier = Modifier
                    .offset(
                        x = 125.dp,
                        y = (-85).dp,
                    )
                    .constrainAs(yearSecond2) {
                        top.linkTo(text.bottom)
                        end.linkTo(text.start)
                    }
            )

            PodcastLogoWhite(
                modifier = Modifier.constrainAs(bottomLogo) {
                    bottom.linkTo(parent.bottom, margin = 30.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )
        }
    }
}

@Composable
private fun TitleView(
    depthMultiplier: Int,
    modifier: Modifier,
) {
    ParallaxView(
        depthMultiplier = depthMultiplier,
        modifier = modifier,
        content = { parallaxModifier, biasAlignment ->
            Image(
                painter = painterResource(R.drawable.pocket_casts_playback),
                contentDescription = stringResource(LR.string.end_of_year_pocket_casts_playback),
                contentScale = ContentScale.FillBounds,
                modifier = parallaxModifier,
                alignment = biasAlignment,
            )
        }
    )
}

@Composable
private fun NumberImage(
    depthMultiplier: Int,
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
) {
    ParallaxView(
        depthMultiplier = depthMultiplier,
        modifier = modifier,
    ) { parallaxModifier, biasAlignment ->
        Box(
            modifier = parallaxModifier
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
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
