package au.com.shiftyjelly.pocketcasts.playlists

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.PagerDotIndicator
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingPage(
    onGotItClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        val pagerState = rememberPagerState(pageCount = { 2 })

        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            contentPadding = PaddingValues(top = 64.dp),
            modifier = Modifier.weight(1f),
        ) { index ->
            when (index) {
                0 -> OnboardingContent(
                    imageId = IR.drawable.playlists_onboarding_smart_playlists,
                    title = stringResource(LR.string.playlists_onboarding_smart_playlist_title),
                    description = stringResource(LR.string.playlists_onboarding_smart_playlist_description),
                )

                1 -> OnboardingContent(
                    imageId = IR.drawable.playlists_onboarding_playlists,
                    title = stringResource(LR.string.playlists_onboarding_playlist_title),
                    description = stringResource(LR.string.playlists_onboarding_playlist_description),
                )

                else -> error("Unknown onboarding page: $index")
            }
        }
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        PagerDotIndicator(
            state = pagerState,
            activeDotColor = MaterialTheme.theme.colors.primaryText01,
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        RowButton(
            text = stringResource(LR.string.got_it),
            includePadding = false,
            onClick = onGotItClick,
            modifier = Modifier
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun OnboardingContent(
    @DrawableRes imageId: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp),
        ) {
            Image(
                painter = painterResource(imageId),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(
                modifier = Modifier.height(24.dp),
            )
            TextH10(
                text = title,
                textAlign = TextAlign.Center,
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            TextP40(
                text = description,
                textAlign = TextAlign.Center,
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingPage(
            onGotItClick = {},
        )
    }
}
