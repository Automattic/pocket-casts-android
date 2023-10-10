package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

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
        ImageTwenty()
        ImageTwentyThree()
    }
}

@Composable
private fun TitleView() {
    Image(
        painter = painterResource(R.drawable.eoy_review),
        contentDescription = stringResource(LR.string.end_of_year_review),
        contentScale = ContentScale.FillBounds
    )
}

@Composable
private fun ImageTwenty(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                withTransform({
                    scale(1.07f, 1f)
                    translate(left = -size.width * .025f, top = 0f)
                }) {
                    this@drawWithContent.drawContent()
                }
            }

    ) {
        Image(
            painter = painterResource(R.drawable.img_20),
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun ImageTwentyThree(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                withTransform({
                    scale(1.17f, 1f)
                    translate(left = -size.width * .06f, top = 0f)
                }) {
                    this@drawWithContent.drawContent()
                }
            }

    ) {
        Image(
            painter = painterResource(R.drawable.img_23),
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )
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
