package au.com.shiftyjelly.pocketcasts.compose.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.random.Random
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import java.time.format.TextStyle as DateTextStyle

enum class HeatLevel {
    None,
    Low,
    Medium,
    High,
    Max,
}

@Composable
fun CalendarHeatMap(
    start: LocalDate,
    end: LocalDate,
    cellHeatLevel: (LocalDate) -> HeatLevel,
    modifier: Modifier = Modifier,
    cellSize: Dp = 12.dp,
    cellSpacing: Dp = 4.dp,
    scrollState: ScrollState = rememberScrollState(initial = Int.MAX_VALUE),
) {
    val colors = rememberHeatColors()
    val data = rememberHeatMapData(
        start = start,
        end = end.plusDays(1).coerceAtLeast(start),
    )
    val sizing = rememberHeatMapSizing(
        weeks = data.weeks,
        cellSize = cellSize,
        cellSpacing = cellSpacing,
    )

    val locale = LocalResources.current.configuration.locales[0]
    val dayLabelsByRow = remember(locale) {
        Days.associateWith { day -> day.getDisplayName(DateTextStyle.SHORT, locale) }
    }

    Column(
        modifier = modifier,
    ) {
        val dayLabelWidth = measureWeekdayLabels(dayLabelsByRow.values, sizing.textStyle)
        MonthLabels(
            data = data,
            sizing = sizing,
            modifier = Modifier
                .padding(start = dayLabelWidth + 4.dp, bottom = 2.dp)
                .horizontalScroll(scrollState),
        )

        Row {
            WeekdayLabels(
                dayLabelsByRow = dayLabelsByRow,
                sizing = sizing,
                modifier = Modifier.padding(end = 4.dp),
            )
            Cells(
                data = data,
                sizing = sizing,
                colors = colors,
                cellHeatLevel = cellHeatLevel,
                modifier = Modifier.horizontalScroll(scrollState),
            )
        }

        Legend(
            sizing = sizing,
            colors = colors,
            modifier = Modifier
                .padding(top = 6.dp)
                .align(Alignment.End),
        )
    }
}

@Composable
private fun WeekdayLabels(
    dayLabelsByRow: Map<DayOfWeek, String>,
    sizing: HeatMapSizing,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier.height(sizing.mapHeight),
    ) {
        dayLabelsByRow.forEach { (dayOfWeek, label) ->
            val labelHeight = with(density) {
                textMeasurer.measure(label, sizing.textStyle).size.height.toDp()
            }
            Text(
                text = label,
                modifier = Modifier.offset(y = sizing.cellPitch * dayOfWeek.value + (sizing.cellSize - labelHeight) / 2),
                color = MaterialTheme.theme.colors.primaryText02,
                style = sizing.textStyle,
            )
        }
    }
}

@Composable
private fun measureWeekdayLabels(
    dayLabels: Collection<String>,
    textStyle: TextStyle,
): Dp {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val maxLabelWidth = dayLabels.maxOfOrNull { label ->
        textMeasurer.measure(label, textStyle).size.width
    } ?: 0

    return with(density) { maxLabelWidth.toDp() }
}

private val Days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

@Composable
private fun MonthLabels(
    data: HeatMapData,
    sizing: HeatMapSizing,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.width(sizing.mapWidth),
    ) {
        val locale = LocalResources.current.configuration.locales[0]
        data.monthLabels.forEach { monthLabel ->
            Text(
                text = monthLabel.month.getDisplayName(DateTextStyle.SHORT, locale),
                modifier = Modifier.offset(x = sizing.cellPitch * monthLabel.column),
                color = MaterialTheme.theme.colors.primaryText02,
                style = sizing.textStyle,
            )
        }
    }
}

@Composable
private fun Cells(
    data: HeatMapData,
    sizing: HeatMapSizing,
    colors: HeatColors,
    cellHeatLevel: (LocalDate) -> HeatLevel,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.size(sizing.mapWidth, sizing.mapHeight),
    ) {
        val cellSizePx = sizing.cellSize.toPx()
        val cellPitchPx = sizing.cellPitch.toPx()

        data.cells.forEach { cell ->
            drawHeatSquare(
                topLeft = Offset(cell.column * cellPitchPx, cell.row * cellPitchPx),
                cellSizePx = cellSizePx,
                color = colors[cellHeatLevel(cell.date)],
            )
        }
    }
}

@Composable
private fun Legend(
    sizing: HeatMapSizing,
    colors: HeatColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(LR.string.less),
            color = MaterialTheme.theme.colors.primaryText02,
            style = sizing.textStyle,
        )
        Canvas(
            modifier = Modifier.size(sizing.legendWidth, sizing.cellSize),
        ) {
            val cellSizePx = sizing.cellSize.toPx()
            val cellPitchPx = sizing.cellPitch.toPx()

            HeatLevel.entries.forEachIndexed { cellIndex, heatLevel ->
                drawHeatSquare(
                    topLeft = Offset(cellIndex * cellPitchPx, 0f),
                    cellSizePx = cellSizePx,
                    color = colors[heatLevel],
                )
            }
        }
        Text(
            text = stringResource(LR.string.more),
            color = MaterialTheme.theme.colors.primaryText02,
            style = sizing.textStyle,
        )
    }
}

private fun DrawScope.drawHeatSquare(
    topLeft: Offset,
    cellSizePx: Float,
    color: Color,
) {
    drawRoundRect(
        topLeft = topLeft,
        size = Size(cellSizePx, cellSizePx),
        cornerRadius = CornerRadius(cellSizePx * 0.2f),
        color = color,
    )
}

private data class HeatMapSizing(
    val cellSize: Dp,
    val cellPitch: Dp,
    val mapWidth: Dp,
    val mapHeight: Dp,
    val legendWidth: Dp,
    val textStyle: TextStyle,
)

@Composable
private fun rememberHeatMapSizing(
    weeks: Int,
    cellSize: Dp,
    cellSpacing: Dp,
): HeatMapSizing {
    val fontScale = LocalDensity.current.fontScale

    return remember(weeks, cellSize, cellSpacing, fontScale) {
        val resizeScale = max(fontScale, 1f)
        val resizedCellSize = cellSize * resizeScale
        val cellPitch = resizedCellSize + cellSpacing

        HeatMapSizing(
            cellSize = resizedCellSize,
            cellPitch = cellPitch,
            mapWidth = resizedCellSize * weeks + cellSpacing * (weeks - 1).coerceAtLeast(0),
            mapHeight = resizedCellSize * 7 + cellSpacing * 6,
            legendWidth = resizedCellSize * HeatLevel.entries.size + cellSpacing * (HeatLevel.entries.size - 1),
            textStyle = TextStyle(fontSize = cellSize.value.sp),
        )
    }
}

private data class HeatColors(
    val none: Color,
    val low: Color,
    val medium: Color,
    val high: Color,
    val max: Color,
) {
    operator fun get(level: HeatLevel): Color = when (level) {
        HeatLevel.None -> none
        HeatLevel.Low -> low
        HeatLevel.Medium -> medium
        HeatLevel.High -> high
        HeatLevel.Max -> max
    }
}

@Composable
private fun rememberHeatColors(): HeatColors {
    val none = MaterialTheme.theme.colors.primaryUi05
    val max = MaterialTheme.theme.colors.primaryIcon01
    return remember(none, max) {
        HeatColors(
            none = none,
            low = ColorUtils.blendColors(none, max, 0.25f),
            medium = ColorUtils.blendColors(none, max, 0.5f),
            high = ColorUtils.blendColors(none, max, 0.75f),
            max = max,
        )
    }
}

private data class HeatMapData(
    val weeks: Int,
    val monthLabels: List<HeatMapMonthLabel>,
    val cells: List<HeatMapCell>,
)

private data class HeatMapMonthLabel(
    val month: Month,
    val column: Int,
)

private data class HeatMapCell(
    val date: LocalDate,
    val column: Int,
    val row: Int,
)

@Composable
private fun rememberHeatMapData(start: LocalDate, end: LocalDate): HeatMapData {
    return remember(start, end) {
        // Use Sunday-based index
        val startOffset = start.dayOfWeek.value % 7

        val monthLabels = buildList {
            var monthStart = start.withDayOfMonth(1)
            if (monthStart.isBefore(start)) {
                monthStart = monthStart.plusMonths(1)
            }

            while (monthStart.isBefore(end)) {
                val daysFromStart = ChronoUnit.DAYS.between(start, monthStart).toInt()
                add(
                    HeatMapMonthLabel(
                        month = monthStart.month,
                        column = (startOffset + daysFromStart) / 7,
                    ),
                )
                monthStart = monthStart.plusMonths(1)
            }
        }

        val daysCount = ChronoUnit.DAYS.between(start, end).toInt()
        val cells = List(daysCount) { dayIndex ->
            val cellIndex = startOffset + dayIndex
            HeatMapCell(
                date = start.plusDays(dayIndex.toLong()),
                column = cellIndex / 7,
                row = cellIndex % 7,
            )
        }

        val cellsCount = startOffset + daysCount
        HeatMapData(
            // Round up partial weeks, then reserve one empty trailing week for labels.
            weeks = (cellsCount + 6) / 7 + 1,
            monthLabels = monthLabels,
            cells = cells,
        )
    }
}

@Preview
@Composable
private fun CalendarHeatMapPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    val previewHeatLevels = remember {
        val random = Random(0)
        List(400) { HeatLevel.entries.random(random) }
    }

    AppThemeWithBackground(themeType) {
        CalendarHeatMap(
            start = LocalDate.of(2025, 3, 6),
            end = LocalDate.of(2026, 1, 1),
            cellHeatLevel = { date -> previewHeatLevels[date.dayOfYear] },
        )
    }
}

@Preview(fontScale = 1.5f)
@Composable
private fun CalendarHeatMapPreviewFont150Percent() {
    val previewHeatLevels = remember {
        val random = Random(0)
        List(366) { HeatLevel.entries.random(random) }
    }

    AppThemeWithBackground(Theme.ThemeType.CLASSIC_LIGHT) {
        CalendarHeatMap(
            start = LocalDate.of(2025, 3, 6),
            end = LocalDate.of(2026, 1, 1),
            cellHeatLevel = { date -> previewHeatLevels[date.dayOfYear] },
        )
    }
}

@Preview(fontScale = 2f)
@Composable
private fun CalendarHeatMapPreviewFont200Percent() {
    val previewHeatLevels = remember {
        val random = Random(0)
        List(366) { HeatLevel.entries.random(random) }
    }

    AppThemeWithBackground(Theme.ThemeType.CLASSIC_LIGHT) {
        CalendarHeatMap(
            start = LocalDate.of(2025, 3, 6),
            end = LocalDate.of(2026, 1, 1),
            cellHeatLevel = { date -> previewHeatLevels[date.dayOfYear] },
        )
    }
}
