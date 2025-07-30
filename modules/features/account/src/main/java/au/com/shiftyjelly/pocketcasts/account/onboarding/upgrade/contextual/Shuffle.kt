package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ShuffleAnimation(
    modifier: Modifier = Modifier,
) {
    var activeDataSet by remember { mutableIntStateOf(0) }

    LaunchedEffect("animation") {
        repeat(predefinedShuffle.chunked(3).size - 1) {
            delay(1500)
            activeDataSet++
        }
    }

    Box(modifier = modifier) {
        predefinedShuffle.chunked(3).forEachIndexed { index, dataSet ->
            ShuffleContainer(
                items = dataSet,
                isDisplayed = index == activeDataSet,
            )
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

private data class ShuffleConfig(
    @DrawableRes val artworkResId: Int,
    val date: Instant,
    val title: String,
    val durationSeconds: Int,
)

private val predefinedShuffle = listOf(
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_0,
        date = Instant.now().minus(Random.nextLong(60L), ChronoUnit.DAYS),
        title = "The Sunday Read",
        durationSeconds = Random.nextInt(7200),
    ),
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_1,
        date = Instant.now().minus(Random.nextLong(60L), ChronoUnit.DAYS),
        title = "What have you done today",
        durationSeconds = Random.nextInt(7200),
    ),
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_2,
        date = Instant.now().minus(Random.nextLong(60L), ChronoUnit.DAYS),
        title = "800: Jane Doe",
        durationSeconds = Random.nextInt(7200),
    ),
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_3,
        date = Instant.now().minus(Random.nextLong(60L), ChronoUnit.DAYS),
        title = "Can Lex rebuild the Coalition?",
        durationSeconds = Random.nextInt(7200),
    ),
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_4,
        date = Instant.now().minus(Random.nextLong(60L), ChronoUnit.DAYS),
        title = "David Bezmozgis Reads \"From, to\"",
        durationSeconds = Random.nextInt(7200),
    ),
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_5,
        date = Instant.now().minus(Random.nextLong(60L), ChronoUnit.DAYS),
        title = "The Trial of Sean Combs",
        durationSeconds = Random.nextInt(7200),
    ),
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_6,
        date = Instant.now().minus(Random.nextLong(60L), ChronoUnit.DAYS),
        title = "Jason played The Switch 2",
        durationSeconds = Random.nextInt(7200),
    ),
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_7,
        date = Instant.now().minus(Random.nextLong(60L), ChronoUnit.DAYS),
        title = "887: Burgertory",
        durationSeconds = Random.nextInt(7200),
    ),
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_8,
        date = Instant.now().minus(Random.nextLong(60L), ChronoUnit.DAYS),
        title = "A Knight's Tale",
        durationSeconds = Random.nextInt(7200),
    ),
)

@Composable
private fun ShuffleContainer(
    items: List<ShuffleConfig>,
    modifier: Modifier = Modifier,
    middleItemScale: Float = 1.1f,
    horizontalPadding: Dp = 32.dp,
    itemsOverlap: Dp = 18.dp,
    isDisplayed: Boolean = true,
) {
    if (items.size % 2 == 0) {
        error("must have odd number of elements!")
    }

    val rowAnimations = remember {
        List(items.size) {
            mutableStateOf(false)
        }
    }

    LaunchedEffect(isDisplayed) {
        rowAnimations.forEach { item ->
            launch {
                item.value = isDisplayed
                delay(100)
            }
        }
    }

    Layout(
        modifier = modifier,
        content = {
            items.forEachIndexed { index, item ->
                ShuffleItem(
                    index = items.size - index,
                    isDisplayed = rowAnimations[index].value,
                    config = item,
                    scale = if (items.isMiddleIndex(index)) {
                        middleItemScale
                    } else {
                        1f
                    },
                    elevation = if (items.isMiddleIndex(index)) {
                        4.dp
                    } else {
                        2.dp
                    },
                )
            }
        },
    ) { measurables, constraints ->
        val regularItemWidth = constraints.maxWidth - 2 * horizontalPadding.roundToPx()
        val focusedItemWidth = (regularItemWidth * middleItemScale).toInt()
        val placeables = measurables.mapIndexed { index, item ->
            val isFocusedItem = measurables.isMiddleIndex(index)

            if (isFocusedItem) {
                item.measure(Constraints.fixedWidth(focusedItemWidth))
            } else {
                item.measure(Constraints.fixedWidth(regularItemWidth))
            }
        }
        val regularItemHeight = placeables.minOf { it.height }
        val totalHeight = regularItemHeight * items.size

        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { index, item ->
                val isCurrentFocused = placeables.isMiddleIndex(index)
                val offset = if (isCurrentFocused) {
                    (item.height - regularItemHeight) / 2
                } else {
                    0
                }
                item.placeRelative(
                    x = (constraints.maxWidth - item.width) / 2,
                    y = index * (regularItemHeight - itemsOverlap.roundToPx()) - offset,
                    zIndex = if (isCurrentFocused) {
                        placeables.size
                    } else {
                        index
                    }.toFloat(),
                )
            }
        }
    }
}

@Composable
private fun ShuffleItem(
    config: ShuffleConfig,
    index: Int,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    elevation: Dp = 2.dp,
    isDisplayed: Boolean = true,
    floatInDistance: Dp = 32.dp,
) {
    var wasVisible by remember { mutableStateOf(false) }
    val floatInDistancePx = LocalDensity.current.run { floatInDistance.toPx() }
    val transition = updateTransition(isDisplayed)

    LaunchedEffect(isDisplayed) {
        if (isDisplayed) {
            wasVisible = isDisplayed
        }
    }

    val alphaAnim by transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = if (isDisplayed) 800 else 400,
                delayMillis = index * 100,
                easing = FastOutSlowInEasing,
            )
        },
    ) { visible ->
        if (visible) {
            1f
        } else {
            0f
        }
    }
    val transitionAnim by transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = if (isDisplayed) 800 else 400,
                delayMillis = index * 100,
                easing = FastOutSlowInEasing,
            )
        },
    ) { visible ->
        if (visible) {
            0f
        } else {
            if (wasVisible) {
                -floatInDistancePx
            } else {
                floatInDistancePx
            }
        }
    }

    Card(
        modifier = modifier.graphicsLayer {
            scaleY = scale
            scaleX = scale
            alpha = alphaAnim
            translationY = transitionAnim
        },
        elevation = elevation,
        backgroundColor = MaterialTheme.theme.colors.primaryUi03,
        shape = RoundedCornerShape(3.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(3.dp)),
                painter = painterResource(config.artworkResId),
                contentDescription = "",
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                TextH70(
                    fontSize = 10.sp,
                    text = dateFormatter.format(config.date).toUpperCase(Locale.current),
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.fillMaxWidth(),
                    disableAutoScale = true,
                )
                TextP60(
                    text = config.title,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    color = MaterialTheme.theme.colors.primaryText01,
                    disableAutoScale = true,
                )
                TextH70(
                    fontSize = 10.sp,
                    text = formatDuration(config.durationSeconds),
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.fillMaxWidth(),
                    disableAutoScale = true,
                )
            }
        }
    }
}

private fun List<Any>.isMiddleIndex(index: Int): Boolean {
    val middleIndex = size / 2
    return if (size % 2 == 0) {
        index == middleIndex || index == middleIndex - 1
    } else {
        index == middleIndex
    }
}

@ReadOnlyComposable
@Composable
fun formatDuration(seconds: Int): String {
    val totalMinutes = seconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return if (hours == 0) {
        pluralStringResource(LR.plurals.onboarding_shuffle_item_duration_minutes, minutes, minutes)
    } else {
        stringResource(LR.string.onboarding_shuffle_item_duration_hours, hours, minutes)
    }
}

@Preview
@Composable
private fun PreviewShuffleContainer(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        ShuffleContainer(
            items = predefinedShuffle.take(3),
        )
        Spacer(Modifier.height(42.dp))
        ShuffleContainer(
            items = predefinedShuffle.take(5),
        )
    }
}

@Preview
@Composable
private fun PreviewShuffle(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShuffleItem(
            config = predefinedShuffle[0],
            index = 0,
        )
        ShuffleItem(
            config = predefinedShuffle[1],
            scale = 1.2f,
            elevation = 4.dp,
            index = 0,
        )
    }
}
