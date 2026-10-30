package au.com.shiftyjelly.pocketcasts.views.fragments

import android.content.res.ColorStateList
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.TextPaint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.compose.bars.AppCompatTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import coil3.compose.AsyncImage
import com.google.android.material.checkbox.MaterialCheckBox
import kotlin.math.ceil
import androidx.appcompat.R as AppCompatR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastSelectPage(
    podcasts: List<Podcast>,
    selectedUuids: Set<String>,
    selectedText: String,
    tintColor: Color?,
    showToolbar: Boolean,
    bottomInset: Dp,
    onToggle: (Podcast, Boolean) -> Unit,
    onSelectButtonClick: () -> Unit,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi01)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        if (showToolbar) {
            AppCompatTopAppBar(
                title = stringResource(LR.string.settings_choose_podcasts),
                navigationButton = NavigationButton.Drawable(AppCompatR.drawable.abc_ic_ab_back_material, AppCompatR.string.abc_action_bar_up_description),
                onNavigationClick = onNavigationClick,
                iconColor = colors.primaryInteractive01,
                titleColor = colors.primaryText01,
                backgroundColor = colors.secondaryUi01,
                bottomShadow = false,
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        ) {
            Box(
                contentAlignment = TextViewAlignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                TextViewText(
                    text = selectedText,
                    fontSize = 14.sp,
                    letterSpacing = 0.02.em,
                    lineSpacingExtra = 4.sp,
                    color = colors.primaryText02,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                contentAlignment = TextViewAlignment.Center,
                modifier = Modifier
                    .height(48.dp)
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(role = Role.Button, onClick = onSelectButtonClick)
                    .defaultMinSize(minWidth = 64.dp)
                    .padding(horizontal = 8.dp),
            ) {
                val selectText = stringResource(if (selectedUuids.size == podcasts.size) LR.string.select_none else LR.string.select_all)
                TextViewText(
                    text = selectText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = BUTTON_LETTER_SPACING.em,
                    color = tintColor ?: colors.primaryInteractive01,
                    maxLines = 1,
                    modifier = Modifier.width(rememberButtonTextWidth(selectText)),
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomInset + 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(podcasts, key = { it.uuid }) { podcast ->
                val isSelected = podcast.uuid in selectedUuids
                PodcastSelectRow(
                    podcast = podcast,
                    isSelected = isSelected,
                    tintColor = tintColor,
                    onToggle = { onToggle(podcast, it) },
                )
            }
        }
    }
}

@Composable
private fun PodcastSelectRow(
    podcast: Podcast,
    isSelected: Boolean,
    tintColor: Color?,
    onToggle: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.theme.colors
    val context = LocalContext.current
    val request = remember(podcast.uuid, context) { PocketCastsImageRequestFactory(context).themed().smallSize().create(podcast) }
    val cardBackground = rememberCardBackground()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primaryUi01)
            .clickable { onToggle(!isSelected) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(cardBackground)
                .size(56.dp),
        )
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        ) {
            TextViewText(text = podcast.title, fontSize = 14.sp, color = colors.primaryText01, maxLines = 1, modifier = Modifier.fillMaxWidth())
            TextViewText(text = podcast.author, fontSize = 14.sp, color = colors.primaryText02, maxLines = 1, modifier = Modifier.fillMaxWidth())
        }
        Box(
            contentAlignment = TextViewAlignment.CenterStart,
            modifier = Modifier
                .width(44.dp)
                .height(56.dp),
        ) {
            AndroidView(
                factory = { MaterialCheckBox(it).apply { setPaddingRelative(0, 0, (16 * resources.displayMetrics.density).toInt(), 0) } },
                update = { checkBox ->
                    checkBox.setOnCheckedChangeListener(null)
                    checkBox.isChecked = isSelected
                    if (tintColor != null) {
                        checkBox.buttonTintList = ColorStateList.valueOf(tintColor.toArgb())
                    }
                    checkBox.setOnCheckedChangeListener { _, checked -> onToggle(checked) }
                },
                modifier = Modifier.width(44.dp),
            )
        }
    }
}

@Composable
private fun rememberCardBackground(): Color {
    val context = LocalContext.current
    return remember(context) {
        val windowBackground = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground)).use { it.getColor(0, 0) }
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(windowBackground, it) }
        if (hsv[2] > 0.5f) Color(0xFFFFFFFF) else Color(0xFF424242)
    }
}

@Composable
private fun rememberButtonTextWidth(text: String): Dp {
    val density = LocalDensity.current
    return remember(text, density) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = with(density) { 14.sp.roundToPx().toFloat() }
            letterSpacing = BUTTON_LETTER_SPACING
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        with(density) { ceil(Layout.getDesiredWidth(text, paint) + paint.letterSpacing * paint.textSize).toInt().toDp() }
    }
}

private const val BUTTON_LETTER_SPACING = 0.0892857143f
