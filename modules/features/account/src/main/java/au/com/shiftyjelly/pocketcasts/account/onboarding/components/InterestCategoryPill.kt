package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@Composable
fun InterestCategoryPill(
    category: DiscoverCategory,
    isSelected: Boolean,
    index: Int,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var animateState by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(animateState) {
        if (animateState) {
            scope.launch {
                scaleAnim.animateTo(
                    targetValue = 1.1f,
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing,
                    ),
                )
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
            scope.launch {
                rotationAnim.animateTo(
                    targetValue = -3f,
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing,
                    ),
                )
                rotationAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    val colorConfig = colors[index % colors.size]
    SelectablePillContainer(
        isSelected = isSelected,
        onSelectedChange = {
            animateState = it
            onSelectedChange(it)
        },
        selectedGradient = colorConfig.gradient.toList(),
        modifier = modifier
            .graphicsLayer {
                scaleY = scaleAnim.value
                scaleX = scaleAnim.value

                transformOrigin = TransformOrigin(0.5f, 0.5f)
                rotationZ = rotationAnim.value
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GradientImage(
                colorConfig = colorConfig,
                isSelected = isSelected,
                url = category.icon,
                modifier = Modifier.size(21.dp),
            )
            TextP30(
                text = category.name,
                color = if (isSelected) {
                    colorConfig.selectedTextColor ?: MaterialTheme.theme.colors.primaryUi01Active
                } else {
                    MaterialTheme.theme.colors.secondaryText02
                },
            )
        }
    }
}

private data class ColorConfig(
    val gradient: Pair<Color, Color>,
    val selectedTextColor: Color? = null,
)

private val colors = listOf(
    ColorConfig(
        gradient = Color(0xFFF43769) to Color(0xFFFB5246),
    ),
    ColorConfig(
        gradient = Color(0xFFFED745) to Color(0xFFFEB525),
        selectedTextColor = Color(0xFFA85605),
    ),
    ColorConfig(
        gradient = Color(0xFF6046E9) to Color(0xFFE74B8A),
    ),
    ColorConfig(
        gradient = Color(0xFF03A9F4) to Color(0xFF50D0F1),
    ),
    ColorConfig(
        Color(0xFF78D549) to Color(0xFF9BE45E),
        selectedTextColor = Color(0xFF1E4316),
    ),
)

@Composable
private fun GradientImage(
    colorConfig: ColorConfig,
    isSelected: Boolean,
    url: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val factory = remember(context) {
        val placeholderType = if (isPreview) {
            PocketCastsImageRequestFactory.PlaceholderType.Small
        } else {
            PocketCastsImageRequestFactory.PlaceholderType.None
        }
        PocketCastsImageRequestFactory(context, placeholderType = placeholderType).themed()
    }
    val imageRequest = remember(colorConfig, isSelected, url, factory) {
        factory.createForFileOrUrl(
            filePathOrUrl = url,
        )
    }

    val fallback = MaterialTheme.theme.colors.primaryUi01Active

    Image(
        painter = rememberAsyncImagePainter(
            model = imageRequest,
            contentScale = ContentScale.Fit,
        ),
        contentDescription = null,
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithCache {
                val brush = if (!isSelected) {
                    Brush.horizontalGradient(colorConfig.gradient.toList())
                } else {
                    Brush.horizontalGradient((0..1).map { colorConfig.selectedTextColor ?: fallback })
                }
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = brush, blendMode = BlendMode.SrcIn)
                }
            },
    )
}

@Composable
private fun SelectablePillContainer(
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    selectedGradient: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(percent = 100))
            .then(
                if (isSelected) {
                    Modifier.background(brush = Brush.horizontalGradient(selectedGradient))
                } else {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.theme.colors.primaryUi05,
                        shape = RoundedCornerShape(percent = 100),
                    )
                }
                    .toggleable(
                        value = isSelected,
                        onValueChange = onSelectedChange,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private val demoCategories = List(10) {
    DiscoverCategory(
        id = it,
        name = "Category $it",
        icon = "",
        source = "",
    )
}

@Preview
@Composable
private fun PreviewCategoryPill(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) = AppThemeWithBackground(themeType) {
    Column(modifier = Modifier.padding(32.dp)) {
        demoCategories.forEachIndexed { index, category ->
            InterestCategoryPill(
                category = category,
                isSelected = index / colors.size == 0,
                onSelectedChange = {},
                index = index,
            )
        }
    }
}
