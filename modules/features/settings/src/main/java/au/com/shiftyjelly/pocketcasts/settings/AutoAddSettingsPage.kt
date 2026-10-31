package au.com.shiftyjelly.pocketcasts.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.compose.bars.AppCompatTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import coil3.compose.AsyncImage
import kotlin.math.ceil
import kotlin.math.cos
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class AutoAddOption(
    @DrawableRes val iconResId: Int?,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@Composable
internal fun AutoAddSettingsPage(
    options: List<AutoAddOption>,
    footer: String?,
    podcasts: List<Podcast>,
    bottomInset: Dp,
    onPodcastClick: (Podcast) -> Unit,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi02),
    ) {
        AppCompatTopAppBar(
            title = stringResource(LR.string.settings_auto_up_next_title),
            navigationButton = NavigationButton.Drawable(IR.drawable.ic_arrow_back, LR.string.back),
            onNavigationClick = onNavigationClick,
            iconColor = colors.secondaryIcon01,
            titleColor = colors.secondaryText01,
            backgroundColor = colors.secondaryUi01,
            bottomShadow = false,
            modifier = toolbarModifier,
        )
        LazyColumn(
            contentPadding = PaddingValues(top = 16.dp, bottom = bottomInset),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(options) { option -> OptionRow(option) }
            if (footer != null) {
                item {
                    TextViewText(
                        text = footer,
                        fontSize = 14.sp,
                        color = colors.primaryText02,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, top = 16.dp, end = 24.dp, bottom = 16.dp),
                    )
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.primaryUi05),
                    )
                    TextViewText(
                        text = stringResource(LR.string.settings_auto_up_next_podcasts),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.support01,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, top = 16.dp, end = 24.dp, bottom = 16.dp),
                    )
                }
            }
            items(podcasts, key = { it.uuid }) { podcast ->
                PodcastRow(podcast = podcast, onClick = { onPodcastClick(podcast) })
            }
        }
    }
}

@Composable
private fun OptionRow(option: AutoAddOption) {
    val colors = MaterialTheme.theme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = option.onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(modifier = Modifier.size(24.dp)) {
            if (option.iconResId != null) {
                DrawableImage(
                    drawableId = option.iconResId,
                    contentDescription = null,
                    tint = colors.primaryIcon01,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            TextViewText(text = option.title, fontSize = 16.sp, color = colors.primaryText01, modifier = Modifier.fillMaxWidth())
            TextViewText(text = option.subtitle, fontSize = 14.sp, color = colors.primaryText02, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PodcastRow(podcast: Podcast, onClick: () -> Unit) {
    val colors = MaterialTheme.theme.colors
    val context = LocalContext.current
    val request = remember(podcast.uuid, context) { PocketCastsImageRequestFactory(context).create(podcast) }
    val card = rememberCompatCard()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .compatCard(card)
                .size(56.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            contentAlignment = TextViewAlignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .height(56.dp + card.verticalPadding * 2),
        ) {
            Column {
                TextViewText(text = podcast.title, fontSize = 16.sp, color = colors.primaryText01, maxLines = 1, modifier = Modifier.fillMaxWidth())
                val subtitle = when (podcast.autoAddToUpNext) {
                    Podcast.AutoAddUpNext.PLAY_NEXT -> stringResource(LR.string.settings_auto_up_next_to_top)
                    Podcast.AutoAddUpNext.PLAY_LAST -> stringResource(LR.string.settings_auto_up_next_to_bottom)
                    Podcast.AutoAddUpNext.OFF -> ""
                }
                TextViewText(text = subtitle, fontSize = 15.sp, letterSpacing = (-0.01).em, color = colors.primaryText02, maxLines = 1, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private class CompatCard(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val backgroundColor: Color,
)

@Composable
private fun rememberCompatCard(): CompatCard {
    val density = LocalDensity.current
    val context = LocalContext.current
    return remember(density, context) {
        with(density) {
            val elevation = CardElevation.toPx()
            val radius = CardCornerRadius.toPx()
            val corner = (1 - cos(Math.toRadians(45.0))) * radius
            val horizontal = elevation + corner
            val vertical = elevation * 1.5 + corner
            val windowBackground = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground)).use { it.getColor(0, 0) }
            val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(windowBackground, it) }
            CompatCard(
                horizontalPadding = ceil(horizontal).toInt().toDp(),
                verticalPadding = ceil(vertical).toInt().toDp(),
                backgroundColor = if (hsv[2] > 0.5f) Color(0xFFFFFFFF) else Color(0xFF424242),
            )
        }
    }
}

private fun Modifier.compatCard(card: CompatCard): Modifier = padding(horizontal = card.horizontalPadding, vertical = card.verticalPadding)
    .shadow(CardElevation, RoundedCornerShape(CardCornerRadius))
    .background(card.backgroundColor)

private val CardElevation = 2.dp
private val CardCornerRadius = 4.dp
