package au.com.shiftyjelly.pocketcasts.compose.folder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable

private val gradientTop = Color(0x00000000)
private val gradientBottom = Color(0x33000000)
private val topPodcastImageGradient = listOf(Color(0x00000000), Color(0x16000000))
private val bottomPodcastImageGradient = listOf(Color(0x16000000), Color(0x33000000))
private const val paddingBottomRatio = 120f / 1f
private const val paddingImageRatio = 120f / 4f
private const val imageSizeRatio = 120f / 44f

@Composable
fun FolderImage(
    name: String,
    color: Color,
    podcastUuids: List<String>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    badgeCount: Int = 0,
    badgeType: Settings.BadgeType = Settings.BadgeType.OFF
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(color = color)
            .aspectRatio(1f)
    ) {
        val constraints = this

        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            gradientTop,
                            gradientBottom
                        )
                    )
                )
                .fillMaxSize()
        ) {}
        val podcastSize = (constraints.maxWidth.value / imageSizeRatio).dp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            val imagePadding = (constraints.maxWidth.value / paddingImageRatio).dp
            Spacer(modifier = Modifier.height(imagePadding))
            Row(horizontalArrangement = Arrangement.Center) {
                Column(horizontalAlignment = Alignment.End) {
                    FolderPodcastImage(
                        uuid = podcastUuids.getOrNull(0),
                        color = color,
                        gradientColor = topPodcastImageGradient,
                        modifier = Modifier.size(podcastSize)
                    )
                    Spacer(modifier = Modifier.height(imagePadding))
                    FolderPodcastImage(
                        uuid = podcastUuids.getOrNull(2),
                        color = color,
                        gradientColor = bottomPodcastImageGradient,
                        modifier = Modifier.size(podcastSize)
                    )
                }
                Spacer(modifier = Modifier.width(imagePadding))
                Column(horizontalAlignment = Alignment.Start) {
                    FolderPodcastImage(
                        uuid = podcastUuids.getOrNull(1),
                        color = color,
                        gradientColor = topPodcastImageGradient,
                        modifier = Modifier.size(podcastSize)
                    )
                    Spacer(modifier = Modifier.height(imagePadding))
                    FolderPodcastImage(
                        uuid = podcastUuids.getOrNull(3),
                        color = color,
                        gradientColor = bottomPodcastImageGradient,
                        modifier = Modifier.size(podcastSize)
                    )
                }
            }
            if (name.isNotBlank()) {
                Spacer(modifier = Modifier.height((constraints.maxWidth.value / paddingBottomRatio).dp))
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight(500),
                    letterSpacing = 0.25.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = Color(0x33000000),
                            offset = Offset(0f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
        PodcastBadge(
            count = badgeCount,
            modifier = Modifier.align(Alignment.TopEnd),
            badgeType = badgeType
        )
    }
}

@Composable
private fun PodcastBadge(modifier: Modifier = Modifier, count: Int, badgeType: Settings.BadgeType) {
    if (count == 0) {
        return
    }
    val badgeColor = MaterialTheme.theme.colors.support05
    Canvas(modifier = modifier.size(30.dp)) {
        val badgePath = Path().apply {
            val size = 30.dp.toPx()
            moveTo(0f, 0f)
            lineTo(size, 0f)
            lineTo(size, size)
        }
        drawPath(
            color = badgeColor,
            path = badgePath
        )
    }
    Text(
        text = if (badgeType != Settings.BadgeType.LATEST_EPISODE) count.toString() else "●",
        fontSize = if (count > 9) 12.sp else 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier.padding(end = if (count > 9) 1.dp else 4.dp)
    )
}

@Composable
private fun FolderPodcastImage(
    uuid: String?,
    color: Color,
    gradientColor: List<Color>,
    modifier: Modifier = Modifier
) {
    if (uuid == null) {
        BoxWithConstraints(modifier) {
            val corners = when {
                maxWidth <= 50.dp -> 3.dp
                maxWidth <= 200.dp -> 4.dp
                else -> 8.dp
            }
            val elevation = when {
                maxWidth <= 50.dp -> 1.dp
                maxWidth <= 200.dp -> 2.dp
                else -> 4.dp
            }
            Card(
                elevation = elevation,
                shape = RoundedCornerShape(corners),
                backgroundColor = color,
                modifier = modifier
            ) {
                Box(
                    modifier = Modifier
                        .size(maxWidth)
                        .background(brush = Brush.verticalGradient(colors = gradientColor))
                ) {}
                Box(
                    modifier = Modifier
                        .size(maxWidth)
                        .background(color = Color(0x19000000))
                ) {}
            }
        }
    } else {
        PodcastImage(
            uuid = uuid,
            modifier = modifier
        )
    }
}

@ShowkaseComposable(name = "FolderImage", group = "Folder", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun FolderImageLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        FolderImagePreview()
    }
}

@ShowkaseComposable(name = "FolderImage", group = "Folder", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun FolderImageDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        FolderImagePreview()
    }
}

@Composable
private fun FolderImagePreview() {
    FolderImage(
        name = "Favourites",
        color = Color.Blue,
        podcastUuids = emptyList(),
        badgeCount = 1,
        badgeType = Settings.BadgeType.ALL_UNFINISHED,
        modifier = Modifier.size(100.dp)
    )
}
