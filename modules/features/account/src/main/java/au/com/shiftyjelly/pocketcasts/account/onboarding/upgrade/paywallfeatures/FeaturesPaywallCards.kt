package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.paywallfeatures

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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

@Composable
fun FeaturedPaywallCards(modifier: Modifier = Modifier) {
    val featuredCards: List<CardData> = listOf(
        bookmarks,
        folders,
        desktop,
        watch,
        slumber,
        storage,
        themes,
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        items(
            count = featuredCards.size,
            key = { index -> index },

        ) { index ->
            FeaturedPaywallCard(cardData = featuredCards[index])
        }
    }
}

@Composable
fun FeaturedPaywallCard(cardData: CardData) {
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
                contentDescription = stringResource(cardData.contentDescriptionResourceId),
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
                    text = stringResource(cardData.titleResourceId),
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.W600,
                    color = Color.White,
                    disableScale = true,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .align(Alignment.Start),
                )

                TextP50(
                    text = stringResource(cardData.descriptionResourceId),
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.W400,
                    disableScale = true,
                    color = colorResource(UR.color.coolgrey_50),
                    modifier = Modifier.align(Alignment.Start),
                )
            }
        }
    }
}

data class CardData(
    @DrawableRes val imageResId: Int,
    @StringRes val contentDescriptionResourceId: Int,
    @StringRes val titleResourceId: Int,
    @StringRes val descriptionResourceId: Int,
    val imageHeight: Dp,
    val imageTopPadding: Dp,
)

private val bookmarks = CardData(
    imageResId = IR.drawable.bookmarks,
    contentDescriptionResourceId = LR.string.paywall_bookmarks_content_description,
    titleResourceId = LR.string.paywall_bookmarks_title,
    descriptionResourceId = LR.string.paywall_bookmarks_description,
    imageHeight = 224.dp,
    imageTopPadding = 37.dp,
)

private val folders = CardData(
    imageResId = IR.drawable.folders,
    contentDescriptionResourceId = LR.string.paywall_folders_content_description,
    titleResourceId = LR.string.paywall_folders_title,
    descriptionResourceId = LR.string.paywall_folders_description,
    imageHeight = 227.dp,
    imageTopPadding = 34.dp,
)

private val desktop = CardData(
    imageResId = IR.drawable.desktop,
    contentDescriptionResourceId = LR.string.paywall_desktop_and_web_content_description,
    titleResourceId = LR.string.paywall_desktop_and_web_title,
    descriptionResourceId = LR.string.paywall_desktop_and_web_description,
    imageHeight = 186.dp,
    imageTopPadding = 57.dp,
)

private val watch = CardData(
    imageResId = IR.drawable.watch,
    contentDescriptionResourceId = LR.string.paywall_watch_content_description,
    titleResourceId = LR.string.paywall_watch_title,
    descriptionResourceId = LR.string.paywall_watch_description,
    imageHeight = 231.dp,
    imageTopPadding = 26.dp,
)

private val slumber = CardData(
    imageResId = IR.drawable.slumber,
    contentDescriptionResourceId = LR.string.paywall_slumber_studios_content_description,
    titleResourceId = LR.string.paywall_slumber_studios_title,
    descriptionResourceId = LR.string.paywall_slumber_studios_description,
    imageHeight = 195.dp,
    imageTopPadding = 49.dp,
)

private val storage = CardData(
    imageResId = IR.drawable.storage,
    contentDescriptionResourceId = LR.string.paywall_storage_content_description,
    titleResourceId = LR.string.paywall_storage_title,
    descriptionResourceId = LR.string.paywall_storage_description,
    imageHeight = 163.dp,
    imageTopPadding = 64.dp,
)

private val themes = CardData(
    imageResId = IR.drawable.themes,
    contentDescriptionResourceId = LR.string.paywall_themes_content_description,
    titleResourceId = LR.string.paywall_themes_title,
    descriptionResourceId = LR.string.paywall_themes_description,
    imageHeight = 84.dp,
    imageTopPadding = 105.dp,
)

@Preview
@Composable
private fun WatchCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        FeaturedPaywallCard(watch)
    }
}

@Preview
@Composable
private fun BookmarksCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        FeaturedPaywallCard(bookmarks)
    }
}

@Preview
@Composable
private fun DesktopWebAppCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        FeaturedPaywallCard(desktop)
    }
}

@Preview
@Composable
private fun FoldersCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        FeaturedPaywallCard(folders)
    }
}

@Preview
@Composable
private fun SlumberStudiosCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        FeaturedPaywallCard(slumber)
    }
}

@Preview
@Composable
private fun StorageCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        FeaturedPaywallCard(storage)
    }
}

@Preview
@Composable
private fun ThemesCardPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        FeaturedPaywallCard(themes)
    }
}
