package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.paywallfeatures

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

data class CardData(
    val imageResId: Int,
    val contentDescription: String,
    val title: String,
    val description: String,
    val imageHeight: Dp,
    val imageTopPadding: Dp,
)

@Composable
fun FeaturePaywallCard(cardData: CardData) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(394.dp)
            .width(313.dp)
            .background(Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(UR.color.coolgrey_90)),
        ) {
            Image(
                painter = painterResource(cardData.imageResId),
                contentDescription = cardData.contentDescription,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = cardData.imageTopPadding)
                    .height(cardData.imageHeight)
                    .padding(horizontal = 4.dp),
                contentScale = ContentScale.Fit,
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp)
                    .fillMaxWidth(),
            ) {
                TextH30(
                    text = cardData.title,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.W600,
                    color = Color.White,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .align(Alignment.Start),
                )

                TextP50(
                    text = cardData.description,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.W400,
                    color = colorResource(UR.color.coolgrey_50),
                    modifier = Modifier.align(Alignment.Start),
                )
            }
        }
    }
}

@Composable
fun BookmarksCard() {
    val bookmarksCardData = CardData(
        imageResId = IR.drawable.bookmarks,
        contentDescription = stringResource(LR.string.paywall_bookmarks_content_description),
        title = stringResource(LR.string.paywall_bookmarks_title),
        description = stringResource(LR.string.paywall_bookmarks_description),
        imageHeight = 224.dp,
        imageTopPadding = 37.dp,
    )
    FeaturePaywallCard(cardData = bookmarksCardData)
}

@Composable
fun FoldersCard() {
    val foldersCardData = CardData(
        imageResId = IR.drawable.folders,
        contentDescription = stringResource(LR.string.paywall_folders_content_description),
        title = stringResource(LR.string.paywall_folders_title),
        description = stringResource(LR.string.paywall_folders_description),
        imageHeight = 231.dp,
        imageTopPadding = 34.dp,
    )
    FeaturePaywallCard(cardData = foldersCardData)
}

@Composable
fun DesktopWebAppCard() {
    val desktopCardData = CardData(
        imageResId = IR.drawable.desktop,
        contentDescription = stringResource(LR.string.paywall_desktop_and_web_content_description),
        title = stringResource(LR.string.paywall_desktop_and_web_title),
        description = stringResource(LR.string.paywall_desktop_and_web_description),
        imageHeight = 186.dp,
        imageTopPadding = 57.dp,
    )
    FeaturePaywallCard(cardData = desktopCardData)
}

@Composable
fun WatchCard() {
    val watchCardData = CardData(
        imageResId = IR.drawable.watch,
        contentDescription = stringResource(LR.string.paywall_watch_content_description),
        title = stringResource(LR.string.paywall_watch_title),
        description = stringResource(LR.string.paywall_watch_description),
        imageHeight = 231.dp,
        imageTopPadding = 26.dp,
    )
    FeaturePaywallCard(cardData = watchCardData)
}

@Composable
fun SlumberStudiosCard() {
    val slumberCardData = CardData(
        imageResId = IR.drawable.slumber,
        contentDescription = stringResource(LR.string.paywall_slumber_studios_content_description),
        title = stringResource(LR.string.paywall_slumber_studios_title),
        description = stringResource(LR.string.paywall_slumber_studios_description),
        imageHeight = 195.dp,
        imageTopPadding = 49.dp,
    )
    FeaturePaywallCard(cardData = slumberCardData)
}

@Composable
fun StorageCard() {
    val slumberCardData = CardData(
        imageResId = IR.drawable.storage,
        contentDescription = stringResource(LR.string.paywall_storage_content_description),
        title = stringResource(LR.string.paywall_storage_title),
        description = stringResource(LR.string.paywall_storage_description),
        imageHeight = 163.dp,
        imageTopPadding = 64.dp,
    )
    FeaturePaywallCard(cardData = slumberCardData)
}

@Composable
fun ThemesCard() {
    val slumberCardData = CardData(
        imageResId = IR.drawable.themes,
        contentDescription = stringResource(LR.string.paywall_themes_content_description),
        title = stringResource(LR.string.paywall_themes_title),
        description = stringResource(LR.string.paywall_themes_description),
        imageHeight = 84.dp,
        imageTopPadding = 105.dp,
    )
    FeaturePaywallCard(cardData = slumberCardData)
}

@Preview
@Composable
private fun WatchCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        WatchCard()
    }
}

@Preview
@Composable
private fun BookmarksCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        BookmarksCard()
    }
}

@Preview
@Composable
private fun DesktopWebAppCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        DesktopWebAppCard()
    }
}

@Preview
@Composable
private fun FoldersCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        FoldersCard()
    }
}

@Preview
@Composable
private fun SlumberStudiosCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        SlumberStudiosCard()
    }
}

@Preview
@Composable
private fun StorageCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        StorageCard()
    }
}

@Preview
@Composable
private fun ThemesCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        ThemesCard()
    }
}
