package au.com.shiftyjelly.pocketcasts.settings.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.StatsHelper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class StatsFragment : BaseFragment() {
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
        val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)
        AppThemeWithBackground(theme.activeTheme) {
            val state: StatsViewModel.State by viewModel.state.collectAsState()
            StatsPage(
                state = state,
                onBackClick = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                onRetryClick = { viewModel.loadStats() },
                launchReviewDialog = { viewModel.launchAppReviewDialog(it) },
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
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
private fun StatsPage(
    state: StatsViewModel.State,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    launchReviewDialog: (AppCompatActivity) -> Unit,
    bottomInset: Dp,
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_navigation_stats),
            onNavigationClick = onBackClick,
        )
        when (state) {
            is StatsViewModel.State.Loaded -> StatsPageLoaded(state, launchReviewDialog, bottomInset)
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
    launchReviewDialog: (AppCompatActivity) -> Unit,
    bottomInset: Dp,
) {
    val context = LocalContext.current
    val timeSpentList = listOf(
        TimeSpent("Casual Tuner", 10.hours),
        TimeSpent("The Audio Addict", 50.hours),
        TimeSpent("Master of Podcasts", 500.hours),
        TimeSpent("The Podcast Deity", 1000.hours),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            if (state.startedAt == null || state.startedAt.time <= 0) {
                item {
                    TextP40(stringResource(LR.string.profile_stats_listened_for))
                }
            } else {
                item {
                    TextP40(
                        stringResource(
                            LR.string.profile_stats_since_listened_for,
                            state.startedAt.toLocalizedFormatLongStyle(),
                        ),
                    )
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                LargeTimeText(state.totalListened)
                Spacer(Modifier.height(6.dp))
            }
            item {
                TextP40(state.funnyText)
                Spacer(Modifier.height(24.dp))
            }
            item {
                TextC70(stringResource(LR.string.profile_stats_time_saved_by))
                Spacer(Modifier.height(6.dp))
            }
            item {
                StatsRow(
                    icon = R.drawable.ic_skipping,
                    label = LR.string.profile_stats_skipping,
                    value = state.skipping,
                )
            }
            item {
                StatsRow(
                    icon = R.drawable.ic_speed,
                    label = LR.string.profile_stats_variable_speed,
                    value = state.variableSpeed,
                )
            }
            item {
                StatsRow(
                    icon = R.drawable.ic_trim,
                    label = LR.string.profile_stats_trim_silence,
                    value = state.trimSilence,
                )
            }
            item {
                StatsRow(
                    icon = R.drawable.ic_skip_both,
                    label = LR.string.profile_stats_auto_skipping,
                    value = state.autoSkipping,
                )
                Spacer(Modifier.height(12.dp))
            }
            item {
                Spacer(Modifier.height(24.dp))
                TotalRow(state.totalSaved)
                Spacer(Modifier.height(12.dp))
            }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
            }

            item {
                Text(
                    text = stringResource(LR.string.profile_stats_achievements),
                    fontSize = 28.sp,
                    color = MaterialTheme.theme.colors.primaryText01,
                    fontWeight = FontWeight.W500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        LazyVerticalGrid(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(timeSpentList.size) { index ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                ) {
                    Badge(isAchieved = state.totalListened.seconds >= timeSpentList[index].time)
                    TextP40(timeSpentList[index].title, fontWeight = FontWeight.W500, modifier = Modifier.padding(top = 4.dp))
                    TextC70("Listen for ${timeSpentList[index].time.inWholeHours} hours", modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }

    if (state.showAppReviewDialog) {
        LaunchedEffect(Unit) {
            context.getActivity()?.let {
                launchReviewDialog(it)
            }
        }
    }
}

@Composable
private fun Badge(
    modifier: Modifier = Modifier,
    isAchieved: Boolean,
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
                painter = painterResource(au.com.shiftyjelly.pocketcasts.images.R.drawable.ic_filters_headphones),
                contentDescription = null,
                tint = iconColor,
            )
        }
    }
}

@Composable
private fun TotalRow(time: Long) {
    Row(Modifier.semantics(mergeDescendants = true) {}) {
        TextP40(stringResource(LR.string.profile_stats_total))
        Spacer(Modifier.weight(1f))
        TimeText(
            time = time,
            color = MaterialTheme.theme.colors.support01,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun LargeTimeText(time: Long, modifier: Modifier = Modifier) {
    TimeText(
        time = time,
        color = MaterialTheme.theme.colors.support01,
        fontSize = 31.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

@Composable
private fun TimeText(
    time: Long,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    val context = LocalContext.current
    val timeText = remember(time) {
        StatsHelper.secondsToFriendlyString(time, context.resources)
    }
    Text(
        text = timeText,
        fontSize = fontSize,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        modifier = modifier,
    )
}

@Composable
private fun StatsRow(@DrawableRes icon: Int, @StringRes label: Int, value: Long) {
    Row(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.theme.colors.primaryIcon01,
            modifier = Modifier.padding(end = 16.dp),
        )
        TextP40(
            text = stringResource(label),
            modifier = Modifier.padding(end = 16.dp),
        )
        Spacer(Modifier.weight(1f))
        TimeText(
            time = value,
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.End,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsPageLoadedPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    val state = StatsViewModel.State.Loaded(
        totalListened = 1234,
        skipping = 60,
        variableSpeed = 34,
        trimSilence = 55,
        autoSkipping = 344,
        totalSaved = 3443,
        funnyText = "During which time you blinked 637 times. Heyooo!",
        startedAt = Date(),
    )
    AppThemeWithBackground(themeType) {
        StatsPage(
            state = state,
            onBackClick = { },
            onRetryClick = { },
            launchReviewDialog = { },
            bottomInset = 0.dp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsPageErrorPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        StatsPage(
            state = StatsViewModel.State.Error,
            onBackClick = { },
            onRetryClick = { },
            launchReviewDialog = { },
            bottomInset = 0.dp,
        )
    }
}

data class TimeSpent(val title: String, val time: Duration)
