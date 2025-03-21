package au.com.shiftyjelly.pocketcasts.settings.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.stats.StatGroup.StatItem.Rating
import au.com.shiftyjelly.pocketcasts.settings.stats.StatGroup.StatItem.TimeSpent
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AchievementsFragment : BaseFragment() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var settings: Settings
    private val viewModel: StatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            val state: StatsViewModel.State by viewModel.state.collectAsState()
            AchievementsPage(
                state = state,
                onBackClick = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                onRetryClick = { viewModel.loadStats() },
            )
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadStats()
    }

    override fun onBackPressed(): Boolean {
        analyticsTracker.track(AnalyticsEvent.STATS_DISMISSED)
        return super.onBackPressed()
    }
}

@Composable
private fun AchievementsPage(
    state: StatsViewModel.State,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_navigation_achievements),
            onNavigationClick = onBackClick,
        )
        when (state) {
            is StatsViewModel.State.Loaded -> StatsPageLoaded(state)
            is StatsViewModel.State.Error -> StatsPageError(onRetryClick)
            is StatsViewModel.State.Loading -> StatsPageLoading()
        }
    }
}

@Composable
private fun StatsPageLoading() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}

@Composable
private fun StatsPageError(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        TextH40(
            text = stringResource(LR.string.profile_status_error_internet),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        TextH40(
            text = stringResource(LR.string.retry),
            color = MaterialTheme.theme.colors.primaryInteractive01,
            modifier = modifier
                .clickable { onRetryClick() }
                .padding(8.dp),
        )
    }
}

@Composable
private fun StatsPageLoaded(
    state: StatsViewModel.State.Loaded,
) {
    val statsGroups = listOf(
        StatGroup.TimeSpentGroup(
            "Total Listen Time",
            listOf(
                TimeSpent("Casual Tuner", 10.hours),
                TimeSpent("The Audio Addict", 50.hours),
                TimeSpent("Master of Podcasts", 500.hours),
                TimeSpent("The Podcast Deity", 1000.hours),
            ),
        ),
        StatGroup.RatingGroup(
            "Total Ratings given",
            listOf(
                Rating("Quick Judge", 1),
                Rating("The Pod Rater", 5),
                Rating("The Critic", 20),
                Rating("The Pod Expert", 50),
            ),
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        statsGroups.forEach { group ->
            item {
                Text(
                    text = group.header,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            items(
                count = group.items.chunked(2).size,
                key = { index -> group.header + index },
                contentType = { "StatRow" },
            ) { rowIndex ->

                val rowItems = group.items.chunked(2)[rowIndex]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowItems.forEach { statItem ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                        ) {
                            when (statItem) {
                                is TimeSpent -> {
                                    Badge(isAchieved = state.totalListened.seconds >= statItem.time, icon = R.drawable.ic_filters_headphones)
                                    TextP40(statItem.title, fontWeight = FontWeight.W500, modifier = Modifier.padding(top = 4.dp))
                                    TextC70("Listen for ${statItem.time.inWholeHours} hours", modifier = Modifier.padding(top = 4.dp))
                                }
                                is Rating -> {
                                    Badge(isAchieved = state.totalRatings >= statItem.count, icon = R.drawable.ic_starred)
                                    TextP40(statItem.title, fontWeight = FontWeight.W500, modifier = Modifier.padding(top = 4.dp))
                                    TextC70("Rated by You: ${statItem.count}", modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(
    modifier: Modifier = Modifier,
    isAchieved: Boolean,
    icon: Int,
    size: Dp = 80.dp,
) {
    var hasAnimated by remember { mutableStateOf(false) }

    LaunchedEffect(isAchieved) {
        if (isAchieved && !hasAnimated) {
            hasAnimated = true
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isAchieved && hasAnimated) 1.2f else 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "badgeScale",
    )

    val gradientColors = if (isAchieved) {
        listOf(Color(0xFFFFD700), Color(0xFFFFA500))
    } else {
        listOf(Color(0xFFA0A0A0), Color(0xFF707070))
    }

    val iconColor = if (isAchieved) Color.White else Color(0xFFB0B0B0)
    val hexagonColor = if (isAchieved) Color.White.copy(alpha = 0.4f) else Color(0xFF888888).copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .size(size * 1.5f)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = gradientColors,
                        center = Offset(size.value / 2, size.value / 2),
                        radius = size.value * 0.6f,
                    ),
                )
                .border(
                    width = 4.dp,
                    brush = Brush.linearGradient(
                        colors = if (isAchieved) {
                            listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
                        } else {
                            listOf(Color.Gray.copy(alpha = 0.3f), Color.Transparent)
                        },
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hexagonPath = Path()
                val hexagonRadius = size.toPx() * 0.4f
                val centerX = size.toPx() / 2
                val centerY = size.toPx() / 2
                val angle = 60f

                for (i in 0 until 6) {
                    val radian = Math.toRadians(angle * i.toDouble())
                    val x = centerX + hexagonRadius * cos(radian).toFloat()
                    val y = centerY + hexagonRadius * sin(radian).toFloat()
                    if (i == 0) hexagonPath.moveTo(x, y) else hexagonPath.lineTo(x, y)
                }
                hexagonPath.close()

                drawPath(
                    path = hexagonPath,
                    color = hexagonColor,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconColor,
            )
        }
    }
}

sealed class StatGroup(open val header: String, open val items: List<StatItem>) {
    data class TimeSpentGroup(
        override val header: String,
        override val items: List<TimeSpent>,
    ) : StatGroup(header, items)

    data class RatingGroup(
        override val header: String,
        override val items: List<Rating>,
    ) : StatGroup(header, items)

    sealed class StatItem {
        data class TimeSpent(val title: String, val time: Duration) : StatItem()
        data class Rating(val title: String, val count: Int) : StatItem()
    }
}
