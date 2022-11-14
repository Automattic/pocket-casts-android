package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val background = Color(0xFF121212)

@Composable
fun OnboardingPlusFeaturesPage(
    onShown: () -> Unit,
    onBackPressed: () -> Unit,
) {

    LaunchedEffect(Unit) { onShown() }
    BackHandler { onBackPressed() }

    Background()

    Column(Modifier.verticalScroll(rememberScrollState())) {

        Spacer(Modifier.height(8.dp))
        NavigationIconButton(
            onNavigationClick = onBackPressed,
            iconColor = Color.White,
            modifier = Modifier
                .height(48.dp)
                .width(48.dp)
        )

        Spacer(Modifier.height(12.dp))

        Column {

            IconRow(Modifier.padding(horizontal = 24.dp))

            Spacer(Modifier.height(36.dp))

            TextH10(
                text = stringResource(LR.string.onboarding_upgrade_everything_you_love_about_pocket_casts_plus),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(12.dp))

            TextP40(
                text = stringResource(LR.string.onboarding_upgrade_exclusive_features_and_options),
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(58.dp))

            FeatureRow()

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(36.dp))

            PlusRowButton(
                text = stringResource(LR.string.onboarding_upgrade_unlock_all_features),
                onClick = { /* TODO */ },
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(16.dp))

            PlusOutlinedRowButton(
                text = stringResource(LR.string.not_now),
                onClick = { /* TODO */ },
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IconRow(modifier: Modifier = Modifier) {
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
private fun FeatureRow() {
    val state = rememberLazyListState()
    LaunchedEffect(Unit) {
        autoScroll(state)
    }
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val contentArray = FeatureItemContent.values()
        items(Int.MAX_VALUE) { n ->
            val content = contentArray[n % contentArray.size]
            FeatureItem(content)
        }
    }
}

@Composable
private fun FeatureItem(
    content: FeatureItemContent,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color(0xFF383839), // FIXME use resource??
                shape = shape,
            )
            .background(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF2A2A2B),
                    1f to Color(0xFF252525),
                ),
                shape = shape,
            )
            .size(width = 156.dp, height = 180.dp)
            .padding(all = 16.dp)
    ) {

        Icon(
            painter = painterResource(content.image),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        TextH40(stringResource(content.title))
        Spacer(Modifier.height(4.dp))
        TextP60(
            text = stringResource(content.text),
            modifier = Modifier.alpha(0.72f),
        )
    }
}

enum class FeatureItemContent(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val text: Int,
) {
    DesktopApps(
        image = IR.drawable.desktop_apps,
        title = LR.string.onboarding_plus_feature_desktop_apps_title,
        text = LR.string.onboarding_plus_feature_desktop_apps_text,
    ),
    Folders(
        image = IR.drawable.ic_folder,
        title = LR.string.onboarding_plus_feature_folders_title,
        text = LR.string.onboarding_plus_feature_folders_text,
    ),
    CloudStorage(
        image = IR.drawable.ic_cloud,
        title = LR.string.onboarding_plus_feature_cloud_storage_title,
        text = LR.string.onboarding_plus_feature_cloud_storage_text,
    ),
    HideAds(
        image = IR.drawable.ads_disabled,
        title = LR.string.onboarding_plus_feature_hide_ads_title,
        text = LR.string.onboarding_plus_feature_hide_ads_text,
    ),
    ThemesIcons(
        image = IR.drawable.whatsnew_theme,
        title = LR.string.onboarding_plus_feature_themes_icons_title,
        text = LR.string.onboarding_plus_feature_themes_icons_text,
    ),
}

@Composable
private fun Background() {

    // Blur only works on Android >=12
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(150.dp)
        ) {

            // Background
            drawRect(background)

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
                .background(background)
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

// From https://stackoverflow.com/a/71344813/1910286
private tailrec suspend fun autoScroll(
    lazyListState: LazyListState
) {
    lazyListState.scroll(MutatePriority.PreventUserInput) {
        scrollBy(1f)
    }
    delay(5)

    autoScroll(lazyListState)
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
    OnboardingPlusFeaturesPage(
        onShown = {},
        onBackPressed = {},
    )
}
